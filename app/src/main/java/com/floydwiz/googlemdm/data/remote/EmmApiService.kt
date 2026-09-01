package com.floydwiz.googlemdm.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface EmmApiService {

    @POST("api/dpc/enroll")
    suspend fun enroll(@Body request: EnrollRequestDto): Response<EnrollResponseDto>

    @POST("api/dpc/checkin")
    suspend fun checkIn(@Body request: CheckInRequestDto): Response<CheckInResponseDto>

    @POST("api/dpc/commands/{commandId}/ack")
    suspend fun acknowledgeCommand(
        @Path("commandId") commandId: String,
        @Body request: CommandAckRequestDto
    ): Response<Unit>
}
