package bassoon

import bassoon.config.ClientDto
import org.apache.zookeeper.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

const val CHECKING_TIMEOUT: Long = 5000
const val ZK_SESSION_TIMEOUT: Int = 5000
const val ZK_CLIENTS_PATH: String = "/automatic/bassoon/registry/clients"
const val ZK_NODE_NAME: String = "session"

class ClientsRegistry: Watcher {
    private var clients: ConcurrentHashMap<String, Client> = ConcurrentHashMap()
    private val zk: ZooKeeper = connectZk()
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun add(config: ClientDto) {
        val client = Client(config, registry = this)
        clients.put(client.name, client)
    }

    fun fetch(name: String): Client? {
        return clients[name]
    }

    fun cleanup(name: String) {
        val session = mySession(name)
        if (session != null) {
            val path = "${clientPath(name)}/$session"
            zk.delete(path, -1)
        }
    }

    fun check() {
        createClientsPath()
        while (true) {
            logger.debug("Check cycle...")
            check_all_clients()
            Thread.sleep(CHECKING_TIMEOUT)
        }
    }

    private fun check_all_clients() {
        for (client in clients.values) {
            check_client(client)
        }
    }

    private fun check_client(client: Client) {
        val name = client.name

        val session = mySession(name)
        if (session != null) {
            logger.debug("Client $name is already connected")
            return
        }

        val clientPath = clientPath(name)
        val allowedConnections = client.config.allowedConnections
        val stats = zk.exists(clientPath, false)
        if (stats.numChildren >= allowedConnections) {
            logger.debug("Allowed connection number exceeded")
            return
        }

        logger.debug("Register and connect [$name]...")
        registerAndConnect(client)
    }

    private fun registerAndConnect(client: Client) {
        val name = client.name
        val clientPath = clientPath(name)
        val nodePath = "$clientPath/$ZK_NODE_NAME"
        val node = zk.create(nodePath, byteArrayOf(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL)
        var connected = false
        try {
            connected = client.connect(node)
        }
        finally {
            if (!connected) {
                cleanup(name)
            }
        }

    }

    private fun connectZk(): ZooKeeper {
        val connectionString = System.getenv("ZK_CONNECTION_STRING") ?: "localhost:2181"
        val zk = ZooKeeper(connectionString, ZK_SESSION_TIMEOUT, this)
        return zk
    }

    private fun createClientsPath() {
        val parts = ZK_CLIENTS_PATH.removePrefix("/").split("/")
        parts.fold(
                initial = "",
                operation = { path, part -> ensureNodeExists("$path/$part") }
        )
    }

    private fun ensureNodeExists(path: String): String {
        val stats = zk.exists(path, false)
        if (stats == null) zk.create(path, byteArrayOf(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
        return path
    }

    private fun clientPath(name: String): String {
        return ensureNodeExists("$ZK_CLIENTS_PATH/$name")
    }

    private fun mySession(name: String): String? {
        val clientPath = clientPath(name)
        val children = zk.getChildren(clientPath, false)
        return children.find {
            val stats = zk.exists("$clientPath/$it", false)
            stats.ephemeralOwner == zk.sessionId
        }
    }

    override fun process(event: WatchedEvent?) {
    }
}