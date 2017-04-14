package bassoon.config

data class CallbackDto(
        val host: String,
        val port: Int,
        val url: String,
        val useSsl: Boolean,
        val headers: Map<String, String>
)