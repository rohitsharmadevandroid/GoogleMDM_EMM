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
    private val _uiState = MutableStateFlow(
        EnrollmentUiState()
    )

    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    init {
        loadEnrollmentState()
    }

    fun loadEnrollmentState() {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    initialCheckComplete = false
                )
            }

            try {
                val isDeviceOwner = deviceAdminManager.isDeviceOwner()

                val isAdminActive = deviceAdminManager.isAdminActive()

                _uiState.update {
                    it.copy(
                        isDeviceOwner = isDeviceOwner,
                        enrollmentStatus = when {
                            isDeviceOwner -> "DEVICE_OWNER"
                            isAdminActive -> "DEVICE_ADMIN"
                            else -> "NOT_ENROLLED"
                        }
                    )
                }
                checkAmapiEnvironment()
            } catch (e: Exception) {
                Logger.e("Failed to load enrollment state: ${e.message}")
                _uiState.update {
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
            _uiState.update{
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

        /* If AMAPI is already READY and UP_TO_DATE,
            then environment is already prepared
         */
        val environementReady =
            state == "READY" &&
            version == "UP_TO_DATE"
        _uiState.update {
            it.copy(
                amapiEnvironmentAvailable = true,
                androidDevicePolicyState = state,
                androidDevicePolicyVersion = version,
                isEnvironmentPrepared = environementReady,
                enrollmentCompleted = environementReady,
                isLoading = false,
                initialCheckComplete = true,
                error = null
            )
        }
    }

    fun prepareEnvironment() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPreparingEnvironment = true,
                    error = null,
                    enrollmentCompleted = false
                )
            }

            try {
                Logger.d(
                    "Starting AMAPI Environment preparation"
                )
                val response = amapiEnvironmentManager.prepareEnvironment()

                if(response == null) {
                    Logger.d(
                        "AMAPI Environment preparation successfully"
                    )

                    _uiState.update {

                        it.copy(
                            isPreparingEnvironment = false,
                            isEnvironmentPrepared = false,
                            enrollmentCompleted = false,
                            error = "Failed to Prepare AMAPI Environment"
                        )
                    }
                    return@launch
                }
                Logger.d(
                    "AMAPI Prepare Environment Response  = $response"
                )
                //Read Environment Again after preparation
                val environment = amapiEnvironmentManager.getEnvironment()
                val adpEnvironment = environment?.androidDevicePolicyEnvironment
                val state = adpEnvironment?.state?.toString()
                val version = adpEnvironment?.version?.toString()

                Logger.d("Final ADP State = $state")
                Logger.d("Final ADP Version = $version")

                val enrollmentSuccessful =
                    state == "READY" &&
                    version == "UP_TO_DATE"
                Logger.d(
                    "AMAPI environment ready = $enrollmentSuccessful"
                )
                 _uiState.update {

                     it.copy(
                         isPreparingEnvironment = false,
                         isEnvironmentPrepared = enrollmentSuccessful,
                         enrollmentCompleted = enrollmentSuccessful,
                         androidDevicePolicyState = state ?: "UNKNOWN",
                         androidDevicePolicyVersion = version ?: "UNKNOWN",
                         error =
                             if (enrollmentSuccessful) null
                             else "Failed to Prepare AMAPI Environment"
                     )
                 }
            } catch (e: Exception) {
                Logger.e("Failed to prepare AMAPI Environment: ${e.message}")
                _uiState.update {
                    it.copy(
                        isPreparingEnvironment = false,
                        error = e.message
                    )
                }
            }
        }
    }
}