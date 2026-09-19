package com.aiko.games.data.remote

import com.aiko.games.data.model.EngineStatus
import com.aiko.games.data.model.GameState
import com.aiko.games.data.model.LegalMovesResponse
import com.aiko.games.data.model.MoveRequest
import com.aiko.games.data.model.SelfplayStartRequest
import com.aiko.games.data.model.SelfplayState
import com.aiko.games.data.model.StartRequest
import com.aiko.games.data.model.WarmupResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ShogiApi {
    @POST("api/games/shogi/start")
    suspend fun start(@Body body: StartRequest = StartRequest()): GameState

    @POST("api/games/shogi/move")
    suspend fun move(@Body body: MoveRequest): GameState

    @GET("api/games/shogi/state")
    suspend fun state(): GameState

    @GET("api/games/shogi/legal-moves")
    suspend fun legalMoves(): LegalMovesResponse

    @GET("api/games/shogi/engine")
    suspend fun engine(): EngineStatus

    @POST("api/games/shogi/warmup")
    suspend fun warmup(): WarmupResponse

    @POST("api/games/shogi/resign")
    suspend fun resign(): GameState

    @POST("api/games/shogi/selfplay/start")
    suspend fun selfplayStart(@Body body: SelfplayStartRequest = SelfplayStartRequest()): SelfplayState

    @GET("api/games/shogi/selfplay/state")
    suspend fun selfplayState(): SelfplayState

    @POST("api/games/shogi/selfplay/stop")
    suspend fun selfplayStop(): SelfplayState
}
