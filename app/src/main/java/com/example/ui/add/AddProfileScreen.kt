package com.example.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileScreen(
    profileId: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: AddProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var uuid by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("VLESS") }

    LaunchedEffect(profileId) {
        if (profileId != 0) {
            val p = viewModel.getProfile(profileId)
            if (p != null) {
                name = p.name
                address = p.address
                port = p.port.toString()
                uuid = p.uuidOrPassword
                protocol = p.protocol
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
                            viewModel.saveProfile(profileId, name, protocol, address, port.toIntOrNull() ?: 443, uuid)
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth().testTag("input_name")
            )

            // Simplistic protocol selector for demo
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = protocol,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Protocol") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("VLESS") },
                        onClick = { protocol = "VLESS"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("VMess") },
                        onClick = { protocol = "VMess"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Shadowsocks") },
                        onClick = { protocol = "Shadowsocks"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Trojan") },
                        onClick = { protocol = "Trojan"; expanded = false }
                    )
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
                modifier = Modifier.fillMaxWidth().testTag("input_port")
            )

            OutlinedTextField(
                value = uuid,
                onValueChange = { uuid = it },
                label = { Text("UUID / Password") },
                modifier = Modifier.fillMaxWidth().testTag("input_uuid")
            )
        }
    }
}
