package support

import bassoon.RegisterableClient

class FakeClient(
        override val name: String = "pants",
        override val allowedConnections: Int = 17,
        val canBeConnected: Boolean = true
) : RegisterableClient {

    private var isConnected: Boolean = false

    override fun connect(): Boolean {
        return if (canBeConnected) {
            isConnected = true
            true
        } else {
            false
        }
    }

    override fun isConnected(): Boolean = isConnected
}
