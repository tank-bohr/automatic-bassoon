package bassoon.config

import okhttp3.HttpUrl

class CallbackDto(
        val scheme: String,
        val host: String,
        val port: Int,
        val path: String,
        val headers: Map<String, String>
) {
    constructor(url: HttpUrl) : this(
            scheme = url.scheme(),
            host = url.host(),
            port = url.port(),
            path = url.encodedPath(),
            headers = mapOf()
    )
}
