package bassoon

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import support.FakeClient
import support.FakeExecutor
import kotlin.test.assertEquals

class ClientsRegistrySpec : Spek({

    val defaultClient = FakeClient()

    describe("add/fetch") {
        it("adds a client which can be fetched") {
            val registry = ClientsRegistry(FakeExecutor())
            registry.add(defaultClient)
            val client = registry.fetch("pants")
            assertEquals(defaultClient, client)
        }
    }

    describe("cleanup") {
        it("calls Exucutor::cleanup") {
            val executor = FakeExecutor()
            val registry = ClientsRegistry(executor)
            registry.cleanup("some-client")

            assertEquals(true, executor.wasCleaned("some-client"))
        }
    }

    describe("check") {
        context("when client already connected") {
            it("doesn't register") {
                val executor = FakeExecutor(exists = true)
                val registry = ClientsRegistry(executor)
                registry.add(defaultClient)

                registry.check()

                assertEquals(false, executor.wasRegistered(defaultClient.name))
            }
        }

        context("when exceeded allowed connections") {
            it("doesn't register") {
                val executor = FakeExecutor(exceeded = true)
                val registry = ClientsRegistry(executor)
                registry.add(defaultClient)

                registry.check()

                assertEquals(false, executor.wasRegistered(defaultClient.name))
            }
        }

        context("when everything is ok") {
            it("registers and connects client") {
                val executor = FakeExecutor()
                val registry = ClientsRegistry(executor)
                val client = FakeClient()
                registry.add(client)

                registry.check()

                assertEquals(true, executor.wasRegistered(defaultClient.name))
                assertEquals(true, client.isConnected())
            }
        }
    }
})
