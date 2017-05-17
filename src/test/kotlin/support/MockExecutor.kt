package support

import bassoon.Executor

class MockExecutor(val exists: Boolean = false, val exceeded: Boolean = false) : Executor {
    private val cleaned: MutableList<String> = mutableListOf()
    private val registered: MutableList<String> = mutableListOf()


    override fun isSessionExists(name: String): Boolean {
        return exists
    }

    override fun isSessionsNotLessThan(name: String, allowed: Int): Boolean {
        return exceeded
    }

    override fun register(name: String): String {
        registered.add(name)
        return "session001"
    }

    override fun unregister(registered: String) {
        this.registered.clear()
    }

    override fun cleanup(name: String) {
        cleaned.add(name)
    }

    fun wasCleaned(name: String): Boolean {
        return cleaned.contains(name)
    }

    fun wasRegistered(name: String): Boolean {
        return registered.contains(name)
    }
}
