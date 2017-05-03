package bassoon

data class MoData(
        val sourceAddress: String,
        val destAddress: String,
        val serviceType: String,
        val shortMessage: String,
        val optionalParameters: Map<String, ByteArray>)
