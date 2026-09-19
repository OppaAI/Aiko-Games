package com.aiko.games.data.remote

import com.aiko.games.data.model.GoEngineStatus
import com.aiko.games.data.model.GoGameState
import com.aiko.games.data.model.GoLegalMovesResponse
import com.aiko.games.data.model.GoMoveRequest
import com.aiko.games.data.model.GoResignResponse
import com.aiko.games.data.model.GoStartRequest
import com.aiko.games.data.model.SelfplayStartRequest
import com.aiko.games.data.model.SelfplayState
import com.aiko.games.data.model.WarmupResponse
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

    @POST("api/games/go/selfplay/start")
    suspend fun selfplayStart(@Body body: SelfplayStartRequest = SelfplayStartRequest()): SelfplayState

    @GET("api/games/go/selfplay/state")
    suspend fun selfplayState(): SelfplayState

    @POST("api/games/go/selfplay/stop")
    suspend fun selfplayStop(): SelfplayState
}
