package ru.cubeshield.cubecore.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// Импортируем наши DTO
import ru.cubeshield.cubecore.api.dto.*
import ru.cubeshield.cubecore.domain.SessionEntity

sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T) : ApiResponse<T>()
    data class Error(
        val exception: Throwable,
        val statusCode: HttpStatusCode? = null,
        val errorBody: String? = null
    ) : ApiResponse<Nothing>()
}

class ApiClient (
    private val client: HttpClient,
    private val json: Json,
    private val baseUrl: String,
    private val apiKey: String,
    private val apiScope: CoroutineScope
) {
    private suspend inline fun <reified T> safeApiCall(
        path: String,
        crossinline requestBuilder: HttpRequestBuilder.() -> Unit
    ): ApiResponse<T> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.request(baseUrl + path) {
                headers { append("X-API-Key", apiKey) }
                requestBuilder()
            }

            if (response.status.isSuccess()) {
                ApiResponse.Success(response.body<T>())
            } else {
                val errorBody = try { response.bodyAsText() } catch (e: Exception) { null }
                ApiResponse.Error(
                    exception = ClientRequestException(response, errorBody ?: "Unknown client error"),
                    statusCode = response.status,
                    errorBody = errorBody
                )
            }
        } catch (e: ClientRequestException) {
            val errorBody = try { e.response.bodyAsText() } catch (innerE: Exception) { null }
            ApiResponse.Error(e, e.response.status, errorBody)
        } catch (e: ServerResponseException) {
            val errorBody = try { e.response.bodyAsText() } catch (innerE: Exception) { null }
            ApiResponse.Error(e, e.response.status, errorBody)
        } catch (e: HttpRequestTimeoutException) {
            ApiResponse.Error(e, statusCode = HttpStatusCode.RequestTimeout)
        } catch (e: Exception) {
            ApiResponse.Error(e)
        }
    }

    suspend fun getPlayers(): ApiResponse<PlayersReadDto> {
        return safeApiCall("/players") {
            method = HttpMethod.Get
        }
    }

    suspend fun activateIntent(playerId: String, intentId: String): ApiResponse<Unit> {
        return safeApiCall("/players/$playerId/intents/$intentId") {
            method = HttpMethod.Post
        }
    }

    suspend fun getPlayer(playername: String): ApiResponse<PlayerReadDto> {
        return safeApiCall("/players/by-playername/$playername") {
            method = HttpMethod.Get
        }
    }

    suspend fun createPlayer(playername: String): ApiResponse<PlayerReadDto> {
        return safeApiCall("/players") {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(PlayerCreateDto(playername))
        }
    }

    suspend fun warnPlayerNewIp(playerId: String, newIp: String): ApiResponse<Unit> {
        return safeApiCall("/players/${playerId}/ip/warn/${newIp}") {
            method = HttpMethod.Post
        }
    }

    suspend fun successPlayerNewIp(playerId: String): ApiResponse<Unit> {
        return safeApiCall("/players/${playerId}/ip/success") {
            method = HttpMethod.Post
        }
    }

    suspend fun setPlayerStateOnline(playerId: String): ApiResponse<Unit> {
        return safeApiCall("/players/${playerId}/state/online") {
            method = HttpMethod.Post
        }
    }

    suspend fun setPlayerStateOffline(playerId: String): ApiResponse<Unit> {
        return safeApiCall("/players/${playerId}/state/offline") {
            method = HttpMethod.Post
        }
    }

    suspend fun setPlayerStateAfk(playerId: String): ApiResponse<Unit> {
        return safeApiCall("/players/${playerId}/state/afk") {
            method = HttpMethod.Post
        }
    }


    suspend fun createSession(playerId: String, sessionCreateDto: SessionCreateDto): ApiResponse<Unit> {
        return safeApiCall("/players/${playerId}/sessions") {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(sessionCreateDto)
        }
    }

    suspend fun createPlayerBankTransaction(playerId: String, amount: Int): ApiResponse<Unit> {
        return safeApiCall("/players/${playerId}/bank/${amount}") {
            method = HttpMethod.Post
        }
    }

    suspend fun createAutoPayBill(playerId: String, billCreateDto: BillCreateDto): ApiResponse<Unit> {
        return safeApiCall("/players/${playerId}/bill/autopay") {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(billCreateDto)
        }
    }

    suspend fun getPlayerProfile(playerId: String): ApiResponse<PlayerProfileReadDto> {
        return safeApiCall("/players/profile/${playerId}") {
            method = HttpMethod.Get
        }
    }

    suspend fun getItems(): ApiResponse<ItemsReadDto> {
        return safeApiCall("/players/items") {
            method = HttpMethod.Get
        }
    }

    suspend fun createItem(itemCreateDto: ItemCreateDto): ApiResponse<ItemReadDto> {
        return safeApiCall("/players/items") {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(itemCreateDto)
        }
    }

    suspend fun getDiscoveries(): ApiResponse<DiscoveriesReadDto> {
        return safeApiCall("/players/discoveries") {
            method = HttpMethod.Get
        }
    }

    suspend fun createDiscovery(playerId: String, discoveryCreateDto: DiscoveryCreateDto): ApiResponse<DiscoveryReadDto> {
        return safeApiCall("/players/$playerId/discoveries") {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(discoveryCreateDto)
        }
    }

}