package bassoon

import bassoon.config.CallbackDto
import com.cloudhopper.smpp.SmppConstants
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler
import com.cloudhopper.smpp.pdu.DeliverSm
import com.cloudhopper.smpp.pdu.Pdu
import com.cloudhopper.smpp.pdu.PduRequest
import com.cloudhopper.smpp.pdu.PduResponse
import org.slf4j.Logger
import kotlin.concurrent.thread

class SessionHandler(
        private val client: Client,
        callbackConfig: CallbackDto?,
        private val logger: Logger = logger<SessionHandler>()) : DefaultSmppSessionHandler(logger) {

    private val callback: Callback = callbackConfig?.let {
        HttpCallback(config = it, smsc = client.name, charset = client.charset)
    } ?: NullCallback()

    override fun firePduRequestReceived(pduRequest: PduRequest<*>): PduResponse? = pduRequest.createResponse()

    override fun firePduReceived(pdu: Pdu?): Boolean =
            if ((pdu is DeliverSm) && isUssd(pdu)) {
                logger.info("USSD received...")
                val responseText = callback.run(pdu).responseText()
                thread(
                        name = "${client.name}-async-ussd-response",
                        block = { client.respondUssd(pdu, responseText) }
                )
                false
            } else {
                true
            }

    override fun fireChannelUnexpectedlyClosed() {
        logger.warn("Unexpected channel closed. Cleanup...")
        client.cleanup()
    }

    override fun fireUnknownThrowable(t: Throwable) = logger.error("WTF?!?", t)

    private fun isUssd(pdu: DeliverSm): Boolean {
        return !isDeliveryReceipt(pdu) && isPssrIndication(pdu)
    }

    private fun isDeliveryReceipt(pdu: DeliverSm): Boolean {
        return pdu.getOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID) != null
    }

    private fun isPssrIndication(pdu: DeliverSm): Boolean {
        return pdu.getOptionalParameter(SmppConstants.TAG_USSD_SERVICE_OP).let { 1.toByte() == it?.value?.get(0) }
    }
}
