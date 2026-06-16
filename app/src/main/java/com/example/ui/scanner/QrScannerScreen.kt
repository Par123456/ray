package com.example.ui.scanner

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    var isFlashOn by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Scanning line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "laser_y"
    )

    val simulatedConfigs = listOf(
        "vless://4a3b2c1d-1234-abcd-ef01-234567890abc@germany.anishtayin.cfd:443?security=reality&sni=google.com#🇩🇪 HighSpeed Reality - Frankfurt",
        "ss://YWVzLTI1Ni1nY206cGFzc3dvcmQxMjM=@finland.anishtayin.cfd:8388#🇫🇮 Gaming Shadowsocks - Helsinki",
        "vless://9e8d7c6b-5678-def0-1234-56789abcdef0@usa-newyork.anishtayin.cfd:443?security=tls&sni=apple.com#🇺🇸 NetBypass TLS - New York",
        "ss://Y2hhY2hhMjAtaWV0Zi1wb2x5MTMwNTpzdXBlcnNlY3VyZTEyMw==@japan.anishtayin.cfd:1080#🇯🇵 Tokyo Premium Tunnel"
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isFlashOn = !isFlashOn }) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Align the QR code within the frame to scan",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // Scanning Viewfinder
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Outer scanning grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    val cornerLength = 32.dp.toPx()

                    // Top Left Corner
                    drawArc(
                        color = Color(0xFF6200EE),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    drawLine(
                        color = Color(0xFF6200EE),
                        start = Offset(0f, 0f),
                        end = Offset(cornerLength, 0f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color(0xFF6200EE),
                        start = Offset(0f, 0f),
                        end = Offset(0f, cornerLength),
                        strokeWidth = strokeWidth
                    )

                    // Top Right Corner
                    drawLine(
                        color = Color(0xFF6200EE),
                        start = Offset(size.width, 0f),
                        end = Offset(size.width - cornerLength, 0f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color(0xFF6200EE),
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, cornerLength),
                        strokeWidth = strokeWidth
                    )

                    // Bottom Left Corner
                    drawLine(
                        color = Color(0xFF6200EE),
                        start = Offset(0f, size.height),
                        end = Offset(cornerLength, size.height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color(0xFF6200EE),
                        start = Offset(0f, size.height),
                        end = Offset(0f, size.height - cornerLength),
                        strokeWidth = strokeWidth
                    )

                    // Bottom Right Corner
                    drawLine(
                        color = Color(0xFF6200EE),
                        start = Offset(size.width, size.height),
                        end = Offset(size.width - cornerLength, size.height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color(0xFF6200EE),
                        start = Offset(size.width, size.height),
                        end = Offset(size.width, size.height - cornerLength),
                        strokeWidth = strokeWidth
                    )

                    // Laser Line
                    val laserY = size.height * laserYOffset
                    drawLine(
                        color = Color(0xFFFF2D55),
                        start = Offset(8.dp.toPx(), laserY),
                        end = Offset(size.width - 8.dp.toPx(), laserY),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Centered QR Icon
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }

            // Presets Option (Hyper-helpful for Streaming Web Emulator where camera lacks hardware)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Simulate Scanning Custom QR Code",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Because you are running in an emulator, tap a configuration below to instantly simulate scanning its QR Code:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    simulatedConfigs.forEach { config ->
                        val label = config.substringAfter("#", "Unknown Profile")
                        val protocol = if (config.startsWith("vless://")) "VLESS" else "SS"

                        Button(
                            onClick = {
                                viewModel.importConfig(config)
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Badge(
                                    containerColor = if (protocol == "VLESS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                ) {
                                    Text(protocol, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
