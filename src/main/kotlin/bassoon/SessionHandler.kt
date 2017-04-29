package bassoon

import bassoon.config.Config
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler
import com.cloudhopper.smpp.pdu.DeliverSm
import com.cloudhopper.smpp.pdu.Pdu
import com.cloudhopper.smpp.pdu.PduRequest
import com.cloudhopper.smpp.pdu.PduResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

class SessionHandler(
        var client: Client,
        val logger: Logger = LoggerFactory.getLogger(DefaultSmppSessionHandler::class.java)
) : DefaultSmppSessionHandler(logger) {

    val callback: Callback = if (Config.config.callback != null) {
        HttpCallback(config = Config.config.callback, smsc = client.name, charset = client.config.charset)
    } else {
        NullCallback()
    }

    override fun firePduRequestReceived(pduRequest: PduRequest<*>): PduResponse? {
        return pduRequest.createResponse()
    }

    override fun firePduReceived(pdu: Pdu?): Boolean {
        if (pdu is DeliverSm) {
            logger.info("USSD received...")
            val responseText = callback.run(pdu).responseText()

            thread(
                    name = "${client.name}-async-ussd-response",
                    block = { client.respondUssd(pdu, responseText) }
            )
            return false
        } else {
            return true
        }
    }

    override fun fireChannelUnexpectedlyClosed() {
        logger.info("Unexpected channel closed. Cleanup...")
        client.cleanup()
    }

    override fun fireUnknownThrowable(t: Throwable) {
        logger.info("WTF?!? UnknownThrowable: [${t::class}]")
    }
}
