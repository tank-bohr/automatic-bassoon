package bassoon

import bassoon.config.ClientDto
import org.apache.zookeeper.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

const val CHECKING_TIMEOUT: Long = 5000
const val ZK_SESSION_TIMEOUT: Int = 5000
const val ZK_CLIENTS_PATH: String = "/automatic/bassoon/registry/clients"
const val ZK_NODE_NAME: String = "session"

class ClientsRegistry: Watcher {
    var clients: ConcurrentHashMap<String, Client> = ConcurrentHashMap()
    val zk: ZooKeeper = connectZk()
    val logger = LoggerFactory.getLogger(javaClass)

    fun add(config: ClientDto) {
        val client = Client(config)
        clients.put(client.name, client)
    }

    fun fetch(name: String): Client? {
        return clients[name]
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
        val allowedConnections = client.config.allowedConnections
        val clientPath = "$ZK_CLIENTS_PATH/${client.name}"
        ensureNodeExists(clientPath)
        if (needRegister(clientPath, allowedConnections)) {
            registerAndConnect(clientPath, client)
        }
    }

    private fun needRegister(path: String, allowedConnections: Int): Boolean {
        val children = zk.getChildren(path, false)
        val alreadyRegisterd = children.any {
            val stats = zk.exists(path, false)
            stats.ephemeralOwner == zk.sessionId
        }
        return !alreadyRegisterd && (children.size < allowedConnections)
    }

    private fun registerAndConnect(path: String, client: Client) {
        val nodePath = "$path/$ZK_NODE_NAME"
        val node = zk.create(nodePath, byteArrayOf(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL)
        client.connect(node)
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

    override fun process(event: WatchedEvent?) {
    }
}