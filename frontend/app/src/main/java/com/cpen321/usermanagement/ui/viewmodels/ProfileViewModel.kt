package com.cpen321.usermanagement.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    // Loading states
    val isLoadingProfile: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isLoadingPhoto: Boolean = false,

    // Data states
    val user: User? = null,
    val allHobbies: List<String> = emptyList(),
    val selectedHobbies: Set<String> = emptySet(),

    // Message states
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProfile = true, errorMessage = null)

            val profileResult = profileRepository.getProfile()
            val hobbiesResult = profileRepository.getAvailableHobbies()

            if (profileResult.isSuccess && hobbiesResult.isSuccess) {
                val user = profileResult.getOrNull()!!
                val availableHobbies = hobbiesResult.getOrNull()!!
                val selectedHobbies = user.hobbies.toSet()

                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    user = user,
                    allHobbies = availableHobbies,
                    selectedHobbies = selectedHobbies
                )
            } else {
                val errorMessage = when {
                    profileResult.isFailure -> {
                        val error = profileResult.exceptionOrNull()
                        Log.e(TAG, "Failed to load profile", error)
                        error?.message ?: "Failed to load profile"
                    }

                    hobbiesResult.isFailure -> {
                        val error = hobbiesResult.exceptionOrNull()
                        Log.e(TAG, "Failed to load hobbies", error)
                        error?.message ?: "Failed to load hobbies"
                    }

                    else -> {
                        Log.e(TAG, "Failed to load data")
                        "Failed to load data"
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun toggleHobby(hobby: String) {
        val currentSelected = _uiState.value.selectedHobbies.toMutableSet()
        if (currentSelected.contains(hobby)) {
            currentSelected.remove(hobby)
        } else {
            currentSelected.add(hobby)
        }
        _uiState.value = _uiState.value.copy(selectedHobbies = currentSelected)
    }

    fun saveHobbies() {
        viewModelScope.launch {
            val originalHobbies = _uiState.value.user?.hobbies?.toSet() ?: emptySet()

            _uiState.value =
                _uiState.value.copy(
                    isSavingProfile = true,
                    errorMessage = null,
                    successMessage = null
                )

            val selectedHobbiesList = _uiState.value.selectedHobbies.toList()
            val result = profileRepository.updateUserHobbies(selectedHobbiesList)

            if (result.isSuccess) {
                val updatedUser = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    user = updatedUser,
                    successMessage = "Hobbies updated successfully!"
                )
            } else {
                // Revert to original hobbies on failure
                val error = result.exceptionOrNull()
                Log.d(TAG, "error: $error")
                Log.e(TAG, "Failed to update hobbies", error)
                val errorMessage = error?.message ?: "Failed to update hobbies"

                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    selectedHobbies = originalHobbies, // Revert the selected hobbies
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun setLoadingPhoto(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoadingPhoto = isLoading)
    }

    fun uploadProfilePicture(pictureUri: Uri, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {

            val currentUser = _uiState.value.user ?: return@launch
            setLoadingPhoto(true)

            try {
                // Convert URI to string (for Google Drive / external URL)
                val uploadResult = uploadProfileFile(pictureUri, context)

                val newProfilePictureUrl = uploadResult.toString()

                // Create the request body
                val result = profileRepository.updateProfilePicture(profilePictureUrl = newProfilePictureUrl)

                if (result.isSuccess) {
                    val updatedUser = result.getOrNull()!!
                    _uiState.value = _uiState.value.copy(
                        isSavingProfile = false,
                        user = updatedUser,
                        successMessage = "Profile updated successfully!"
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPhoto = false,
                        errorMessage = "Failed to update profile picture"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update profile picture", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingPhoto = false,
                    errorMessage = "Failed to update profile picture"
                )
            }
        }
    }

    suspend fun uploadProfileFile(pictureUri: Uri, context: Context): String {
        setLoadingPhoto(true)
        try {
            // Open input stream from the URI
            val inputStream = context.contentResolver.openInputStream(pictureUri)
                ?: throw IllegalArgumentException("Cannot open URI")

            // Copy to a temp file
            val tempFile = File.createTempFile("profile_pic", ".jpg", context.cacheDir)
            inputStream.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }

            // Create MultipartBody
            val requestFile = tempFile.asRequestBody("image/*".toMediaType())
            val body = MultipartBody.Part.createFormData("media", tempFile.name, requestFile)

            // Upload
            val response = RetrofitClient.imageInterface.uploadPicture("", body) //auth handled by interceptor

            if (response.isSuccessful && response.body()?.data != null) {
                return response.body()!!.data?.image.toString();
            } else {
                val errorBody = response.errorBody()?.string()
                throw Exception("Failed to upload profile picture: $errorBody")
            }
        } finally {
            setLoadingPhoto(false)
        }
    }

    fun updateProfile(name: String, bio: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSavingProfile = true,
                    errorMessage = null,
                    successMessage = null
                )

            val result = profileRepository.updateProfile(name, bio)
            if (result.isSuccess) {
                val updatedUser = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    user = updatedUser,
                    successMessage = "Profile updated successfully!"
                )
                onSuccess()
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to update profile", error)
                val errorMessage = error?.message ?: "Failed to update profile"
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    errorMessage = errorMessage
                )
            }
        }
    }
}
