package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_profiles")
data class VpnProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val protocol: String, // "VLESS", "Shadowsocks", "VMess", "Trojan", "WireGuard", "Hysteria2", "Tuic", "SOCKS5", "HTTP"
    val address: String,
    val port: Int,
    val uuidOrPassword: String,
    val network: String = "tcp", // ws, h2, grpc, quic, httpupgrade, xhttp
    val sni: String = "",
    val security: String = "none", // tls, reality, none
    val isSelected: Boolean = false,
    val lastPing: Long = -1L,
    
    // Advanced fields
    val configType: Int = 1, // 1=VMess, 2=VLess, 3=Shadowsocks, 4=Trojan, 5=WireGuard, 6=Hysteria2, 7=SSH, 8=HTTP, 9=SOCKS5, 10=Tuic
    val subscriptionId: String = "",
    val username: String = "",
    val password: String = "",
    val method: String = "none", // aes-256-gcm, chacha20-ietf-poly1305, etc.
    val flow: String = "", // xtls-rprx-vision, xtls-rprx-vision-udp443
    val headerType: String = "none", // http, srtp, utp, etc.
    val host: String = "",
    val path: String = "",
    val seed: String = "",
    val serviceName: String = "",
    val mode: String = "gun", // gun, multi
    val alpn: String = "h2,http/1.1",
    val fingerPrint: String = "chrome", // chrome, firefox, safari, ios, android, random
    val insecure: Boolean = false,
    val publicKey: String = "",
    val shortId: String = "",
    val obfs: String = "",
    val upMbps: Int = 0,
    val downMbps: Int = 0,
    val congestionControl: String = "bbr",
    val zeroRttHandshake: Boolean = false,
    val detour: String = "",
    val domainStrategy: String = "AsIs"
)
