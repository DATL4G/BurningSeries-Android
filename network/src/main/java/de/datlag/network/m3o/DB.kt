package de.datlag.network.m3o

import com.hadiyarajesh.flower.ApiResponse
import de.datlag.model.Constants
import de.datlag.model.m3o.db.Count
import de.datlag.model.m3o.db.create.BurningSeriesHoster
import de.datlag.model.m3o.db.read.BurningSeriesHosterQuery
import de.datlag.model.m3o.db.read.BurningSeriesHosterRecords
import io.michaelrocks.paranoid.Obfuscate
import kotlinx.coroutines.flow.Flow
import retrofit2.Call
import retrofit2.http.*

@Obfuscate
interface DB {

    @Headers("Content-Type: ${Constants.MEDIATYPE_JSON}")
    @POST("/v1/db/Count")
    fun countBurningSeries(@Header("Authorization") token: String, @Body body: Count.Request): Flow<ApiResponse<Count>>

    @Headers("Content-Type: ${Constants.MEDIATYPE_JSON}")
    @POST("/v1/db/Read")
    fun getStream(@Header("Authorization") token: String, @Body body: BurningSeriesHosterQuery): Flow<ApiResponse<BurningSeriesHosterRecords>>

    @Headers("Content-Type: ${Constants.MEDIATYPE_JSON}")
    @POST("/v1/db/Create")
    fun saveStream(@Header("Authorization") token: String, @Body body: BurningSeriesHoster): Call<Unit>
}