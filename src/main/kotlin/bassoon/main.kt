package bassoon

import bassoon.config.Config
import kotlin.concurrent.thread

const val CHECKING_TIMEOUT: Long = 5000

fun main(args: Array<String>) {
    val executor = ZkExecutor()
    val registry = ClientsRegistry(executor)

    Config.config.clients.forEach {
        val client = Client(it, registry)
        registry.add(client)
    }

    thread(name = "web") { WebApp(registry).run() }

    while (true) {
        registry.check()
        Thread.sleep(CHECKING_TIMEOUT)
    }
}