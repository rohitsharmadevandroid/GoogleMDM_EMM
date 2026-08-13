package com.floydwiz.googlemdm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.enrollment.manager.AmapiEnvironmentManager
import com.floydwiz.googlemdm.enterprise.enrollment.model.EnrollmentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnrollmentViewModel @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager,
    private val amapiEnvironmentManager: AmapiEnvironmentManager,
) : ViewModel() {
    private val _uistate = MutableStateFlow(
        EnrollmentUiState()
    )

    val uiState: StateFlow<EnrollmentUiState> = _uistate.asStateFlow()

    init {
        loadEnrollmentState()
    }

    fun loadEnrollmentState() {
        viewModelScope.launch {

            _uistate.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    initialCheckComplete = false
                )
            }

            try {
                val isDeviceOwner = deviceAdminManager.isDeviceOwner()

                _uistate.update {
                    it.copy(
                        isDeviceOwner = isDeviceOwner,
                        enrollmentStatus = when {
                            isDeviceOwner -> "DEVICE_OWNER"
                            deviceAdminManager.isAdminActive() -> "DEVICE_ADMIN"
                            else -> "NOT_ENROLLED"
                        }
                    )
                }
                checkAmapiEnvironment()
            } catch (e: Exception) {
                Logger.e("Failed to load enrollment state: ${e.message}")
                _uistate.update {
                    it.copy(
                        isLoading = false,
                        initialCheckComplete = true,
                        error = e.message
                    )
                }
            }
        }
    }

    private suspend fun checkAmapiEnvironment() {
        val environment = amapiEnvironmentManager.getEnvironment()

        if (environment == null) {
            _uistate.update{
                it.copy(
                    amapiEnvironmentAvailable = false,
                    androidDevicePolicyState = "UNKNOWN",
                    androidDevicePolicyVersion = "UNKNOWN",
                    isLoading = false,
                    initialCheckComplete = true,
                    error = "Unable to read AMAPI Environment"
                )
            }
            return
        }

        Logger.d(
            "Enrollment AMAPI Environment = $environment"
        )

        val androidDevicePolicyEnvironment = environment.androidDevicePolicyEnvironment
        val state = androidDevicePolicyEnvironment.state.toString()
        val version = androidDevicePolicyEnvironment.version.toString()

        Logger.d("Enrollment ADP State = $state")
        Logger.d("Enrollment ADP Version = $version")

        _uistate.update {
            it.copy(
                amapiEnvironmentAvailable = true,
                androidDevicePolicyState = state,
                androidDevicePolicyVersion = version,
                isLoading = false,
                initialCheckComplete = true,
                error = null
            )
        }
    }

    fun prepareEnvironment() {
        viewModelScope.launch {
            _uistate.update {
                it.copy(
                    isPreparingEnvironment = true,
                    error = null
                )
            }

            try {
                val response = amapiEnvironmentManager.prepareEnvironment()

                if(response != null) {
                    Logger.d(
                        "AMAPI Environment preparation successfully"
                    )

                    Logger.d(
                        "AMAPI Prepare Environment Response  = $response"
                    )
                    _uistate.update {
                        it.copy(
                            isPreparingEnvironment = false,
                            isEnvironmentPrepared = true,
                        )
                    }
                    // Read the Environment again after preparation
                    checkAmapiEnvironment()
                } else {
                    Logger.e(
                        "AMAPI Environment preparation failed"
                    )

                    _uistate.update {
                        it.copy(
                            isPreparingEnvironment = false,
                            isEnvironmentPrepared = false,
                            error = "Failed to Prepare AMAPI Environment"
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.e("Failed to prepare AMAPI Environment: ${e.message}")
                _uistate.update {
                    it.copy(
                        isPreparingEnvironment = false,
                        isEnvironmentPrepared = false,
                        error = e.message
                    )
                }
            }
        }
    }
}