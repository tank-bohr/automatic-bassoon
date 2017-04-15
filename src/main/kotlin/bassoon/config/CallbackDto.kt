package bassoon.config

data class CallbackDto(
        val scheme: String,
        val host: String,
        val port: Int,
        val path: String,
        val headers: Map<String, String>
)