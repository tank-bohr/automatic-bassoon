package basson

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler
import com.cloudhopper.smpp.pdu.DeliverSm
import com.cloudhopper.smpp.pdu.PduRequest
import com.cloudhopper.smpp.pdu.PduResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory



class SesssionHandler(
        var client: Client,
        val logger: Logger = LoggerFactory.getLogger(DefaultSmppSessionHandler::class.java)
) : DefaultSmppSessionHandler(logger) {

    override fun firePduRequestReceived(pduRequest: PduRequest<*>): PduResponse? {
        if (pduRequest is DeliverSm) {
            client.respondUssd(pduRequest)
        }
        return pduRequest.createResponse()
    }

    override fun fireChannelUnexpectedlyClosed() {
        super.fireChannelUnexpectedlyClosed()
        client.cleanup()
    }

    override fun fireUnknownThrowable(t: Throwable) {
        logger.info("WTF?!? UnknownThrowable: [${t::class}]")
    }
}