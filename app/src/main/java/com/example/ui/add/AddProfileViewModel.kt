package com.example.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VpnProfile
import kotlinx.coroutines.launch

class AddProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).vpnProfileDao()

    suspend fun getProfile(id: Int): VpnProfile? {
        return dao.getProfileById(id)
    }

    fun saveProfile(id: Int, name: String, protocol: String, address: String, port: Int, uuid: String) {
        viewModelScope.launch {
            if (id != 0) {
                val existing = dao.getProfileById(id)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name.ifEmpty { "Profile" },
                        protocol = protocol,
                        address = address,
                        port = port,
                        uuidOrPassword = uuid
                    )
                    dao.updateProfile(updated)
                    return@launch
                }
            }
            // Otherwise insert new
            val profile = VpnProfile(
                name = name.ifEmpty { "New Profile" },
                protocol = protocol,
                address = address,
                port = port,
                uuidOrPassword = uuid
            )
            dao.insertProfile(profile)
        }
    }
}
