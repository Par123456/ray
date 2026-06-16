package com.example.ui.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VpnProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileScreen(
    profileId: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: AddProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("VLESS") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var uuid by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("tcp") }
    var sni by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("none") }
    
    // Advanced fields
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("none") }
    var flow by remember { mutableStateOf("") }
    var headerType by remember { mutableStateOf("none") }
    var host by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var seed by remember { mutableStateOf("") }
    var serviceName by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("gun") }
    var alpn by remember { mutableStateOf("h2,http/1.1") }
    var fingerPrint by remember { mutableStateOf("chrome") }
    var insecure by remember { mutableStateOf(false) }
    var publicKey by remember { mutableStateOf("") }
    var shortId by remember { mutableStateOf("") }
    var obfs by remember { mutableStateOf("") }
    var upMbps by remember { mutableStateOf("") }
    var downMbps by remember { mutableStateOf("") }
    var congestionControl by remember { mutableStateOf("bbr") }
    var zeroRttHandshake by remember { mutableStateOf(false) }
    var detour by remember { mutableStateOf("") }
    var domainStrategy by remember { mutableStateOf("AsIs") }

    // Toggleable sections to avoid visual clutter
    var showAuthSecSection by remember { mutableStateOf(true) }
    var showNetworkSection by remember { mutableStateOf(false) }
    var showProtocolSpecSection by remember { mutableStateOf(false) }
    var showAdvancedSection by remember { mutableStateOf(false) }

    var isSelectedState by remember { mutableStateOf(false) }
    var lastPingState by remember { mutableStateOf(-1L) }

    LaunchedEffect(profileId) {
        if (profileId != 0) {
            val p = viewModel.getProfile(profileId)
            if (p != null) {
                name = p.name
                protocol = p.protocol
                address = p.address
                port = p.port.toString()
                uuid = p.uuidOrPassword
                network = p.network
                sni = p.sni
                security = p.security
                isSelectedState = p.isSelected
                lastPingState = p.lastPing
                
                username = p.username
                password = p.password
                method = p.method
                flow = p.flow
                headerType = p.headerType
                host = p.host
                path = p.path
                seed = p.seed
                serviceName = p.serviceName
                mode = p.mode
                alpn = p.alpn
                fingerPrint = p.fingerPrint
                insecure = p.insecure
                publicKey = p.publicKey
                shortId = p.shortId
                obfs = p.obfs
                upMbps = if (p.upMbps > 0) p.upMbps.toString() else ""
                downMbps = if (p.downMbps > 0) p.downMbps.toString() else ""
                congestionControl = p.congestionControl
                zeroRttHandshake = p.zeroRttHandshake
                detour = p.detour
                domainStrategy = p.domainStrategy
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (profileId != 0) "ویرایش کانفیگ / Edit" else "افزودن کانفیگ / Add") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val profile = VpnProfile(
                                id = profileId,
                                name = name.ifEmpty { "Profile" },
                                protocol = protocol,
                                address = address,
                                port = port.toIntOrNull() ?: 443,
                                uuidOrPassword = uuid,
                                network = network,
                                sni = sni,
                                security = security,
                                isSelected = isSelectedState,
                                lastPing = lastPingState,
                                username = username,
                                password = password,
                                method = method,
                                flow = flow,
                                headerType = headerType,
                                host = host,
                                path = path,
                                seed = seed,
                                serviceName = serviceName,
                                mode = mode,
                                alpn = alpn,
                                fingerPrint = fingerPrint,
                                insecure = insecure,
                                publicKey = publicKey,
                                shortId = shortId,
                                obfs = obfs,
                                upMbps = upMbps.toIntOrNull() ?: 0,
                                downMbps = downMbps.toIntOrNull() ?: 0,
                                congestionControl = congestionControl,
                                zeroRttHandshake = zeroRttHandshake,
                                detour = detour,
                                domainStrategy = domainStrategy
                            )
                            viewModel.saveProfile(profile)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ================= CARD 1: BASE PARAMETERS =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("مشخصات اصلی / Base Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Profile Name (remarks)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_name")
                    )

                    // Protocol picker Menu Box
                    var protocolExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = protocolExpanded,
                        onExpandedChange = { protocolExpanded = !protocolExpanded }
                    ) {
                        OutlinedTextField(
                            value = protocol,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Protocol") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = protocolExpanded,
                            onDismissRequest = { protocolExpanded = false }
                        ) {
                            listOf("VLESS", "VMess", "Shadowsocks", "Trojan", "WireGuard", "Hysteria2", "Tuic", "SOCKS5", "HTTP").forEach { proto ->
                                DropdownMenuItem(
                                    text = { Text(proto) },
                                    onClick = { protocol = proto; protocolExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address (Server IP/Domain)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_address")
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_port")
                    )
                }
            }

            // ================= CARD 2: AUTHENTICATION & SECURITY =================
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAuthSecSection = !showAuthSecSection }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("امنیت و احراز هویت / Security & Auth", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Icon(
                            if (showAuthSecSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Section"
                        )
                    }

                    AnimatedVisibility(
                        visible = showAuthSecSection,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uuid,
                                onValueChange = { uuid = it },
                                label = { Text("UUID / Password / Key") },
                                modifier = Modifier.fillMaxWidth().testTag("input_uuid")
                            )

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username (Shadowsocks / SSH / Tuic)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Secondary Password (if needed)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Security Dropdown (none, tls, reality)
                            var secExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = secExpanded,
                                onExpandedChange = { secExpanded = !secExpanded }
                            ) {
                                OutlinedTextField(
                                    value = security,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Security Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = secExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = secExpanded,
                                    onDismissRequest = { secExpanded = false }
                                ) {
                                    listOf("none", "tls", "reality").forEach { sec ->
                                        DropdownMenuItem(
                                            text = { Text(sec) },
                                            onClick = { security = sec; secExpanded = false }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = sni,
                                onValueChange = { sni = it },
                                label = { Text("SNI (Server Name Indication)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = flow,
                                onValueChange = { flow = it },
                                label = { Text("Flow (e.g., xtls-rprx-vision)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = method,
                                onValueChange = { method = it },
                                label = { Text("Encryption Method (Shadowsocks / VMess)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = fingerPrint,
                                onValueChange = { fingerPrint = it },
                                label = { Text("Client UTLS Fingerprint (e.g. chrome)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("نادیده‌گرفتن گواهینامه / Allow Insecure", fontWeight = FontWeight.Medium)
                                    Text("Ignore unsafe certificates check", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = insecure,
                                    onCheckedChange = { insecure = it }
                                )
                            }
                        }
                    }
                }
            }

            // ================= CARD 3: TRANSPORT & NETWORKS =================
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNetworkSection = !showNetworkSection }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("شبکه و انتقال / Network Transport", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Icon(
                            if (showNetworkSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Section"
                        )
                    }

                    AnimatedVisibility(
                        visible = showNetworkSection,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Network Dropdown
                            var netExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = netExpanded,
                                onExpandedChange = { netExpanded = !netExpanded }
                            ) {
                                OutlinedTextField(
                                    value = network,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Transport Network") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = netExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = netExpanded,
                                    onDismissRequest = { netExpanded = false }
                                ) {
                                    listOf("tcp", "kcp", "ws", "h2", "grpc", "quic", "httpupgrade", "xhttp").forEach { net ->
                                        DropdownMenuItem(
                                            text = { Text(net) },
                                            onClick = { network = net; netExpanded = false }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = headerType,
                                onValueChange = { headerType = it },
                                label = { Text("Header Type (none, http, etc.)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = { Text("Host (HTTP / WebSocket host)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = path,
                                onValueChange = { path = it },
                                label = { Text("Path / Endpoint") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = serviceName,
                                onValueChange = { serviceName = it },
                                label = { Text("gRPC Service Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = mode,
                                onValueChange = { mode = it },
                                label = { Text("gRPC Mode (gun / multi)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ================= CARD 4: PROTOCOL SPECIFICS (REALITY, HYSTERIA) =================
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProtocolSpecSection = !showProtocolSpecSection }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("پارامترهای اختصاصی / Protocol Specifics", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Icon(
                            if (showProtocolSpecSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Section"
                        )
                    }

                    AnimatedVisibility(
                        visible = showProtocolSpecSection,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("مکانیزم REALITY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                            OutlinedTextField(
                                value = publicKey,
                                onValueChange = { publicKey = it },
                                label = { Text("REALITY Public Key") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = shortId,
                                onValueChange = { shortId = it },
                                label = { Text("REALITY Short ID") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("سایر پروتکل ها (Hysteria2 / KCP)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                            OutlinedTextField(
                                value = obfs,
                                onValueChange = { obfs = it },
                                label = { Text("OBFS Obfuscation Password") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = seed,
                                onValueChange = { seed = it },
                                label = { Text("KCP Seed") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = upMbps,
                                onValueChange = { upMbps = it },
                                label = { Text("Max Upload Limit (Mbps)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = downMbps,
                                onValueChange = { downMbps = it },
                                label = { Text("Max Download Limit (Mbps)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ================= CARD 5: ADVANCED ROUTING & TUNING =================
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedSection = !showAdvancedSection }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تنظیمات پیشرفته / Advanced Tuning", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Icon(
                            if (showAdvancedSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Section"
                        )
                    }

                    AnimatedVisibility(
                        visible = showAdvancedSection,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = alpn,
                                onValueChange = { alpn = it },
                                label = { Text("ALPN (comma separated list)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = domainStrategy,
                                onValueChange = { domainStrategy = it },
                                label = { Text("Domain DNS Strategy (AsIs, IPIfNonMatch, etc.)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = detour,
                                onValueChange = { detour = it },
                                label = { Text("Detour Profile ID Range") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = congestionControl,
                                onValueChange = { congestionControl = it },
                                label = { Text("QUIC Congestion Control (bbr, cubic)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("فعال‌سازی هندشیک 0-RTT", fontWeight = FontWeight.Medium)
                                    Text("Zero round trip time handshake protection", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = zeroRttHandshake,
                                    onCheckedChange = { zeroRttHandshake = it }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
