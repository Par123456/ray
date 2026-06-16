package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_profiles")
data class VpnProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val protocol: String, // "VLESS", "Shadowsocks", "VMess"
    val address: String,
    val port: Int,
    val uuidOrPassword: String,
    val network: String = "tcp", // ws, grpc
    val sni: String = "",
    val security: String = "none", // tls, reality
    val isSelected: Boolean = false,
    val lastPing: Long = -1L
)
