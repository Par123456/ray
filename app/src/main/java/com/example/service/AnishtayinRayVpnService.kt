package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class AnishtayinRayVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var proxyServer: ServerSocket? = null
    private var proxyThread: Thread? = null
    private var socksServer: ServerSocket? = null
    private var tunThread: Thread? = null

    // Safe socket tracker and stream references to avoid system-wide leaks
    private var tunInputChannel: java.io.FileInputStream? = null
    private var tunOutputChannel: java.io.FileOutputStream? = null
    private val activeSockets = java.util.Collections.synchronizedList(mutableListOf<Socket>())
    private var dohClient: okhttp3.OkHttpClient? = null
    private var directDohClient: okhttp3.OkHttpClient? = null

    // High performance pre-allocated thread pool executor (like native Go-coroutines in v2ray)
    private val proxyExecutor = java.util.concurrent.ThreadPoolExecutor(
        16, 256, 30L, java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.SynchronousQueue<Runnable>(),
        java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy()
    )

    // High performance memory cache for the active profile and its pre-resolved IP
    @Volatile
    private var cachedActiveProfile: com.example.data.VpnProfile? = null
    @Volatile
    private var cachedServerAddr: String? = null

    // Concurrent high-speed DNS Cache with TTL tracking to bypass remote roundtrips
    private val dnsCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
    private val dnsCacheExpiry = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun byteArrayToHex(bytes: ByteArray, start: Int, length: Int): String {
        val hexChars = "0123456789ABCDEF"
        val result = java.lang.StringBuilder(length * 2)
        for (i in start until (start + length)) {
            val octet = bytes[i].toInt()
            val firstIndex = (octet and 0xF0) ushr 4
            val secondIndex = octet and 0x0F
            result.append(hexChars[firstIndex])
            result.append(hexChars[secondIndex])
        }
        return result.toString()
    }

    private fun tuneSocket(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.sendBufferSize = 131072 // 128KB optimal buffer
            socket.receiveBufferSize = 131072 // 128KB optimal buffer
            socket.trafficClass = 0x18 // Low delay + high throughput
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error tuning socket", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning.value = true
        createNotificationChannel()
        
        // Initialize Doh client with connection pooling and optimal-reuse timeouts
        val builder = okhttp3.OkHttpClient.Builder()
            .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(15, 5, java.util.concurrent.TimeUnit.MINUTES))
        try {
            val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", 10809))
            builder.proxy(proxy)
        } catch (e: java.lang.Exception) {}
        dohClient = builder.build()

        // Direct fallback client in case local proxy has not bootstrapped yet
        directDohClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES))
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            disconnect()
            return START_NOT_STICKY
        }
        
        startForegroundService()
        establishVpn()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Connection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows secure core VPN tunneling status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AnishtayiN-RAY Secure Core")
            .setContentText("Connected to selected proxy server. Tunnel routing active.")
            .setSmallIcon(android.R.drawable.ic_menu_share) // standard fallback android drawable icon
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .apply {
                pendingIntent?.let { setContentIntent(it) }
            }
            .build()

        isRunning = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun establishVpn() {
        try {
            if (vpnInterface != null) return

            // Start local high-speed V2Ray/Xray tunnel proxy!
            startLocalProxy()

            val builder = Builder()
                .setSession("AnishtayiN-RAY Secure Core")
                .addAddress("10.8.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1360)
                .addRoute("10.8.0.0", 24)
                .addRoute("1.1.1.1", 32)
                .addRoute("8.8.8.8", 32)
                .addRoute("8.8.4.4", 32)
                .allowBypass()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    builder.setHttpProxy(android.net.ProxyInfo.buildPacProxy(android.net.Uri.parse("http://127.0.0.1:10809/proxy.pac")))
                    Log.d("AnishtayinVpnService", "System-wide PAC proxy configured for local SOCKS5 tunnel.")
                } catch (e: Exception) {
                    Log.e("AnishtayinVpnService", "Failed to set PAC proxy on Builder", e)
                }
            }

            vpnInterface = builder.establish()
            Log.d("AnishtayinVpnService", "VPN Interface established successfully.")
            startTunPacketReader()
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error establishing VPN interface", e)
        }
    }

    private fun startLocalProxy() {
        try {
            proxyServer = ServerSocket(10809)
            proxyThread = Thread {
                while (isRunning) {
                    try {
                        val clientSocket = proxyServer?.accept() ?: break
                        tuneSocket(clientSocket)
                        proxyExecutor.submit {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        // socket closed
                    }
                }
            }.apply { start() }

            socksServer = ServerSocket(10808)
            Thread {
                while (isRunning) {
                    try {
                        val clientSocket = socksServer?.accept() ?: break
                        tuneSocket(clientSocket)
                        proxyExecutor.submit {
                            handleSocksClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        // socket closed
                    }
                }
            }.start()

            Log.d("AnishtayinVpnService", "Local Proxies started: HTTP (10809), SOCKS5 (10808).")
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Failed to start local proxies", e)
        }
    }

    private fun handleClient(clientSocket: Socket) {
        activeSockets.add(clientSocket)
        try {
            val inputStream = clientSocket.getInputStream()
            val outputStream = clientSocket.getOutputStream()
            
            val headerStream = ByteArrayOutputStream()
            var state = 0
            while (isRunning) {
                val b = inputStream.read()
                if (b == -1) break
                headerStream.write(b)
                
                // Track sequence of \r\n\r\n to find the end of the HTTP/CONNECT headers
                if (state == 0 && b == '\r'.code) {
                    state = 1
                } else if (state == 1 && b == '\n'.code) {
                    state = 2
                } else if (state == 2 && b == '\r'.code) {
                    state = 3
                } else if (state == 3 && b == '\n'.code) {
                    state = 4
                    break
                } else {
                    if (b == '\r'.code) {
                        state = 1
                    } else if (b == '\n'.code) {
                        if (state == 2) {
                            state = 4
                            break
                        } else {
                            state = 2
                        }
                    } else {
                        state = 0
                    }
                }
            }
            
            val headersBytes = headerStream.toByteArray()
            if (headersBytes.isEmpty()) {
                clientSocket.close()
                return
            }
            
            val headersStr = String(headersBytes, java.nio.charset.StandardCharsets.UTF_8)
            val lines = headersStr.split("\r\n", "\n")
            if (lines.isEmpty()) {
                clientSocket.close()
                return
            }
            
            val firstLine = lines[0]
            Log.d("AnishtayinProxy", "Client request line: $firstLine")
            
            val parts = firstLine.split(" ")
            if (parts.size < 2) {
                clientSocket.close()
                return
            }
            
            val method = parts[0]
            val url = parts[1]

            if (url.contains("proxy.pac") || url.endsWith("/proxy.pac")) {
                val pacContent = "function FindProxyForURL(url, host) {\n" +
                        "  return \"SOCKS5 127.0.0.1:10808; SOCKS 127.0.0.1:10808; PROXY 127.0.0.1:10809; DIRECT\";\n" +
                        "}\n"
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/x-ns-proxy-autoconfig\r\n" +
                        "Content-Length: ${pacContent.toByteArray(java.nio.charset.StandardCharsets.UTF_8).size}\r\n" +
                        "Connection: close\r\n\r\n" +
                        pacContent
                outputStream.write(response.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                outputStream.flush()
                try { clientSocket.close() } catch (e: Exception) {}
                return
            }
            
            var targetHost = ""
            var targetPort = 80
            
            val isConnect = method.equals("CONNECT", ignoreCase = true)
            if (isConnect) {
                val hostPort = url.split(":")
                targetHost = hostPort[0]
                targetPort = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 443 else 443
                
                outputStream.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                outputStream.flush()
            } else {
                try {
                    val uri = java.net.URI(url)
                    targetHost = uri.host ?: ""
                    targetPort = uri.port.takeIf { it > 0 } ?: 80
                } catch (ex: Exception) {
                    val cleanUrl = if (url.startsWith("/")) "http://localhost$url" else url
                    val uri = java.net.URI(cleanUrl)
                    targetHost = uri.host ?: ""
                    targetPort = uri.port.takeIf { it > 0 } ?: 80
                }
            }
            
            if (targetHost.isBlank()) {
                clientSocket.close()
                return
            }
            
            val clientInStream = if (isConnect) {
                inputStream
            } else {
                java.io.SequenceInputStream(
                    java.io.ByteArrayInputStream(headersBytes),
                    inputStream
                )
            }
            
            val activeProfile = getActiveProfile()
            val prefs = getSharedPreferences("anishtayin_prefs", Context.MODE_PRIVATE)
            val currentRoutingMode = prefs.getString("routing_mode", "Global (Proxy All)") ?: "Global (Proxy All)"
            
            var shouldBypass = false
            when (currentRoutingMode) {
                "Only Direct" -> {
                    shouldBypass = true
                }
                "Bypass LAN & Mainland" -> {
                    shouldBypass = targetHost == "localhost" || 
                                   targetHost == "127.0.0.1" || 
                                   targetHost.startsWith("192.168.") || 
                                   targetHost.startsWith("10.") || 
                                   targetHost.endsWith(".ir") ||
                                   targetHost.contains("aparat") ||
                                   targetHost.contains("snapp") ||
                                   targetHost.contains("divar")
                }
                else -> { // "Global (Proxy All)"
                    shouldBypass = false
                }
            }
            
            if (activeProfile != null && !shouldBypass) {
                val proto = activeProfile.protocol.uppercase()
                when (proto) {
                    "VLESS" -> connectVless(clientSocket, clientInStream, outputStream, targetHost, targetPort, activeProfile)
                    "TROJAN" -> connectTrojan(clientSocket, clientInStream, outputStream, targetHost, targetPort, activeProfile)
                    else -> connectDirect(clientSocket, clientInStream, outputStream, targetHost, targetPort)
                }
            } else {
                connectDirect(clientSocket, clientInStream, outputStream, targetHost, targetPort)
            }
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error in HTTP proxy client tunnel handler", e)
        } finally {
            activeSockets.remove(clientSocket)
            try { clientSocket.close() } catch (ex: Exception) {}
        }
    }

    private fun sha224(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-224")
            val digest = md.digest(input.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
            val sb = StringBuilder()
            for (b in digest) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            input
        }
    }

    private fun getTrustAllSocketFactory(): SSLSocketFactory {
        return try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                object : javax.net.ssl.X509TrustManager {
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
                }
            )
            val sc = javax.net.ssl.SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, java.security.SecureRandom())
            sc.socketFactory as SSLSocketFactory
        } catch (e: Exception) {
            SSLSocketFactory.getDefault() as SSLSocketFactory
        }
    }

    private fun configureAlpn(sslSocket: javax.net.ssl.SSLSocket) {
        try {
            val method = sslSocket.javaClass.getMethod("setApplicationProtocols", Array<String>::class.java)
            method.invoke(sslSocket, arrayOf("h2", "http/1.1"))
        } catch (e: Exception) {
            // Fallback gracefully on runtimes that don't support ALPN
        }
    }

    private fun bridgeForward(
        clientSocket: Socket,
        clientIn: java.io.InputStream,
        clientOut: java.io.OutputStream,
        remoteSocket: Socket,
        remoteIn: java.io.InputStream,
        remoteOut: java.io.OutputStream
    ) {
        val latch = java.util.concurrent.CountDownLatch(1)

        // 1. Client to Remote forwarding using pre-allocated thread pools (asynchronous, non-blocking caller)
        proxyExecutor.submit {
            val buffer = ByteArray(65536)
            try {
                var bytesRead: Int
                while (isRunning && !clientSocket.isClosed && !remoteSocket.isClosed) {
                    bytesRead = clientIn.read(buffer)
                    if (bytesRead < 0) break
                    remoteOut.write(buffer, 0, bytesRead)
                    remoteOut.flush()
                }
            } catch (e: Exception) {
                // Connection broken
            } finally {
                try { remoteSocket.close() } catch (e: Exception) {}
                try { clientSocket.close() } catch (e: Exception) {}
                latch.countDown()
            }
        }

        // 2. Remote to Client forwarding running directly on the current caller thread (extreme CPU / context-switch saving)
        val buffer = ByteArray(65536)
        try {
            var bytesRead: Int
            while (isRunning && !remoteSocket.isClosed && !clientSocket.isClosed) {
                bytesRead = remoteIn.read(buffer)
                if (bytesRead < 0) break
                clientOut.write(buffer, 0, bytesRead)
                clientOut.flush()
            }
        } catch (e: Exception) {
            // Connection broken
        } finally {
            try { clientSocket.close() } catch (e: Exception) {}
            try { remoteSocket.close() } catch (e: Exception) {}
            latch.countDown()
        }

        // 3. Keep-alive sync bounded to 5 seconds gracefully
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: Exception) {}
    }

    private fun connectTrojan(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        targetHost: String,
        targetPort: Int,
        profile: com.example.data.VpnProfile
    ) {
        var trojanSocket: Socket? = null
        try {
            val serverAddr = cachedServerAddr ?: try {
                val resolved = InetAddress.getByName(profile.address).hostAddress
                if (resolved != null) {
                    cachedServerAddr = resolved
                    resolved
                } else {
                    profile.address
                }
            } catch (e: Exception) {
                profile.address
            }
            val rawSocket = Socket()
            tuneSocket(rawSocket)
            protect(rawSocket)
            rawSocket.connect(InetSocketAddress(serverAddr, profile.port), 6000)
            
            if (profile.security.lowercase() == "tls" || profile.security.lowercase() == "reality" || profile.port == 443) {
                val factory = getTrustAllSocketFactory()
                val sslSocket = factory.createSocket(rawSocket, profile.address, profile.port, true) as SSLSocket
                tuneSocket(sslSocket)
                if (!profile.sni.isNullOrBlank()) {
                    try {
                        val sslParams = sslSocket.sslParameters
                        sslParams.serverNames = listOf(SNIHostName(profile.sni))
                        sslSocket.sslParameters = sslParams
                    } catch (ex: Exception) {
                        Log.e("AnishtayinProxy", "Unable to apply SNI param to Trojan TLS", ex)
                    }
                }
                configureAlpn(sslSocket)
                sslSocket.startHandshake()
                trojanSocket = sslSocket
            } else {
                trojanSocket = rawSocket
            }
            tuneSocket(trojanSocket)
            
            val trojanOut = trojanSocket.getOutputStream()
            val trojanIn = trojanSocket.getInputStream()
            
            val header = ByteArrayOutputStream()
            val hexPassword = sha224(profile.uuidOrPassword)
            header.write(hexPassword.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
            header.write(0x0D)
            header.write(0x0A)
            header.write(0x01)
            
            header.write(0x03)
            val hostBytes = targetHost.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
            header.write(hostBytes.size and 0xFF)
            header.write(hostBytes)
            
            header.write((targetPort shr 8) and 0xFF)
            header.write(targetPort and 0xFF)
            header.write(0x0D)
            header.write(0x0A)
            
            trojanOut.write(header.toByteArray())
            trojanOut.flush()
            
            bridgeForward(clientSocket, clientIn, clientOut, trojanSocket, trojanIn, trojanOut)
        } catch (e: Exception) {
            Log.e("AnishtayinProxy", "Trojan forwarding failed to $targetHost:$targetPort", e)
            try { trojanSocket?.close() } catch (ex: Exception) {}
            try { clientSocket.close() } catch (ex: Exception) {}
        }
    }

    private fun connectVless(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        targetHost: String,
        targetPort: Int,
        profile: com.example.data.VpnProfile
    ) {
        var vlessSocket: Socket? = null
        try {
            val serverAddr = cachedServerAddr ?: try {
                val resolved = InetAddress.getByName(profile.address).hostAddress
                if (resolved != null) {
                    cachedServerAddr = resolved
                    resolved
                } else {
                    profile.address
                }
            } catch (e: Exception) {
                profile.address
            }
            val rawSocket = Socket()
            tuneSocket(rawSocket)
            protect(rawSocket)
            rawSocket.connect(InetSocketAddress(serverAddr, profile.port), 6000)
            
            if (profile.security.lowercase() == "tls" || profile.security.lowercase() == "reality") {
                val factory = getTrustAllSocketFactory()
                val sslSocket = factory.createSocket(rawSocket, profile.address, profile.port, true) as SSLSocket
                tuneSocket(sslSocket)
                if (!profile.sni.isNullOrBlank()) {
                    try {
                        val sslParams = sslSocket.sslParameters
                        sslParams.serverNames = listOf(SNIHostName(profile.sni))
                        sslSocket.sslParameters = sslParams
                    } catch (ex: Exception) {
                        Log.e("AnishtayinProxy", "Unable to apply SNI param", ex)
                    }
                }
                configureAlpn(sslSocket)
                sslSocket.startHandshake()
                vlessSocket = sslSocket
            } else {
                vlessSocket = rawSocket
            }
            tuneSocket(vlessSocket)
            
            val vlessOut = vlessSocket.getOutputStream()
            val vlessIn = vlessSocket.getInputStream()
            
            // Format VLESS v0 request header
            val header = ByteArrayOutputStream()
            header.write(0x00) // version (v0)
            header.write(uuidToBytes(profile.uuidOrPassword)) // 16 bytes UUID
            header.write(0x00) // addons length (0)
            header.write(0x01) // command TCP (0x01)
            
            // Port (2 bytes big endian)
            header.write((targetPort shr 8) and 0xFF)
            header.write(targetPort and 0xFF)
            
            // Address type (0x02 for domain name representation, most robust)
            header.write(0x02)
            val hostBytes = targetHost.toByteArray()
            header.write(hostBytes.size and 0xFF)
            header.write(hostBytes)
            
            // Write VLESS header
            vlessOut.write(header.toByteArray())
            vlessOut.flush()
            
            // Parse VLESS v0 response header: [1 byte version] + [1 byte addon len] + [addon bytes if any]
            val version = vlessIn.read()
            val addonLen = vlessIn.read()
            if (version >= 0 && addonLen >= 0) {
                if (addonLen > 0) {
                    var skipped = 0
                    val dump = ByteArray(addonLen)
                    while (skipped < addonLen) {
                        val r = vlessIn.read(dump, skipped, addonLen - skipped)
                        if (r < 0) break
                        skipped += r
                    }
                }
                
                bridgeForward(clientSocket, clientIn, clientOut, vlessSocket, vlessIn, vlessOut)
            } else {
                throw java.io.IOException("VLESS handshake failed: invalid server response headers")
            }
        } catch (e: Exception) {
            Log.e("AnishtayinProxy", "VLESS forwarding failed to $targetHost:$targetPort", e)
            try { vlessSocket?.close() } catch (ex: Exception) {}
            try { clientSocket.close() } catch (ex: Exception) {}
        }
    }

    private fun connectDirect(
        clientSocket: Socket,
        clientIn: java.io.InputStream,
        clientOut: java.io.OutputStream,
        targetHost: String,
        targetPort: Int
    ) {
        var destSocket: Socket? = null
        try {
            destSocket = Socket()
            tuneSocket(destSocket)
            protect(destSocket)
            destSocket.connect(InetSocketAddress(targetHost, targetPort), 5000)
            tuneSocket(destSocket)
            
            val destOut = destSocket.getOutputStream()
            val destIn = destSocket.getInputStream()
            
            bridgeForward(clientSocket, clientIn, clientOut, destSocket, destIn, destOut)
        } catch (e: Exception) {
            try { destSocket?.close() } catch (ex: Exception) {}
            try { clientSocket.close() } catch (ex: Exception) {}
        }
    }

    private fun getActiveProfile(): com.example.data.VpnProfile? {
        val cached = cachedActiveProfile
        if (cached != null) return cached
        return try {
            val dao = com.example.data.AppDatabase.getDatabase(applicationContext).vpnProfileDao()
            val profile = runBlocking { dao.getSelectedProfile().first() }
            cachedActiveProfile = profile
            profile
        } catch (e: Exception) {
            null
        }
    }

    private fun uuidToBytes(uuidStr: String): ByteArray {
        val clean = uuidStr.replace("-", "")
        if (clean.length != 32) return ByteArray(16)
        val bytes = ByteArray(16)
        for (i in 0 until 16) {
            bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }

    private fun disconnect() {
        isRunning = false
        isServiceRunning.value = false
        
        // Clear server and profile cache references to allow dynamic transition on restart
        cachedActiveProfile = null
        cachedServerAddr = null

        // Drain pending tasks from proxy thread pool
        try {
            proxyExecutor.queue.clear()
        } catch (e: Exception) {}
        
        // Clear secure DNS caching layers to prevent stale redirection
        try {
            dnsCache.clear()
            dnsCacheExpiry.clear()
        } catch (e: Exception) {}
        
        // 1. Rigorous cleanup of active network sockets
        synchronized(activeSockets) {
            for (socket in activeSockets) {
                try {
                    socket.close()
                } catch (e: Exception) {
                    Log.e("AnishtayinVpnService", "Error closing sockets on exit", e)
                }
            }
            activeSockets.clear()
        }

        // 2. Shut down proxy servers immediately to trigger IOException in accept() loops
        try {
            proxyServer?.close()
            proxyServer = null
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error closing proxy server", e)
        }
        try {
            socksServer?.close()
            socksServer = null
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error closing SOCKS server", e)
        }

        // 3. Close the TUN streams to unblock native read/write threads
        try {
            tunInputChannel?.close()
            tunInputChannel = null
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error closing input stream", e)
        }
        try {
            tunOutputChannel?.close()
            tunOutputChannel = null
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error closing output stream", e)
        }

        // 4. Close the interface descriptor
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error closing interface", e)
        }

        // 5. Interrupt worker thread
        try {
            tunThread?.interrupt()
            tunThread = null
        } catch (e: Exception) {}

        // 6. Stop foreground state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun handleSocksClient(clientSocket: Socket) {
        activeSockets.add(clientSocket)
        try {
            val inStream = clientSocket.getInputStream()
            val outStream = clientSocket.getOutputStream()
            
            // 1. Greeting
            val version = inStream.read()
            if (version != 5) {
                clientSocket.close()
                return
            }
            val numMethods = inStream.read()
            if (numMethods <= 0) {
                clientSocket.close()
                return
            }
            val methods = ByteArray(numMethods)
            var totalRead = 0
            while (totalRead < numMethods) {
                val r = inStream.read(methods, totalRead, numMethods - totalRead)
                if (r < 0) break
                totalRead += r
            }
            
            // Respond with SOCKS v5, No Authentication
            outStream.write(byteArrayOf(0x05, 0x00))
            outStream.flush()
            
            // 2. Client Request
            val ver = inStream.read()
            val cmd = inStream.read()
            val rsv = inStream.read()
            val atyp = inStream.read()
            
            if (ver != 5 || cmd != 1) { // 1 = CONNECT
                clientSocket.close()
                return
            }
            
            var targetHost = ""
            if (atyp == 1) { // IPv4
                val ipBytes = ByteArray(4)
                var readBytes = 0
                while (readBytes < 4) {
                    val r = inStream.read(ipBytes, readBytes, 4 - readBytes)
                    if (r < 0) break
                    readBytes += r
                }
                targetHost = "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}.${ipBytes[3].toInt() and 0xFF}"
            } else if (atyp == 3) { // Domain
                val len = inStream.read()
                val domainBytes = ByteArray(len)
                var readBytes = 0
                while (readBytes < len) {
                    val r = inStream.read(domainBytes, readBytes, len - readBytes)
                    if (r < 0) break
                    readBytes += r
                }
                targetHost = String(domainBytes, java.nio.charset.StandardCharsets.UTF_8)
            } else if (atyp == 4) { // IPv6
                val ipBytes = ByteArray(16)
                var readBytes = 0
                while (readBytes < 16) {
                    val r = inStream.read(ipBytes, readBytes, 16 - readBytes)
                    if (r < 0) break
                    readBytes += r
                }
                targetHost = InetAddress.getByAddress(ipBytes).hostAddress ?: ""
            } else {
                clientSocket.close()
                return
            }
            
            val portHigh = inStream.read()
            val portLow = inStream.read()
            val targetPort = ((portHigh and 0xFF) shl 8) or (portLow and 0xFF)
            
            // Respond with connection SUCCESS status
            outStream.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            outStream.flush()
            
            val activeProfile = getActiveProfile()
            val prefs = getSharedPreferences("anishtayin_prefs", Context.MODE_PRIVATE)
            val currentRoutingMode = prefs.getString("routing_mode", "Global (Proxy All)") ?: "Global (Proxy All)"
            
            var shouldBypass = false
            when (currentRoutingMode) {
                "Only Direct" -> shouldBypass = true
                "Bypass LAN & Mainland" -> {
                    shouldBypass = targetHost == "localhost" || 
                                   targetHost == "127.0.0.1" || 
                                   targetHost.startsWith("192.168.") || 
                                   targetHost.startsWith("10.") || 
                                   targetHost.endsWith(".ir") ||
                                   targetHost.contains("aparat") ||
                                   targetHost.contains("snapp") ||
                                   targetHost.contains("divar")
                }
                else -> shouldBypass = false
            }
            
            if (activeProfile != null && !shouldBypass) {
                val proto = activeProfile.protocol.uppercase()
                when (proto) {
                    "VLESS" -> connectVless(clientSocket, inStream, outStream, targetHost, targetPort, activeProfile)
                    "TROJAN" -> connectTrojan(clientSocket, inStream, outStream, targetHost, targetPort, activeProfile)
                    else -> connectDirect(clientSocket, inStream, outStream, targetHost, targetPort)
                }
            } else {
                connectDirect(clientSocket, inStream, outStream, targetHost, targetPort)
            }
        } catch (e: Exception) {
            Log.e("AnishtayinVpnService", "Error in SOCKS proxy client tunnel handler", e)
        } finally {
            activeSockets.remove(clientSocket)
            try { clientSocket.close() } catch (ex: Exception) {}
        }
    }

    private fun startTunPacketReader() {
        val fd = vpnInterface?.fileDescriptor ?: return
        val inputChannel = java.io.FileInputStream(fd)
        val outputChannel = java.io.FileOutputStream(fd)
        
        tunInputChannel = inputChannel
        tunOutputChannel = outputChannel
        
        tunThread = Thread {
            val buffer = ByteArray(16384)
            while (isRunning) {
                try {
                    val length = inputChannel.read(buffer)
                    if (length > 0) {
                        handleTunPacket(buffer, length, outputChannel)
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }.apply { start() }
    }

    private fun isAaaaOrHttpsQuery(query: ByteArray): Boolean {
        try {
            if (query.size < 12) return false
            var pos = 12
            while (pos < query.size) {
                val len = query[pos].toInt() and 0xFF
                if (len == 0) {
                    pos++
                    break
                }
                pos += len + 1
            }
            if (pos + 2 <= query.size) {
                val qtype = ((query[pos].toInt() and 0xFF) shl 8) or (query[pos+1].toInt() and 0xFF)
                return qtype == 28 || qtype == 65 // 28 is AAAA, 65 is HTTPS
            }
        } catch (e: Exception) {
            Log.e("AnishtayinVpn", "Error parsing DNS packet type", e)
        }
        return false
    }

    private fun buildEmptyDnsResponse(query: ByteArray): ByteArray {
        val resp = query.clone()
        if (resp.size >= 12) {
            // Flags: 0x8180 (Standard query response, No error, Recursion desired + available)
            resp[2] = 0x81.toByte()
            resp[3] = 0x80.toByte()
            // Answer count: 0
            resp[6] = 0x00.toByte()
            resp[7] = 0x00.toByte()
            // Authority count: 0
            resp[8] = 0x00.toByte()
            resp[9] = 0x00.toByte()
            // Additional count: 0
            resp[10] = 0x00.toByte()
            resp[11] = 0x00.toByte()
        }
        return resp
    }

    private fun handleTunPacket(packet: ByteArray, length: Int, outputStream: java.io.FileOutputStream) {
        if (length < 28) return
        
        // Check standard IPv4 version
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return
        
        // Extract protocol
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return // 17 is UDP
        
        // Dest port
        val destPort = ((packet[22].toInt() and 0xFF) shl 8) or (packet[23].toInt() and 0xFF)
        if (destPort == 53) {
            val dnsPayloadLen = length - 28
            if (dnsPayloadLen <= 0) return
            
            val dnsQuery = ByteArray(dnsPayloadLen)
            System.arraycopy(packet, 28, dnsQuery, 0, dnsPayloadLen)
            
            if (isAaaaOrHttpsQuery(dnsQuery)) {
                val dnsResponse = buildEmptyDnsResponse(dnsQuery)
                sendDnsResponse(packet, dnsResponse, outputStream)
                return
            }
            
            proxyExecutor.submit {
                val dnsResponse = queryDoh(dnsQuery)
                if (dnsResponse != null) {
                    sendDnsResponse(packet, dnsResponse, outputStream)
                }
            }
        }
    }

    private fun executeDohCall(client: okhttp3.OkHttpClient?, url: String, host: String, dnsQuery: ByteArray): ByteArray? {
        val targetClient = client ?: return null
        val requestBody = okhttp3.RequestBody.create(
            "application/dns-message".toMediaTypeOrNull(),
            dnsQuery
        )
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Accept", "application/dns-message")
            .header("Host", host)
            .post(requestBody)
            .build()
        targetClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body?.bytes()
            }
        }
        return null
    }

    private fun queryDoh(dnsQuery: ByteArray): ByteArray? {
        if (dnsQuery.size <= 2) return null
        
        // 1. Check Cache (Skip first 2 bytes to match questions regardless of transaction ID)
        val cacheKey = byteArrayToHex(dnsQuery, 2, dnsQuery.size - 2)
        val now = System.currentTimeMillis()
        val cached = dnsCache[cacheKey]
        val expiry = dnsCacheExpiry[cacheKey] ?: 0L
        
        if (cached != null && now < expiry) {
            val response = cached.clone()
            if (response.size >= 2) {
                // Impose current request transaction ID
                response[0] = dnsQuery[0]
                response[1] = dnsQuery[1]
                return response
            }
        }

        val endpoints = listOf(
            Pair("https://1.1.1.1/dns-query", "cloudflare-dns.com"),
            Pair("https://8.8.8.8/dns-query", "dns.google"),
            Pair("https://9.9.9.9/dns-query", "dns.quad9.net"),
            Pair("https://94.140.14.140/dns-query", "dns.adguard-dns.com")
        )

        // 2. Try proxied DoH (bypasses DPI, encrypted inside the active proxy tunnel)
        for (endpoint in endpoints) {
            try {
                val responseBytes = executeDohCall(dohClient, endpoint.first, endpoint.second, dnsQuery)
                if (responseBytes != null && responseBytes.isNotEmpty()) {
                    dnsCache[cacheKey] = responseBytes
                    dnsCacheExpiry[cacheKey] = now + 300000L // 5 mins TTL
                    return responseBytes
                }
            } catch (e: Exception) {
                // Failover to next endpoint
            }
        }

        // 3. Fallback to direct secure DNS if the proxy server is offline, switching, or bootstrapping
        for (endpoint in endpoints) {
            try {
                val responseBytes = executeDohCall(directDohClient, endpoint.first, endpoint.second, dnsQuery)
                if (responseBytes != null && responseBytes.isNotEmpty()) {
                    dnsCache[cacheKey] = responseBytes
                    dnsCacheExpiry[cacheKey] = now + 120000L // 2 mins TTL
                    return responseBytes
                }
            } catch (e: Exception) {
                // Failover to next direct endpoint
            }
        }

        return null
    }

    private fun sendDnsResponse(
        originalPacket: ByteArray,
        dnsResponse: ByteArray,
        outputStream: java.io.FileOutputStream
    ) {
        try {
            val responseLen = 20 + 8 + dnsResponse.size
            val packet = ByteArray(responseLen)
            
            packet[0] = 0x45.toByte() // IPv4, 20 bytes
            packet[1] = 0x00.toByte()
            packet[2] = ((responseLen shr 8) and 0xFF).toByte()
            packet[3] = (responseLen and 0xFF).toByte()
            packet[6] = 0x40.toByte() // Don't fragment
            packet[7] = 0x00.toByte()
            packet[8] = 64.toByte() // TTL
            packet[9] = 17.toByte() // UDP Protocol
            
            // Set IP sources
            packet[12] = originalPacket[16]
            packet[13] = originalPacket[17]
            packet[14] = originalPacket[18]
            packet[15] = originalPacket[19]
            
            // Set IP dest
            packet[16] = originalPacket[12]
            packet[17] = originalPacket[13]
            packet[18] = originalPacket[14]
            packet[19] = originalPacket[15]
            
            val checksum = calculateChecksum(packet, 0, 20)
            packet[10] = ((checksum shr 8) and 0xFF).toByte()
            packet[11] = (checksum and 0xFF).toByte()
            
            // UDP Header
            packet[20] = originalPacket[22]
            packet[21] = originalPacket[23]
            packet[22] = originalPacket[20]
            packet[23] = originalPacket[21]
            
            val udpLen = 8 + dnsResponse.size
            packet[24] = ((udpLen shr 8) and 0xFF).toByte()
            packet[25] = (udpLen and 0xFF).toByte()
            
            System.arraycopy(dnsResponse, 0, packet, 28, dnsResponse.size)
            
            synchronized(outputStream) {
                outputStream.write(packet)
                outputStream.flush()
            }
        } catch (e: Exception) {
            Log.e("AnishtayinVpn", "Error writing DNS reply", e)
        }
    }

    private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val high = (data[i].toInt() and 0xFF) shl 8
            val low = if (i + 1 < offset + length) data[i + 1].toInt() and 0xFF else 0
            sum += (high or low)
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv()) and 0xFFFF
    }

    override fun onDestroy() {
        disconnect()
        isServiceRunning.value = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_DISCONNECT = "com.example.service.DISCONNECT"
        const val CHANNEL_ID = "anishtayin_ray_service_channel"
        const val NOTIFICATION_ID = 26541
        
        val isServiceRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    }
}
