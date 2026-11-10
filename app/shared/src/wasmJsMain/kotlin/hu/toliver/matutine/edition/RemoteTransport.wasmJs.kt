package hu.toliver.matutine.edition

actual fun openRemoteTransport(baseUrl: String): RemoteEditionTransport {
    error("The browser does not open a device remote repository: $baseUrl")
}
