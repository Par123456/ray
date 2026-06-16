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

    fun saveProfile(profile: VpnProfile) {
        viewModelScope.launch {
            if (profile.id != 0) {
                dao.updateProfile(profile)
            } else {
                dao.insertProfile(profile)
            }
        }
    }
}
