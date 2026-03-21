package ru.tabel.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tabel.app.data.model.Profile
import ru.tabel.app.data.repository.TabelRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: TabelRepository
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = repo.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<Profile?> = repo.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createProfile(name: String) = viewModelScope.launch {
        // Читаем актуальный список из БД
        val all = repo.allProfiles.first()
        repo.profileDao.insertProfile(
            Profile(
                id       = UUID.randomUUID().toString(),
                name     = name.trim().ifEmpty { "Профиль" },
                isActive = all.isEmpty()  // активный только если первый
            )
        )
    }

    fun selectProfile(profile: Profile) = viewModelScope.launch {
        repo.switchProfile(profile.id)
    }

    fun deleteProfile(profile: Profile) = viewModelScope.launch {
        // Читаем актуальный список из БД (не из кэша Flow)
        val all       = repo.allProfiles.first()
        val remaining = all.filter { it.id != profile.id }

        // repo.deleteProfile удаляет и профиль и все его смены
        repo.deleteProfile(profile)

        // Если удалили активный — активируем следующий
        if (profile.isActive && remaining.isNotEmpty()) {
            repo.switchProfile(remaining.first().id)
        }
    }

    fun renameProfile(profile: Profile, newName: String) = viewModelScope.launch {
        repo.profileDao.insertProfile(
            profile.copy(name = newName.trim().ifEmpty { profile.name })
        )
    }
}
