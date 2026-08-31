package com.floydwiz.googlemdm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.model.CheckInResult
import com.floydwiz.googlemdm.data.model.EnrollmentResult
import com.floydwiz.googlemdm.data.repository.EmmRepository
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.sync.CheckInScheduler
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EmmUiState(
    val enrollmentToken: String = "",
    val isEnrolled: Boolean = false,
    val deviceId: String? = null,
    val isEnrolling: Boolean = false,
    val enrollmentError: String? = null,
    val isCheckingIn: Boolean = false,
    val checkInError: String? = null,
    val lastCheckInAtMillis: Long? = null,
    val checkInIntervalSeconds: Long? = null,
    val devCaCertInstallResult: String? = null,
    // One-shot signal for a just-succeeded enroll() - true for exactly one
    // event, the UI consumes it via consumeJustEnrolledEvent() so it doesn't
    // re-fire on recomposition or on navigating back to an already-enrolled screen.
    val justEnrolled: Boolean = false
)

/**
 * Backs the manual test UI for the custom EMM backend integration (enroll /
 * check-in). Kept separate from EnrollmentViewModel, which drives the
 * unrelated AMAPI environment preparation flow.
 */
@HiltViewModel
class EmmViewModel @Inject constructor(
    private val emmRepository: EmmRepository,
    private val checkInScheduler: CheckInScheduler,
    private val deviceAdminManager: DeviceAdminManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<EmmUiState> = _uiState.asStateFlow()

    private fun loadInitialState() = EmmUiState(
        isEnrolled = emmRepository.isEnrolled,
        deviceId = emmRepository.deviceId,
        lastCheckInAtMillis = emmRepository.lastSuccessfulCheckInAtMillis,
        checkInIntervalSeconds = if (emmRepository.isEnrolled) emmRepository.checkInIntervalSeconds else null
    )

    fun onEnrollmentTokenChanged(token: String) {
        _uiState.update { it.copy(enrollmentToken = token) }
    }

    /**
     * A scanned QR's raw content is either the backend's placeholder JSON
     * shape ({"enrollmentToken": "..."}) or, in the future, a bare token
     * string - try JSON first, fall back to treating the whole scan as the
     * token itself so either shape works without the app needing to know
     * which one the backend currently emits.
     */
    fun onQrCodeScanned(rawValue: String) {
        val token = extractEnrollmentToken(rawValue)
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(enrollmentError = "QR code did not contain a valid enrollment token") }
            return
        }

        _uiState.update { it.copy(enrollmentToken = token) }
        enroll()
    }

    private fun extractEnrollmentToken(rawValue: String): String? {
        return try {
            JsonParser.parseString(rawValue).asJsonObject.get("enrollmentToken")?.asString
        } catch (e: Exception) {
            rawValue.trim().takeIf { it.isNotEmpty() }
        }
    }

    fun enroll() {
        // Guards against a redundant/racing second call re-submitting an
        // already-consumed token - e.g. QR auto-enroll firing followed by a
        // manual Enroll tap, which would otherwise overwrite a just-succeeded
        // state with a stale "token already used" 400 from the second call.
        if (_uiState.value.isEnrolling || _uiState.value.isEnrolled) return

        val token = _uiState.value.enrollmentToken.trim()
        if (token.isEmpty()) {
            _uiState.update { it.copy(enrollmentError = "Enter an enrollment token") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true, enrollmentError = null) }

            when (val result = emmRepository.enroll(token)) {
                is EnrollmentResult.Success -> {
                    Logger.i("EMM enrollment succeeded")
                    checkInScheduler.ensureScheduled(result.checkInIntervalSeconds)
                    _uiState.update {
                        it.copy(
                            isEnrolling = false,
                            isEnrolled = true,
                            deviceId = result.deviceId,
                            checkInIntervalSeconds = result.checkInIntervalSeconds,
                            enrollmentError = null,
                            justEnrolled = true
                        )
                    }
                }

                is EnrollmentResult.Error -> {
                    _uiState.update {
                        it.copy(isEnrolling = false, enrollmentError = result.message)
                    }
                }
            }
        }
    }

    /** Called by the UI right after showing the one-shot enrolled confirmation. */
    fun consumeJustEnrolledEvent() {
        _uiState.update { it.copy(justEnrolled = false) }
    }

    fun checkInNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingIn = true, checkInError = null) }

            when (val result = emmRepository.checkIn()) {
                is CheckInResult.Success -> {
                    checkInScheduler.rescheduleIfIntervalChanged(result.checkInIntervalSeconds)
                    _uiState.update {
                        it.copy(
                            isCheckingIn = false,
                            checkInIntervalSeconds = result.checkInIntervalSeconds,
                            lastCheckInAtMillis = emmRepository.lastSuccessfulCheckInAtMillis,
                            checkInError = null
                        )
                    }
                }

                CheckInResult.MissingCredentials -> {
                    _uiState.update {
                        it.copy(isCheckingIn = false, checkInError = "Device is not enrolled yet")
                    }
                }

                CheckInResult.Unauthorized -> {
                    // The repository already cleared local credentials - reflect that in the UI
                    // instead of leaving a stale "Enrolled: Yes" showing next to the error.
                    checkInScheduler.stop()
                    _uiState.update {
                        it.copy(
                            isCheckingIn = false,
                            isEnrolled = false,
                            deviceId = null,
                            checkInIntervalSeconds = null,
                            checkInError = "Credential rejected by backend (401) - unenrolled locally, enter a new token to re-enroll"
                        )
                    }
                }

                is CheckInResult.Error -> {
                    _uiState.update {
                        it.copy(isCheckingIn = false, checkInError = result.message)
                    }
                }
            }
        }
    }

    /**
     * Clears local enrollment state only - the backend contract has no DPC
     * unenroll endpoint, so this can't (and doesn't try to) notify the backend.
     */
    fun resetEnrollment() {
        viewModelScope.launch {
            emmRepository.clearEnrollment()
            checkInScheduler.stop()
            _uiState.update {
                EmmUiState(enrollmentToken = it.enrollmentToken)
            }
        }
    }

    /** Debug-only: installs the local backend's dev CA cert via the device-owner API. */
    fun installDevCaCertificate() {
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) {
                deviceAdminManager.installBundledDevCaCertificate()
            }
            Logger.i("Dev CA cert install requested, installed=$installed")
            _uiState.update {
                it.copy(
                    devCaCertInstallResult = if (installed) {
                        "Installed - retry Enroll now"
                    } else {
                        "Failed - see Logcat"
                    }
                )
            }
        }
    }
}
