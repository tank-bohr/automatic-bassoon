package bassoon

import org.apache.zookeeper.*

const val ZK_SESSION_TIMEOUT: Int = 5000
const val ZK_CLIENTS_PATH: String = "/automatic/bassoon/registry/clients"
const val ZK_NODE_NAME: String = "session"


class ZkExecutor : Watcher, Executor {
    private val connectionString: String = System.getenv("ZK_CONNECTION_STRING") ?: "localhost:2181"
    private val zkEmptyData = byteArrayOf()
    private val zk: ZooKeeper = ZooKeeper(connectionString, ZK_SESSION_TIMEOUT, this)

    init {
        createClientsPath()
    }

    override fun exists(name: String): Boolean {
        val clientPath = clientPath(name)
        val children = zk.getChildren(clientPath, false)
        return children.any { mine("$clientPath/$it") }
    }

    override fun exceeded(name: String, allowed: Int): Boolean {
        val clientPath = clientPath(name)
        val stat = zk.exists(clientPath, false)
        return if (stat == null) {
            false
        } else {
            stat.numChildren >= allowed
        }
    }

    override fun register(name: String): String {
        val clientPath = clientPath(name)
        val nodePath = "$clientPath/$ZK_NODE_NAME"
        return zk.create(nodePath, zkEmptyData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL)
    }

    override fun unregister(registered: String) {
        val stat = zk.exists(registered, false)
        if (stat != null) {
            zk.delete(registered, -1)
        }
    }


    override fun cleanup(name: String) {
        val clientPath = clientPath(name)
        val children = zk.getChildren(clientPath, false)
        return children
                .map { "$clientPath/$it" }
                .filter(this::mine)
                .forEach { zk.delete(it, -1) }
    }

    private fun createClientsPath() {
        val parts = ZK_CLIENTS_PATH.removePrefix("/").split("/")
        parts.fold("") { path, part -> ensureNodeExists("$path/$part") }
    }

    private fun clientPath(name: String): String {
        return ensureNodeExists("$ZK_CLIENTS_PATH/$name")
    }

    private fun ensureNodeExists(path: String): String {
        val stat = zk.exists(path, false)
        if (stat == null) {
            zk.create(path, zkEmptyData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
        }
        return path
    }

    private fun mine(path: String): Boolean {
        val stat = zk.exists(path, false)
        return if (stat == null) {
            false
        } else {
            stat.ephemeralOwner == zk.sessionId
        }
    }

    override fun process(event: WatchedEvent?) {
    }
}
