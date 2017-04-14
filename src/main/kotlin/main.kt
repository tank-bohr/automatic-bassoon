import bassoon.ClientsRegistry
import bassoon.WebApp
import bassoon.config.Config
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    val config = Config().loadFromResources()
    val registry = ClientsRegistry()
    config.clients.forEach { registry.add(it) }

    thread(
            name = "web",
            block = { WebApp(registry).run() }
    )

    registry.check()
}
