package hu.toliver.matutine

data class MatutineBootstrap(
    val token: String,
)

expect fun consumeMatutineBootstrap(): MatutineBootstrap?
expect fun consumeBootstrapEditionJson(): String?