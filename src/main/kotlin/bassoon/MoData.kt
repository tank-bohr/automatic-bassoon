package bassoon

data class MoData(
        val source_address: String,
        val dest_address: String,
        val service_type: String,
        val short_message: String,
        val optional_parameters: Map<String, ByteArray>
)
