package bassoon

import bassoon.config.CallbackDto
import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.smpp.SmppConstants
import com.cloudhopper.smpp.pdu.DeliverSm
import com.cloudhopper.smpp.type.Address
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals

class HttpCallbackSpec : Spek({
    describe("run") {
        context("successful response") {
            val server = MockWebServer()
            server.enqueue(MockResponse()
                .setBody("{\"response_text\": \"Hi bassoon!\"}")
                .setResponseCode(200)
            )
            server.start()
            val url = server.url("/success/callback")

            it("responses with `response_text`") {
                val callback = HttpCallback(
                        config = CallbackDto(url),
                        smsc = "pants",
                        charset = "ISO-8859-1"
                )
                val response = callback.run(DeliverSm().also {
                    it.destAddress = Address(SmppConstants.TON_ALPHANUMERIC, SmppConstants.NPI_UNKNOWN, "1111")
                    it.sourceAddress = Address(SmppConstants.TON_INTERNATIONAL, SmppConstants.NPI_UNKNOWN, "1111")
                    it.shortMessage = CharsetUtil.encode("Take me home", "ISO-8859-1")
                    it.serviceType = "WTF"
                })
                assertEquals("Hi bassoon!", response.responseText())
            }
            afterGroup { server.shutdown() }
        }
    }
})
