package bassoon

interface Executor {
    fun isSessionExists(name: String): Boolean
    fun isSessionsNotLessThan(name: String, allowed: Int): Boolean
    fun register(name: String): String
    fun unregister(registered: String)
    fun cleanup(name: String)
}
