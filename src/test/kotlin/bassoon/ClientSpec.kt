package bassoon

import bassoon.config.ClientDto
import com.cloudhopper.smpp.SmppConstants
import com.cloudhopper.smpp.simulator.SmppSimulatorServer
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import support.SmppSimulatorSimpleProcessor
import kotlin.test.assertEquals

class ClientSpec : Spek({
    describe("Client") {
        val server = SmppSimulatorServer()
        server.handler.defaultPduProcessor = SmppSimulatorSimpleProcessor("smpptest", "smpppassword")

        beforeGroup { server.start(9785) }

        afterGroup { server.stop() }

        val config = ClientDto(
                name = "smpp-test",
                systemId = "smpptest",
                password = "smpppassword",
                port = 9785
        )

        val client = Client(config)

        describe("connect") {
            it("connects") {
                val result = client.connect()
                assertEquals(true, result)
                assertEquals(true, client.isConnected())
            }

            context("when password is incorrect") {
                val wrongPasswordConfig = ClientDto(
                        name = "smpp-test",
                        systemId = "smpptest",
                        password = "wrongpassword",
                        port = 9785
                )

                val wrongPasswordClient = Client(wrongPasswordConfig)

                it("doesn't connect") {
                    val result = wrongPasswordClient.connect()
                    assertEquals(false, result)
                    assertEquals(false, wrongPasswordClient.isConnected())
                }
            }
        }

        describe("disconnect") {
            it("disconnects") {
                client.disconnect()
                assertEquals(false, client.isConnected())
            }
        }

        describe("isConnected") {
            context("when connected") {
                beforeEachTest { client.connect() }

                it("returns true") {
                    assertEquals(true, client.isConnected())
                }
            }

            context("when disconnected") {
                beforeEachTest { client.disconnect() }

                it("returns false") {
                    assertEquals(false, client.isConnected())
                }
            }
        }

        describe("submit") {
            context("when connected") {
                beforeEachTest { client.connect() }

                it("responses with OK") {
                    val pdu = client.submit()
                    assertEquals(SmppConstants.STATUS_OK, pdu?.commandStatus)
                }
            }
        }
    }
})