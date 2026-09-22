package com.floydwiz.googlemdm.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelsTest {

    private val gson = Gson()

    @Test
    fun `enroll request serializes with exact backend field name`() {
        val json = gson.toJson(EnrollRequestDto(enrollmentToken = "TOKEN123"))

        assertEquals("""{"enrollmentToken":"TOKEN123"}""", json)
    }

    @Test
    fun `enroll response parses deviceId, deviceApiKey and interval`() {
        val json = """
            {"deviceId":"uuid-1","deviceApiKey":"secret-key","checkInIntervalSeconds":60}
        """.trimIndent()

        val response = gson.fromJson(json, EnrollResponseDto::class.java)

        assertEquals("uuid-1", response.deviceId)
        assertEquals("secret-key", response.deviceApiKey)
        assertEquals(60L, response.checkInIntervalSeconds)
    }

    @Test
    fun `check-in request omits all fields when null`() {
        val json = gson.toJson(CheckInRequestDto())

        assertEquals("{}", json)
    }

    @Test
    fun `check-in request includes only the fields that were supplied`() {
        val dto = CheckInRequestDto(
            osVersion = "14",
            model = "Pixel 7",
            manufacturer = "Google",
            lastPolicyVersionApplied = null
        )

        val json = gson.toJson(dto)

        assertTrue(json.contains("\"osVersion\":\"14\""))
        assertTrue(json.contains("\"model\":\"Pixel 7\""))
        assertTrue(json.contains("\"manufacturer\":\"Google\""))
        assertTrue(!json.contains("lastPolicyVersionApplied"))
    }

    @Test
    fun `check-in response parses null policy and empty pending commands`() {
        val json = """
            {"policy":null,"policyVersion":null,"pendingCommands":[],"checkInIntervalSeconds":60}
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)

        assertNull(response.policy)
        assertNull(response.policyVersion)
        assertEquals(0, response.pendingCommands?.size)
        assertEquals(60L, response.checkInIntervalSeconds)
    }

    @Test
    fun `check-in response parses a populated camera-disabled policy`() {
        val json = """
            {
              "policy": {
                "password": null,
                "cameraDisabled": true,
                "factoryResetDisabled": false,
                "screenCaptureDisabled": false,
                "usbFileTransferDisabled": false,
                "safeBootDisabled": false,
                "addUserDisabled": false,
                "outgoingCallsDisabled": false,
                "smsDisabled": false,
                "kioskMode": null,
                "appRestrictions": [],
                "wifi": null
              },
              "policyVersion": 1,
              "pendingCommands": [],
              "checkInIntervalSeconds": 60
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)

        assertEquals(1, response.policyVersion)
        assertEquals(true, response.policy?.cameraDisabled)
        assertEquals(false, response.policy?.factoryResetDisabled)
        assertEquals(false, response.policy?.screenCaptureDisabled)
        assertEquals(false, response.policy?.usbFileTransferDisabled)
        assertEquals(false, response.policy?.safeBootDisabled)
        assertEquals(false, response.policy?.addUserDisabled)
        assertEquals(false, response.policy?.outgoingCallsDisabled)
        assertEquals(false, response.policy?.smsDisabled)
    }

    @Test
    fun `check-in response parses a populated passwordPolicy`() {
        val json = """
            {
              "policy": {
                "password": {
                  "minimumLength": 6,
                  "quality": "PASSWORD_QUALITY_ALPHANUMERIC",
                  "maxFailedAttemptsBeforeWipe": 10
                },
                "cameraDisabled": true,
                "factoryResetDisabled": true,
                "screenCaptureDisabled": false,
                "usbFileTransferDisabled": true,
                "safeBootDisabled": true,
                "addUserDisabled": false,
                "outgoingCallsDisabled": false,
                "smsDisabled": false,
                "kioskMode": null,
                "appRestrictions": [],
                "wifi": null
              },
              "policyVersion": 1,
              "pendingCommands": [],
              "checkInIntervalSeconds": 60
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)

        assertEquals(6, response.policy?.passwordPolicy?.minimumLength)
        assertEquals("PASSWORD_QUALITY_ALPHANUMERIC", response.policy?.passwordPolicy?.quality)
        assertEquals(10, response.policy?.passwordPolicy?.maxFailedAttemptsBeforeWipe)
    }

    @Test
    fun `check-in response parses populated appRestrictions`() {
        val json = """
            {
              "policy": {
                "cameraDisabled": false,
                "factoryResetDisabled": false,
                "screenCaptureDisabled": false,
                "usbFileTransferDisabled": false,
                "safeBootDisabled": false,
                "addUserDisabled": false,
                "outgoingCallsDisabled": false,
                "smsDisabled": false,
                "appRestrictions": [
                  { "packageName": "com.example.requiredapp", "installType": "REQUIRED" },
                  { "packageName": "com.example.blockedapp", "installType": "BLOCKED" },
                  { "packageName": "com.example.availableapp", "installType": "AVAILABLE" }
                ]
              },
              "policyVersion": 1,
              "pendingCommands": [],
              "checkInIntervalSeconds": 60
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)
        val restrictions = response.policy?.appRestrictions

        assertEquals(3, restrictions?.size)
        assertEquals("com.example.requiredapp", restrictions?.get(0)?.packageName)
        assertEquals("REQUIRED", restrictions?.get(0)?.installType)
        assertEquals("BLOCKED", restrictions?.get(1)?.installType)
        assertEquals("AVAILABLE", restrictions?.get(2)?.installType)
    }

    @Test
    fun `check-in response parses a populated wifi config`() {
        val json = """
            {
              "policy": {
                "cameraDisabled": false,
                "factoryResetDisabled": false,
                "screenCaptureDisabled": false,
                "usbFileTransferDisabled": false,
                "safeBootDisabled": false,
                "addUserDisabled": false,
                "outgoingCallsDisabled": false,
                "smsDisabled": false,
                "wifi": {
                  "ssid": "TestCorpWifi",
                  "securityType": "WPA2_PSK",
                  "password": "TestPass123",
                  "hidden": false
                }
              },
              "policyVersion": 1,
              "pendingCommands": [],
              "checkInIntervalSeconds": 60
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)

        assertEquals("TestCorpWifi", response.policy?.wifiConfig?.ssid)
        assertEquals("WPA2_PSK", response.policy?.wifiConfig?.securityType)
        assertEquals("TestPass123", response.policy?.wifiConfig?.password)
        assertEquals(false, response.policy?.wifiConfig?.hidden)
    }

    @Test
    fun `check-in response parses a populated kioskMode policy`() {
        val json = """
            {
              "policy": {
                "cameraDisabled": false,
                "factoryResetDisabled": false,
                "screenCaptureDisabled": false,
                "usbFileTransferDisabled": false,
                "safeBootDisabled": false,
                "addUserDisabled": false,
                "outgoingCallsDisabled": false,
                "smsDisabled": false,
                "kioskMode": {
                  "enabled": true,
                  "allowedPackageNames": ["com.floydwiz.googlemdm"]
                }
              },
              "policyVersion": 1,
              "pendingCommands": [],
              "checkInIntervalSeconds": 60
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)

        assertEquals(true, response.policy?.kioskMode?.enabled)
        assertEquals(listOf("com.floydwiz.googlemdm"), response.policy?.kioskMode?.allowedPackageNames)
    }

    @Test
    fun `check-in response parses an empty allowedPackageNames kioskMode policy`() {
        val json = """
            {
              "policy": {
                "cameraDisabled": false,
                "factoryResetDisabled": false,
                "screenCaptureDisabled": false,
                "usbFileTransferDisabled": false,
                "safeBootDisabled": false,
                "addUserDisabled": false,
                "outgoingCallsDisabled": false,
                "smsDisabled": false,
                "kioskMode": {
                  "enabled": true,
                  "allowedPackageNames": []
                }
              },
              "policyVersion": 1,
              "pendingCommands": [],
              "checkInIntervalSeconds": 60
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)

        assertEquals(true, response.policy?.kioskMode?.enabled)
        assertEquals(emptyList<String>(), response.policy?.kioskMode?.allowedPackageNames)
    }

    @Test
    fun `check-in response parses a LOCK pending command with the full params shape`() {
        val json = """
            {
              "policy": null,
              "policyVersion": null,
              "pendingCommands": [
                {
                  "commandId": "36fb68b7-f915-4c01-b546-82d5c0c3dc82",
                  "type": "LOCK",
                  "params": {
                    "lockDurationSeconds": 300,
                    "newPassword": null,
                    "resetPasswordFlags": [],
                    "wipeDataFlags": [],
                    "clearAppsDataPackageNames": [],
                    "requestDeviceInfoType": null
                  }
                }
              ],
              "checkInIntervalSeconds": 60
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)
        val command = response.pendingCommands?.single()

        assertEquals("36fb68b7-f915-4c01-b546-82d5c0c3dc82", command?.commandId)
        assertEquals("LOCK", command?.type)
        assertEquals(300L, command?.params?.lockDurationSeconds)
        assertNull(command?.params?.newPassword)
        assertEquals(emptyList<String>(), command?.params?.wipeDataFlags)
    }

    @Test
    fun `check-in response parses a CLEAR_APP_DATA pending command with a populated package list`() {
        val json = """
            {
              "policy": null,
              "policyVersion": null,
              "pendingCommands": [
                {
                  "commandId": "91ef3267-8cf8-41ab-8a7d-d41f3ae7fd35",
                  "type": "CLEAR_APP_DATA",
                  "params": {
                    "lockDurationSeconds": null,
                    "newPassword": null,
                    "resetPasswordFlags": [],
                    "wipeDataFlags": [],
                    "clearAppsDataPackageNames": ["com.example.app1", "com.example.app2"],
                    "requestDeviceInfoType": null
                  }
                }
              ],
              "checkInIntervalSeconds": 60
            }
        """.trimIndent()

        val response = gson.fromJson(json, CheckInResponseDto::class.java)
        val command = response.pendingCommands?.single()

        assertEquals("CLEAR_APP_DATA", command?.type)
        assertEquals(
            listOf("com.example.app1", "com.example.app2"),
            command?.params?.clearAppsDataPackageNames
        )
    }

    @Test
    fun `command ack request omits resultData when null`() {
        val json = gson.toJson(CommandAckRequestDto(status = "COMPLETED"))

        assertEquals("""{"status":"COMPLETED"}""", json)
    }

    @Test
    fun `command ack request includes resultData when present`() {
        val json = gson.toJson(
            CommandAckRequestDto(
                status = "COMPLETED",
                resultData = mapOf("imei" to "490154203237518", "batteryLevel" to "84")
            )
        )

        assertTrue(json.contains("\"imei\":\"490154203237518\""))
        assertTrue(json.contains("\"batteryLevel\":\"84\""))
    }
}
