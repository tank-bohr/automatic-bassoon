package bassoon.config

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals

class ConfigSpec : Spek({
    describe("YAML parsing stuff") {
        beforeEachTest {
            System.setProperty("config", javaClass.classLoader.getResource("camelCasedConfig.yml").file)
        }

        it("parses config correctly") {
            val config = Config()
            val clients = config.clients
            val client = clients.first()
            val callback = config.callback

            assertEquals(1, clients.size)
            assertEquals("pants", client.name)
            assertEquals("smppclient1", client.systemId)
            assertEquals("password", client.password)
            assertEquals(0, client.dataCoding)
            assertEquals("ISO-8859-1", client.charset)

            // Check defaults
            assertEquals("localhost", client.host)
            assertEquals(2775, client.port)
            assertEquals(5, client.sourceTon)
            assertEquals(0, client.sourceNpi)
            assertEquals(1, client.destTon)
            assertEquals(1, client.destNpi)
            assertEquals(false, client.useMessagePayload)
            assertEquals(1, client.allowedConnections)

            // Check callback
            assertEquals("example.com", callback?.host)
            assertEquals(3000, callback?.port)
            assertEquals("http", callback?.scheme)
            assertEquals("/automatic/basson/callback", callback?.path)
            assertEquals("home", callback?.headers?.get("x-take-me"))
        }
    }
})
