package bassoon

import java.util.concurrent.ConcurrentHashMap

class ClientsRegistry(private val executor: Executor) {

    private val clients: ConcurrentHashMap<String, RegistrableClient> = ConcurrentHashMap()
    private val logger = logger<ClientsRegistry>()

    fun add(client: RegistrableClient) = clients.put(client.name, client)

    fun fetch(name: String): RegistrableClient? = clients[name]

    fun cleanup(name: String) = executor.cleanup(name)

    fun check() {
        logger.debug("Check cycle...")
        clients.values.forEach { checkClient(it) }
    }

    fun healthCheck() : HashMap<String, Boolean> {
        return HashMap<String, Boolean>().also { result ->
            clients.values.forEach { client ->
                result.put(client.name, isAlive(client))
            }
        }

    }

    private fun checkClient(client: RegistrableClient) {
        val name = client.name
        val allowedConnections = client.allowedConnections
        when {
            executor.isSessionExists(name) -> {
                logger.debug("Client $name is already connected")
                ensureConnected(client)
            }
            executor.isSessionsNotLessThan(name, allowedConnections) -> {
                logger.debug("Allowed connections number isSessionsNotLessThan")
                ensureDisconnected(client)
            }
            else -> {
                logger.debug("Register and connect [$name]...")
                registerAndConnect(client)
            }
        }
    }

    private fun registerAndConnect(client: RegistrableClient) {
        val registered = executor.register(client.name)
        var connected = false
        try {
            connected = client.connect()
        } finally {
            if (!connected) {
                executor.unregister(registered)
            }
        }
    }

    private fun ensureConnected(client: RegistrableClient) {
        if (!client.check()) {
            logger.error("Client [${client.name}] is not connected")
        }
    }

    private fun ensureDisconnected(client: RegistrableClient) {
        val connected = client.isConnected()
        if (connected) {
            logger.warn("Client [${client.name}] is connected, but it shouldn't. So disconnect")
            client.disconnect()
        }
    }

    private fun isAlive(client: RegistrableClient): Boolean {
        val name = client.name
        return if (executor.isSessionExists(name)) {
            client.isConnected()
        } else {
            executor.isSessionsNotLessThan(name, 1)
        }
    }
}
