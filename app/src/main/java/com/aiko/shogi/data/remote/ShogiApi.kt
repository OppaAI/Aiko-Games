package com.aiko.shogi.data.remote

import com.aiko.shogi.data.model.EngineStatus
import com.aiko.shogi.data.model.GameState
import com.aiko.shogi.data.model.LegalMovesResponse
import com.aiko.shogi.data.model.MoveRequest
import com.aiko.shogi.data.model.ResignResponse
import com.aiko.shogi.data.model.StartRequest
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

    @POST("api/games/shogi/resign")
    suspend fun resign(): ResignResponse
}
