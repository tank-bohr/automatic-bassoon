package bassoon

import bassoon.config.ClientDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class ClientsRegistry(val executor: Executor) {
    private var clients: ConcurrentHashMap<String, Client> = ConcurrentHashMap()
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun add(config: ClientDto) {
        val client = Client(config, registry = this)
        clients.put(client.name, client)
    }

    fun fetch(name: String): Client? {
        return clients[name]
    }

    fun cleanup(name: String) {
        executor.cleanup(name)
    }

    fun check() {
        logger.debug("Check cycle...")
        for (client in clients.values) {
            check_client(client)
        }
    }


    private fun check_client(client: Client) {
        val name = client.name
        val allowedConnections = client.config.allowedConnections

        if (executor.exists(name)) {
            logger.debug("Client $name is already connected")
            return
        }
        if (executor.exceeded(name, allowedConnections)) {
            logger.debug("Allowed connections number exceeded")
            return
        }

        logger.debug("Register and connect [$name]...")
        registerAndConnect(client)
    }

    private fun registerAndConnect(client: Client) {
        val name = client.name
        val registered = executor.register(name)
        var connected = false
        try {
            connected = client.connect()
        }
        finally {
            if (!connected) { executor.unregister(registered) }
        }
    }
}
