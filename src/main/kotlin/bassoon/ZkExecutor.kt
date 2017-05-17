package bassoon

import org.apache.zookeeper.*

class ZkExecutor : Watcher, Executor {

    companion object {
        private const val ZK_SESSION_TIMEOUT = 5000
        private const val ZK_CLIENTS_PATH = "/automatic/bassoon/registry/clients"
        private const val ZK_NODE_NAME = "session"
    }

    private val connectionString = System.getenv("ZK_CONNECTION_STRING") ?: "localhost:2181"
    private val zkEmptyData = byteArrayOf()
    private val zk = ZooKeeper(connectionString, ZK_SESSION_TIMEOUT, this)

    init {
        createClientsPath()
    }

    override fun isSessionExists(name: String): Boolean {
        val clientPath = clientPath(name)
        val children = zk.getChildren(clientPath, false)
        return children.any { isMine("$clientPath/$it") }
    }

    override fun isSessionsNotLessThan(name: String, allowed: Int): Boolean = zk.exists(clientPath(name), false)
            ?.let { it.numChildren >= allowed }
            ?: false

    override fun register(name: String): String {
        val clientPath = clientPath(name)
        val nodePath = "$clientPath/$ZK_NODE_NAME"
        return zk.create(nodePath, zkEmptyData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL)
    }

    override fun unregister(registered: String) {
        zk.exists(registered, false)?.also { zk.delete(registered, -1) }
    }

    override fun cleanup(name: String) {
        val clientPath = clientPath(name)
        val children = zk.getChildren(clientPath, false)
        return children
                .map { "$clientPath/$it" }
                .filter(this::isMine)
                .forEach { zk.delete(it, -1) }
    }

    private fun createClientsPath() = ZK_CLIENTS_PATH.removePrefix("/").split("/")
            .fold("") { path, part -> ensureNodeExists("$path/$part") }

    private fun clientPath(name: String): String = ensureNodeExists("$ZK_CLIENTS_PATH/$name")

    private fun ensureNodeExists(path: String): String {
        if (zk.exists(path, false) == null) {
            zk.create(path, zkEmptyData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
        }
        return path
    }

    private fun isMine(path: String): Boolean = zk.exists(path, false)?.let { it.ephemeralOwner == zk.sessionId } ?: false

    override fun process(event: WatchedEvent?) {}
}
