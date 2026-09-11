package com.aiko.shogi.data.remote

import com.aiko.shogi.data.model.GoEngineStatus
import com.aiko.shogi.data.model.GoGameState
import com.aiko.shogi.data.model.GoLegalMovesResponse
import com.aiko.shogi.data.model.GoMoveRequest
import com.aiko.shogi.data.model.GoResignResponse
import com.aiko.shogi.data.model.GoStartRequest
import com.aiko.shogi.data.model.WarmupResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GoApi {
    @POST("api/games/go/start")
    suspend fun start(@Body body: GoStartRequest = GoStartRequest()): GoGameState

    @POST("api/games/go/move")
    suspend fun move(@Body body: GoMoveRequest): GoGameState

    @GET("api/games/go/state")
    suspend fun state(): GoGameState

    @GET("api/games/go/legal-moves")
    suspend fun legalMoves(): GoLegalMovesResponse

    @GET("api/games/go/engine")
    suspend fun engine(): GoEngineStatus

    @POST("api/games/go/warmup")
    suspend fun warmup(): WarmupResponse

    @POST("api/games/go/resign")
    suspend fun resign(): GoResignResponse
}
