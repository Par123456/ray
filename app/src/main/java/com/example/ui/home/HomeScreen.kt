package com.example.ui.home

import android.app.Activity
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VpnProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onNavigateToAddProfile: (Int?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState(initial = emptyList())
    val isConnected by viewModel.isConnected.collectAsState()
    val externalIp by viewModel.externalIp.collectAsState()
    val egressDetails by viewModel.egressDetails.collectAsState()
    val isSmartHealerEnabled by viewModel.isSmartHealerEnabled.collectAsState()
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val totalUpload by viewModel.totalUpload.collectAsState()
    val totalDownload by viewModel.totalDownload.collectAsState()
    val routingMode by viewModel.routingMode.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                android.util.Log.w("HomeScreen", "Notification permission was denied")
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
            }
        }
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.toggleConnection()
            } else {
                scope.launch { snackbarHostState.showSnackbar("VPN permission was declined.") }
            }
        }
    )

    val activeProfile = profiles.firstOrNull { it.isSelected }

    val fabPaddingBottom by animateDpAsState(
        targetValue = if (isConnected) 295.dp else 205.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fab_padding"
    )

    val listPaddingBottom by animateDpAsState(
        targetValue = if (isConnected) 305.dp else 215.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "list_padding"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Header with app title and logo background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.VpnLock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AnishtayiN-RAY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "v1.6.8 - High Speed Core",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    label = { Text("Server Profiles") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.GroupWork, contentDescription = null) },
                    label = { Text("Subscription Groups") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            snackbarHostState.showSnackbar("Subscriptions setting opened.")
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Global Settings") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToSettings()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("About Application") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            showAboutDialog = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "AnishtayiN-RAY",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Scan QR Code
                        IconButton(onClick = onNavigateToScanner) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR Code")
                        }
                        // Paste config clipboard
                        IconButton(onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                viewModel.importConfig(clipText)
                                scope.launch { snackbarHostState.showSnackbar("Configuration imported from clipboard.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Clipboard is empty.") }
                            }
                        }) {
                            Icon(Icons.Default.ContentPasteGo, contentDescription = "Import from Clipboard")
                        }
                        // Latency Ping check all
                        IconButton(onClick = { viewModel.pingAll(profiles) }) {
                            Icon(Icons.Default.Speed, contentDescription = "Ping All Server Latencies")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToAddProfile(null) },
                    modifier = Modifier
                        .padding(bottom = fabPaddingBottom) // Leave dynamic roomy space for the bottom connection panel!
                        .testTag("add_profile_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Profile Configuration")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Config List
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Server Profiles Headline Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SERVER PROFILES (${profiles.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (profiles.isNotEmpty()) {
                            TextButton(onClick = { viewModel.pingAll(profiles) }) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Realtime Ping Test", fontSize = 12.sp)
                            }
                        }
                    }

                    if (profiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "هیچ کانفیگی یافت نشد / No Profiles Available",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "جهت افزودن، بارکد اسکن کنید یا از کلیپ‌بورد جای‌گذاری نمایید\nTap QR Scan or Paste from Clipboard to import",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = listPaddingBottom) // so list content is not blocked by bottom bar and flows animations nicely
                        ) {
                            items(profiles) { profile ->
                                ProfileItem(
                                    profile = profile,
                                    onSelect = { viewModel.selectProfile(profile.id) },
                                    onEdit = { onNavigateToAddProfile(profile.id) },
                                    onCopy = {
                                        val configLink = viewModel.exportConfig(profile)
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(configLink))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("لینک کانفیگ با موفقیت کپی شد / Config copied!")
                                        }
                                    },
                                    onDelete = { viewModel.deleteProfile(profile) },
                                    onPing = { viewModel.pingProfile(profile) }
                                )
                            }
                        }
                    }
                }

                // SIGNATURE V2RAYNG CONNECTION CONTROL PANEL - Positioned sticking dynamically at bottom
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Connection Pulse Indicator
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse_light")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "pulse_alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isConnected) Color(0xFF4CAF50).copy(alpha = pulseAlpha)
                                        else Color.Gray.copy(alpha = 0.5f)
                                    )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Connection text & active server metadata
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isConnected) "Connected Successfully" else "Disconnected (Tap right button to tunnel)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = activeProfile?.name ?: "No Server Selected",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isConnected && externalIp != null) {
                                    Column(modifier = Modifier.padding(top = 2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = "Active External IP",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "IP: $externalIp",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (!egressDetails.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = egressDetails ?: "",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Dedicated Play/Pause Switch on the Bottom Panel (Signature v2rayNG Style!)
                            FloatingActionButton(
                                onClick = {
                                    if (!isConnected) {
                                        if (activeProfile == null) {
                                            scope.launch { snackbarHostState.showSnackbar("Please select or add a VPN server profile first.") }
                                        } else {
                                            val intent = VpnService.prepare(context)
                                            if (intent != null) {
                                                vpnLauncher.launch(intent)
                                            } else {
                                                viewModel.toggleConnection()
                                            }
                                        }
                                    } else {
                                        viewModel.toggleConnection()
                                    }
                                },
                                containerColor = if (isConnected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isConnected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("connection_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isConnected) "Disconnect" else "Connect"
                                )
                            }
                        }

                        if (isConnected) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Traffic flow throughput speeds (Upload/Download)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Upload speed",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = uploadSpeed,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Download speed",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = downloadSpeed,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Badge(
                                    containerColor = MaterialTheme.colorScheme.successColor()
                                ) {
                                    Text(
                                        text = activeProfile?.protocol ?: "VLESS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Cumulative transferred traffic session volume metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "کل ارسال / Total Up: ",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = totalUpload,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "کل دریافت / Total Down: ",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = totalDownload,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    try {
                                        val ssIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("tg://socks?server=127.0.0.1&port=10808"))
                                        context.startActivity(ssIntent)
                                    } catch (e: Exception) {
                                        try {
                                            val ssIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/socks?server=127.0.0.1&port=10808"))
                                            context.startActivity(ssIntent)
                                        } catch (ex: Exception) {
                                            scope.launch { snackbarHostState.showSnackbar("Telegram app or web browser not found.") }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF24A1DE),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("telegram_proxy_btn"),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send proxy config to Telegram",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تنظیم و اتصال خودکار پروکسی تلگرام (SOCKS5)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Smart Latency Healer option Switch row (Premium feature over v2rayNG)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSmartHealerEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isSmartHealerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "بهبود هوشمند تاخیر (Smart Auto-Healer)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSmartHealerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "سوییچ خودکار پس‌زمینه به سریع‌ترین سرور در صورت افت سرعت",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Switch(
                                checked = isSmartHealerEnabled,
                                onCheckedChange = { viewModel.setSmartHealerEnabled(it) },
                                modifier = Modifier.scale(0.7f).height(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Routing mode switcher selector
                            var showRoutingMenu by remember { mutableStateOf(false) }
                            Box {
                                AssistChip(
                                    onClick = { showRoutingMenu = true },
                                    label = { Text("Route: $routingMode", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = MaterialTheme.colorScheme.primary,
                                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                DropdownMenu(
                                    expanded = showRoutingMenu,
                                    onDismissRequest = { showRoutingMenu = false }
                                ) {
                                    val routingModesList = listOf("Global (Proxy All)", "Bypass LAN & Mainland", "Only Direct")
                                    routingModesList.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode, fontSize = 13.sp) },
                                            onClick = {
                                                viewModel.setRoutingMode(mode)
                                                showRoutingMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Console Terminal button
                            var showTerminalLogsDialog by remember { mutableStateOf(false) }
                            AssistChip(
                                onClick = { showTerminalLogsDialog = true },
                                label = { Text("Core Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = MaterialTheme.colorScheme.secondary,
                                    leadingIconContentColor = MaterialTheme.colorScheme.secondary
                                )
                            )

                            // Dialog showing real-time logs
                            if (showTerminalLogsDialog) {
                                AlertDialog(
                                    onDismissRequest = { showTerminalLogsDialog = false },
                                    confirmButton = {
                                        TextButton(onClick = { showTerminalLogsDialog = false }) {
                                            Text("Close")
                                        }
                                    },
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("AnishtayiN-RAY Core Logs", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    text = {
                                        val scrollState = rememberScrollState()
                                        // launch a coroutine to scroll to the end of the logs automatically
                                        LaunchedEffect(logs.size) {
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp)
                                                .background(Color(0xFF0F0E13), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF2C2A35), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                                .verticalScroll(scrollState)
                                        ) {
                                            logs.forEach { logLine ->
                                                val textColor = when {
                                                    logLine.startsWith("[SUCCESS]") -> Color(0xFF4CAF50)
                                                    logLine.startsWith("[PROXY]") -> Color(0xFF8C9EFF)
                                                    logLine.startsWith("[DIRECT]") -> Color(0xFFFFB300)
                                                    logLine.startsWith("[SYSTEM]") -> Color(0xFF41C9E2)
                                                    else -> Color(0xFFE0E0E0)
                                                }
                                                Text(
                                                    text = logLine,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = textColor,
                                                    lineHeight = 14.sp,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                            }
                                        }
                                    },
                                    properties = androidx.compose.ui.window.DialogProperties(
                                        usePlatformDefaultWidth = false
                                    ),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text("بستن / Close")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnLock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "درباره برنامه / About App",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "AnishtayiN-RAY v1.7.0",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "کلاینت اختصاصی و بهینه‌سازی شده V2Ray با سرعت بالا و قابلیت‌های هوشمند انحصاری.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "Exclusive premium features (Smart Auto-Healer, Egress Geo-IP details) for supreme bypass.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "برنامه‌نویس / Developer:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            // Telegram Button Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF24A1DE).copy(alpha = 0.15f))
                                    .clickable {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/ANishtayiN"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Telegram Channel",
                                    tint = Color(0xFF24A1DE),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("کانال تلگرام", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF24A1DE))
                                    Text("t.me/ANishtayiN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // GitHub Button Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/AnishtayiN"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "GitHub Repositories",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("گیت‌هاب سازنده", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("github.com/AnishtayiN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileItem(
    profile: VpnProfile,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onPing: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (profile.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (profile.isSelected) 4.dp else 1.dp
        ),
        border = if (profile.isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            )
        ) else null
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = profile.isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${profile.protocol} \u2022 ${profile.address}:${profile.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (profile.isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Ping state
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pingText: String
                    val pingColor: Color
                    when {
                        profile.lastPing == -2L -> {
                            pingText = "Testing delay..."
                            pingColor = MaterialTheme.colorScheme.primary
                        }
                        profile.lastPing == -1L -> {
                            pingText = "Not tested"
                            pingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                        profile.lastPing == -3L -> {
                            pingText = "Network unreachable"
                            pingColor = MaterialTheme.colorScheme.error
                        }
                        profile.lastPing >= 9999L -> {
                            pingText = "Timeout / Unreachable"
                            pingColor = MaterialTheme.colorScheme.error
                        }
                        profile.lastPing >= 600L -> {
                            pingText = "${profile.lastPing} ms"
                            pingColor = MaterialTheme.colorScheme.error
                        }
                        profile.lastPing >= 250L -> {
                            pingText = "${profile.lastPing} ms"
                            pingColor = Color(0xFFFF9800)
                        }
                        else -> {
                            pingText = "${profile.lastPing} ms"
                            pingColor = Color(0xFF4CAF50)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(pingColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pingText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = pingColor
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Ping test trigger button
                IconButton(
                    onClick = onPing,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Test latency",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Copy configuration link button
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Config",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Edit configuration button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Config",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete configuration
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Delete Configuration",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ColorScheme.successColor(): Color {
    return Color(0xFF4CAF50)
}
