package hu.toliver.matutine

const val BEARER_SESSION_KEY = "matutine.bearerToken"

expect fun readStoredBearerToken(): String?