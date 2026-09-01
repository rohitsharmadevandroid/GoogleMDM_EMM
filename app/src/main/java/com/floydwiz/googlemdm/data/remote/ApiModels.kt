package com.floydwiz.googlemdm.data.remote

import com.google.gson.annotations.SerializedName

data class EnrollRequestDto(
    @SerializedName("enrollmentToken") val enrollmentToken: String
)

data class EnrollResponseDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceApiKey") val deviceApiKey: String,
    @SerializedName("checkInIntervalSeconds") val checkInIntervalSeconds: Long?
)

data class CheckInRequestDto(
    @SerializedName("osVersion") val osVersion: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("manufacturer") val manufacturer: String? = null,
    @SerializedName("lastPolicyVersionApplied") val lastPolicyVersionApplied: Int? = null
)

data class CheckInResponseDto(
    // Null means "no change since lastPolicyVersionApplied" - not "no policy
    // assigned." Only re-parse/apply when this is non-null.
    @SerializedName("policy") val policy: CustomDpcPolicyPayloadDto? = null,
    @SerializedName("policyVersion") val policyVersion: Int? = null,
    @SerializedName("pendingCommands") val pendingCommands: List<CommandDto>? = null,
    @SerializedName("checkInIntervalSeconds") val checkInIntervalSeconds: Long?
)

/**
 * Wire format for one pendingCommands[] entry. `params` is always the full
 * shape regardless of `type` - irrelevant fields arrive as null/[] rather
 * than being omitted, so every field here is nullable/defaulted.
 */
data class CommandParamsDto(
    @SerializedName("lockDurationSeconds") val lockDurationSeconds: Long? = null,
    @SerializedName("newPassword") val newPassword: String? = null,
    @SerializedName("resetPasswordFlags") val resetPasswordFlags: List<String> = emptyList(),
    @SerializedName("wipeDataFlags") val wipeDataFlags: List<String> = emptyList(),
    @SerializedName("clearAppsDataPackageNames") val clearAppsDataPackageNames: List<String> = emptyList(),
    @SerializedName("requestDeviceInfoType") val requestDeviceInfoType: String? = null
)

data class CommandDto(
    @SerializedName("commandId") val commandId: String,
    // One of: LOCK, WIPE, REBOOT, RESET_PASSWORD, CLEAR_APP_DATA, REQUEST_DEVICE_INFO
    @SerializedName("type") val type: String,
    @SerializedName("params") val params: CommandParamsDto
)

data class KioskModePayloadDto(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("allowedPackageNames") val allowedPackageNames: List<String> = emptyList()
)

/**
 * Field names deliberately match DevicePolicyManager's own PASSWORD_QUALITY_*
 * constant names, resolved at apply time via PasswordQualityResolver instead
 * of a hardcoded map - see PasswordQualityResolver.kt.
 */
data class PasswordPolicyPayloadDto(
    @SerializedName("minimumLength") val minimumLength: Int? = null,
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("maxFailedAttemptsBeforeWipe") val maxFailedAttemptsBeforeWipe: Int? = null
)

data class AppRestrictionPayloadDto(
    @SerializedName("packageName") val packageName: String,
    // One of: REQUIRED, BLOCKED, AVAILABLE
    @SerializedName("installType") val installType: String
)

/** A single network - the backend model supports only one per policy, not a list. */
data class WifiConfigPayloadDto(
    @SerializedName("ssid") val ssid: String,
    // One of: OPEN, WPA2_PSK confirmed; other values are logged and skipped, not guessed at.
    @SerializedName("securityType") val securityType: String,
    @SerializedName("password") val password: String? = null,
    @SerializedName("hidden") val hidden: Boolean = false
)

/**
 * Wire format for the custom (non-GMS) DPC policy payload - matches the
 * backend's CustomDpcPolicyPayload one-to-one (see mdm-backend's
 * policy/translator/CustomDpcPolicyPayload.kt).
 */
data class CustomDpcPolicyPayloadDto(
    @SerializedName("cameraDisabled") val cameraDisabled: Boolean = false,
    @SerializedName("factoryResetDisabled") val factoryResetDisabled: Boolean = false,
    @SerializedName("screenCaptureDisabled") val screenCaptureDisabled: Boolean = false,
    @SerializedName("usbFileTransferDisabled") val usbFileTransferDisabled: Boolean = false,
    @SerializedName("safeBootDisabled") val safeBootDisabled: Boolean = false,
    @SerializedName("addUserDisabled") val addUserDisabled: Boolean = false,
    @SerializedName("outgoingCallsDisabled") val outgoingCallsDisabled: Boolean = false,
    @SerializedName("smsDisabled") val smsDisabled: Boolean = false,
    @SerializedName("kioskMode") val kioskMode: KioskModePayloadDto? = null,
    @SerializedName("password") val passwordPolicy: PasswordPolicyPayloadDto? = null,
    @SerializedName("appRestrictions") val appRestrictions: List<AppRestrictionPayloadDto> = emptyList(),
    @SerializedName("wifi") val wifiConfig: WifiConfigPayloadDto? = null
)

data class CommandAckRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("errorMessage") val errorMessage: String? = null,
    // Only REQUEST_DEVICE_INFO populates this - a flat string map, e.g.
    // {"imei": "...", "batteryLevel": "84"}.
    @SerializedName("resultData") val resultData: Map<String, String>? = null
)
