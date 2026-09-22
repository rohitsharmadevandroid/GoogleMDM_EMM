package com.floydwiz.googlemdm.data.repository

import com.floydwiz.googlemdm.data.model.CheckInResult
import com.floydwiz.googlemdm.data.model.EnrollmentResult
import com.floydwiz.googlemdm.data.remote.ApiClient
import com.floydwiz.googlemdm.data.remote.AuthInterceptor
import com.floydwiz.googlemdm.enterprise.command.model.CommandOutcome
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EmmRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var credentialsStore: FakeDeviceCredentialsStore
    private lateinit var policyApplier: FakePolicyApplier
    private lateinit var commandExecutor: FakeCommandExecutor
    private lateinit var repository: EmmRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        credentialsStore = FakeDeviceCredentialsStore()
        policyApplier = FakePolicyApplier()
        commandExecutor = FakeCommandExecutor()
        val okHttpClient = ApiClient.buildOkHttpClient(
            authInterceptor = AuthInterceptor(credentialsStore),
            enableLogging = false
        )
        val apiService = ApiClient.buildEmmApiService(server.url("/").toString(), okHttpClient)
        repository = EmmRepository(apiService, credentialsStore, FakeDeviceInfoProvider(), policyApplier, commandExecutor)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `enroll success persists deviceId and deviceApiKey and returns Success`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"deviceId":"device-1","deviceApiKey":"secret-key","checkInIntervalSeconds":60}""")
                .addHeader("Content-Type", "application/json")
                .build()
        )

        val result = repository.enroll("ENROLL-TOKEN")

        assertTrue(result is EnrollmentResult.Success)
        result as EnrollmentResult.Success
        assertEquals("device-1", result.deviceId)
        assertEquals(60L, result.checkInIntervalSeconds)

        assertEquals("device-1", credentialsStore.getDeviceId())
        assertEquals("secret-key", credentialsStore.getDeviceApiKey())
        assertTrue(credentialsStore.isEnrolled())

        val recorded = server.takeRequest()
        assertEquals("/api/dpc/enroll", recorded.target)
        assertNull("enroll must not send an Authorization header", recorded.headers["Authorization"])
        assertTrue(recorded.body?.utf8()?.contains("\"enrollmentToken\":\"ENROLL-TOKEN\"") == true)
    }

    @Test
    fun `enroll http error returns Error and does not persist credentials`() = runTest {
        server.enqueue(MockResponse.Builder().code(400).body("").build())

        val result = repository.enroll("BAD-TOKEN")

        assertTrue(result is EnrollmentResult.Error)
        assertFalse(credentialsStore.isEnrolled())
    }

    @Test
    fun `check-in without stored credentials returns MissingCredentials and makes no request`() = runTest {
        val result = repository.checkIn()

        assertEquals(CheckInResult.MissingCredentials, result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `check-in sends bearer token and updates server-controlled interval`() = runTest {
        credentialsStore.saveEnrollment("device-1", "secret-key", 60L)

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"policy":null,"policyVersion":null,"pendingCommands":[],"checkInIntervalSeconds":120}""")
                .addHeader("Content-Type", "application/json")
                .build()
        )

        val result = repository.checkIn()

        assertTrue(result is CheckInResult.Success)
        result as CheckInResult.Success
        assertEquals(120L, result.checkInIntervalSeconds)
        assertFalse(result.hasPolicy)
        assertEquals(0, result.pendingCommandCount)
        assertEquals(120L, credentialsStore.getCheckInIntervalSeconds())
        assertTrue("policy is null (no change) - applier must not be invoked", policyApplier.appliedPayloads.isEmpty())
        assertNull("policy is null (no change) - must not overwrite lastPolicyVersionApplied", credentialsStore.getLastPolicyVersionApplied())

        val recorded = server.takeRequest()
        assertEquals("/api/dpc/checkin", recorded.target)
        assertEquals("Bearer secret-key", recorded.headers["Authorization"])
    }

    @Test
    fun `check-in applies a populated policy and persists the applied version`() = runTest {
        credentialsStore.saveEnrollment("device-1", "secret-key", 60L)

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(
                    """{"policy":{"cameraDisabled":true},"policyVersion":1,"pendingCommands":[],"checkInIntervalSeconds":60}"""
                )
                .addHeader("Content-Type", "application/json")
                .build()
        )

        val result = repository.checkIn()

        assertTrue(result is CheckInResult.Success)
        result as CheckInResult.Success
        assertTrue(result.hasPolicy)
        assertEquals(1, result.policyVersion)

        assertEquals(1, policyApplier.appliedPayloads.size)
        assertTrue(policyApplier.appliedPayloads.single().cameraDisabled)
        assertEquals(1, credentialsStore.getLastPolicyVersionApplied())
    }

    @Test
    fun `check-in echoes back lastPolicyVersionApplied on the next request`() = runTest {
        credentialsStore.saveEnrollment("device-1", "secret-key", 60L)
        credentialsStore.setLastPolicyVersionApplied(1)

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"policy":null,"policyVersion":1,"pendingCommands":[],"checkInIntervalSeconds":60}""")
                .addHeader("Content-Type", "application/json")
                .build()
        )

        repository.checkIn()

        val recorded = server.takeRequest()
        assertTrue(recorded.body?.utf8()?.contains("\"lastPolicyVersionApplied\":1") == true)
    }

    @Test
    fun `check-in returns Unauthorized on HTTP 401 without crashing`() = runTest {
        credentialsStore.saveEnrollment("device-1", "stale-key", 60L)
        server.enqueue(MockResponse.Builder().code(401).body("").build())

        val result = repository.checkIn()

        assertEquals(CheckInResult.Unauthorized, result)
    }

    @Test
    fun `check-in on HTTP 401 clears local enrollment so the device shows as unenrolled`() = runTest {
        credentialsStore.saveEnrollment("device-1", "stale-key", 60L)
        server.enqueue(MockResponse.Builder().code(401).body("").build())

        repository.checkIn()

        assertFalse(credentialsStore.isEnrolled())
        assertNull(credentialsStore.getDeviceId())
        assertNull(credentialsStore.getDeviceApiKey())
    }

    @Test
    fun `check-in executes a non-terminal command and acks it as completed`() = runTest {
        credentialsStore.saveEnrollment("device-1", "secret-key", 60L)
        commandExecutor.enqueueOutcome(CommandOutcome.Completed())

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(
                    """{"policy":null,"policyVersion":null,"pendingCommands":[{"commandId":"cmd-1","type":"LOCK",""" +
                        """"params":{"lockDurationSeconds":300,"newPassword":null,"resetPasswordFlags":[],""" +
                        """"wipeDataFlags":[],"clearAppsDataPackageNames":[],"requestDeviceInfoType":null}}],""" +
                        """"checkInIntervalSeconds":60}"""
                )
                .addHeader("Content-Type", "application/json")
                .build()
        )
        server.enqueue(MockResponse.Builder().code(200).body("").build())

        val result = repository.checkIn()

        assertTrue(result is CheckInResult.Success)
        assertEquals(1, (result as CheckInResult.Success).pendingCommandCount)
        assertEquals("cmd-1", commandExecutor.executedCommands.single().commandId)

        server.takeRequest() // checkin
        val ackRequest = server.takeRequest()
        assertEquals("/api/dpc/commands/cmd-1/ack", ackRequest.target)
        assertTrue(ackRequest.body?.utf8()?.contains("\"status\":\"COMPLETED\"") == true)
    }

    @Test
    fun `check-in acks a failed command with the failure message`() = runTest {
        credentialsStore.saveEnrollment("device-1", "secret-key", 60L)
        commandExecutor.enqueueOutcome(CommandOutcome.Failed("resetPassword() failed"))

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(
                    """{"policy":null,"policyVersion":null,"pendingCommands":[{"commandId":"cmd-2",""" +
                        """"type":"RESET_PASSWORD","params":{"lockDurationSeconds":null,"newPassword":"new-pw",""" +
                        """"resetPasswordFlags":[],"wipeDataFlags":[],"clearAppsDataPackageNames":[],""" +
                        """"requestDeviceInfoType":null}}],"checkInIntervalSeconds":60}"""
                )
                .addHeader("Content-Type", "application/json")
                .build()
        )
        server.enqueue(MockResponse.Builder().code(200).body("").build())

        repository.checkIn()

        server.takeRequest() // checkin
        val ackRequest = server.takeRequest()
        assertEquals("/api/dpc/commands/cmd-2/ack", ackRequest.target)
        assertTrue(ackRequest.body?.utf8()?.contains("\"status\":\"FAILED\"") == true)
        assertTrue(ackRequest.body?.utf8()?.contains("resetPassword() failed") == true)
    }

    @Test
    fun `check-in acks a terminal REBOOT command before invoking it, when device owner`() = runTest {
        credentialsStore.saveEnrollment("device-1", "secret-key", 60L)
        commandExecutor.readyForDestructiveCommand = true

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(
                    """{"policy":null,"policyVersion":null,"pendingCommands":[{"commandId":"cmd-3",""" +
                        """"type":"REBOOT","params":{"lockDurationSeconds":null,"newPassword":null,""" +
                        """"resetPasswordFlags":[],"wipeDataFlags":[],"clearAppsDataPackageNames":[],""" +
                        """"requestDeviceInfoType":null}}],"checkInIntervalSeconds":60}"""
                )
                .addHeader("Content-Type", "application/json")
                .build()
        )
        server.enqueue(MockResponse.Builder().code(200).body("").build())

        repository.checkIn()

        // processTerminalCommand() awaits the ack call before invoking
        // execute() - by the time execute() has recorded this command, the
        // ack HTTP call has already completed.
        assertEquals(2, server.requestCount)
        assertEquals("cmd-3", commandExecutor.executedCommands.single().commandId)

        server.takeRequest() // checkin
        val ackRequest = server.takeRequest()
        assertEquals("/api/dpc/commands/cmd-3/ack", ackRequest.target)
        assertTrue(ackRequest.body?.utf8()?.contains("\"status\":\"COMPLETED\"") == true)
    }

    @Test
    fun `check-in acks a terminal WIPE command as failed and never executes it when not device owner`() = runTest {
        credentialsStore.saveEnrollment("device-1", "secret-key", 60L)
        commandExecutor.readyForDestructiveCommand = false

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(
                    """{"policy":null,"policyVersion":null,"pendingCommands":[{"commandId":"cmd-4",""" +
                        """"type":"WIPE","params":{"lockDurationSeconds":null,"newPassword":null,""" +
                        """"resetPasswordFlags":[],"wipeDataFlags":[],"clearAppsDataPackageNames":[],""" +
                        """"requestDeviceInfoType":null}}],"checkInIntervalSeconds":60}"""
                )
                .addHeader("Content-Type", "application/json")
                .build()
        )
        server.enqueue(MockResponse.Builder().code(200).body("").build())

        repository.checkIn()

        assertTrue("WIPE must never be invoked when not Device Owner", commandExecutor.executedCommands.isEmpty())

        server.takeRequest() // checkin
        val ackRequest = server.takeRequest()
        assertEquals("/api/dpc/commands/cmd-4/ack", ackRequest.target)
        assertTrue(ackRequest.body?.utf8()?.contains("\"status\":\"FAILED\"") == true)
        assertTrue(ackRequest.body?.utf8()?.contains("not Device Owner") == true)
    }
}
