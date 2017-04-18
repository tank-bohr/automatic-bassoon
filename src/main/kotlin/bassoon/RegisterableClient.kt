package bassoon

interface RegisterableClient {
    val name: String
    val allowedConnections: Int
    fun connect(): Boolean
    fun isConnected(): Boolean
}
