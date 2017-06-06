package support

import bassoon.RegistrableClient

class MockClient(
        override val name: String = "pants",
        override val allowedConnections: Int = 17,
        val canBeConnected: Boolean = true
) : RegistrableClient {

    private var isConnected: Boolean = false

    override fun connect(): Boolean {
        return if (canBeConnected) {
            isConnected = true
            true
        } else {
            false
        }
    }

    override fun check(): Boolean = true

    override fun disconnect() {}

    override fun isConnected(): Boolean = isConnected
}
