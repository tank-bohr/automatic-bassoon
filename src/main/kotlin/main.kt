import bassoon.ClientsRegistry
import bassoon.config.Config

fun main(args: Array<String>) {
    val config = Config().loadFromResources()
    val registry = ClientsRegistry()
    for (clientConfig in config.clients) {
        registry.add(clientConfig)
    }
    registry.check()
}
