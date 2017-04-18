package bassoon

interface Executor {
    fun exists(name: String): Boolean
    fun exceeded(name: String, allowed: Int): Boolean
    fun register(name: String): String
    fun unregister(registered: String)
    fun cleanup(name: String)
}
