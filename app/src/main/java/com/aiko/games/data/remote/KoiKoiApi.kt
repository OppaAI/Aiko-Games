package com.aiko.games.data.remote

import com.aiko.games.data.model.KoiEngineStatus
import com.aiko.games.data.model.KoiLegalMoves
import com.aiko.games.data.model.KoiMoveRequest
import com.aiko.games.data.model.KoiStartRequest
import com.aiko.games.data.model.KoiState
import com.aiko.games.data.model.SelfplayStartRequest
import com.aiko.games.data.model.SelfplayState
import com.aiko.games.data.model.WarmupResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface KoiKoiApi {
    @POST("api/games/koikoi/start")
    suspend fun start(@Body body: KoiStartRequest = KoiStartRequest()): KoiState

    @POST("api/games/koikoi/move")
    suspend fun move(@Body body: KoiMoveRequest): KoiState

    @GET("api/games/koikoi/state")
    suspend fun state(): KoiState

    @GET("api/games/koikoi/legal-moves")
    suspend fun legalMoves(): KoiLegalMoves

    @GET("api/games/koikoi/engine")
    suspend fun engine(): KoiEngineStatus

    @POST("api/games/koikoi/warmup")
    suspend fun warmup(): WarmupResponse

    @POST("api/games/koikoi/resign")
    suspend fun resign(): KoiState

    @POST("api/games/koikoi/selfplay/start")
    suspend fun selfplayStart(@Body body: SelfplayStartRequest = SelfplayStartRequest()): SelfplayState

    @GET("api/games/koikoi/selfplay/state")
    suspend fun selfplayState(): SelfplayState

    @POST("api/games/koikoi/selfplay/stop")
    suspend fun selfplayStop(): SelfplayState
}
