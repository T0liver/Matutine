package hu.toliver.matutine

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform