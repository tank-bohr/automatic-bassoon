package bassoon

interface RegistrableClient {
    val name: String
    val allowedConnections: Int
    fun connect(): Boolean
    fun disconnect()
    fun isConnected(): Boolean
}
