import bassoon.ClientsRegistry
import bassoon.WebApp
import bassoon.ZkExecutor
import bassoon.config.Config
import kotlin.concurrent.thread

const val CHECKING_TIMEOUT: Long = 5000

fun main(args: Array<String>) {
    val executor = ZkExecutor()
    val registry = ClientsRegistry(executor)
    Config.config.clients.forEach { registry.add(it) }

    thread(name = "web") { WebApp(registry).run() }
    while (true) {
        registry.check()
        Thread.sleep(CHECKING_TIMEOUT)
    }
}
