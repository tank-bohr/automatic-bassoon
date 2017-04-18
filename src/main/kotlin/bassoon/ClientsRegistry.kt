package bassoon

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class ClientsRegistry(val executor: Executor) {
    private var clients: ConcurrentHashMap<String, RegistrableClient> = ConcurrentHashMap()
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun add(client: RegistrableClient) {
        clients.put(client.name, client)
    }

    fun fetch(name: String): RegistrableClient? {
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


    private fun check_client(client: RegistrableClient) {
        val name = client.name
        val allowedConnections = client.allowedConnections

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

    private fun registerAndConnect(client: RegistrableClient) {
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
