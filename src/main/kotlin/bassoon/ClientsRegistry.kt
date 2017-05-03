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

    private fun checkClient(client: RegistrableClient) {
        val name = client.name
        val allowedConnections = client.allowedConnections
        when {
            executor.exists(name) -> logger.debug("Client $name is already connected")
            executor.exceeded(name, allowedConnections) -> logger.debug("Allowed connections number exceeded")
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
}
