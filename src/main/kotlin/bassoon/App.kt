package bassoon

import bassoon.config.Config
import kotlin.concurrent.thread

class App {
    private val logger = logger<App>()
    private val executor = ZkExecutor()
    private val registry = ClientsRegistry(executor)
    private val config = Config()

    companion object {
        const val CHECKING_TIMEOUT: Millis = 5000
    }

    fun run() {
        try {
            initRegistry()
            runWebApp()
            runMainLoop()
        } catch (t: Throwable) {
            logger.error("System exit", t)
            System.exit(1)
        }
    }

    private fun initRegistry() {
        config.clients.forEach {
            val client = Client(config = it, registry = registry, callbackConfig = config.callback)
            registry.add(client)
        }
    }

    private fun runWebApp() {
        thread(name = "web") { WebApp(registry).run() }
    }

    private fun runMainLoop() {
        while (true) {
            registry.check()
            Thread.sleep(CHECKING_TIMEOUT)
        }
    }
}
