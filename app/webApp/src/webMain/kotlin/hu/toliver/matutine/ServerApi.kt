package hu.toliver.matutine

data class ApiResponse(
    val status: Int,
    val body: String,
)

expect suspend fun apiGet(path: String, token: String): ApiResponse

expect suspend fun apiPut(path: String, token: String, body: String): ApiResponse

expect suspend fun apiPost(path: String, token: String, body: String = ""): ApiResponse
