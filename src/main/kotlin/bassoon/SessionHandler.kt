package bassoon

import bassoon.config.CallbackDto
import com.cloudhopper.commons.charset.CharsetUtil
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
        private val logger: Logger = logger<SessionHandler>()
) : DefaultSmppSessionHandler(logger) {

    private val callback: Callback = callbackConfig?.let {
        HttpCallback(config = it, smsc = client.name)
    } ?: NullCallback()

    override fun firePduRequestReceived(pduRequest: PduRequest<*>): PduResponse? = pduRequest.createResponse()

    override fun firePduReceived(pdu: Pdu?): Boolean =
            if (pdu is DeliverSm) {
                handleDeliverSm(pdu)
            } else {
                true
            }

    override fun fireChannelUnexpectedlyClosed() {
        logger.warn("Unexpected channel closed. Cleanup...")
        client.cleanup()
    }

    override fun fireUnknownThrowable(t: Throwable) = logger.error("WTF?!?", t)

    private fun isDeliveryReceipt(pdu: DeliverSm): Boolean {
        return pdu.getOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID) != null
    }

    private fun isPssrIndication(pdu: DeliverSm): Boolean {
        return pdu.getOptionalParameter(SmppConstants.TAG_USSD_SERVICE_OP).let { 1.toByte() == it?.value?.get(0) }
    }

    private fun handleDeliverSm(pdu: DeliverSm): Boolean {
        return when {
            isDeliveryReceipt(pdu) -> handleDeliveryReceipt(pdu)
            isPssrIndication(pdu) -> handleUssd(pdu)
            else -> handleIncomingSms(pdu)
        }
    }

    private fun handleDeliveryReceipt(pdu: DeliverSm): Boolean {
        // Игнорируем отчеты о доставке
        return true
    }

    private fun handleUssd(pdu: DeliverSm): Boolean {
        logger.info("USSD received...")
        val responseText = callback.run(pdu = pdu, charset = client.charset).responseText()
        respondUssdAsync(pdu, responseText)
        // NOTE: Возвращая false в `DefaultSmppSessionHandler::firePduReceived`,
        // мы прекращаем обработку в `DefaultSmppSession::firePduReceived`.
        // Это делается, чтобы освободить слушающий поток для обработки ответа на наш ответ
        return false
    }

    private fun handleIncomingSms(pdu: DeliverSm): Boolean {
        logger.info("SMS received...")
        callback.run(pdu = pdu, charset = guessCharset(pdu.dataCoding))
        return true
    }

    private fun respondUssdAsync(pdu: DeliverSm, responseText: String) {
        // NOTE: Запускаем процедуру ответа в отдельном потоке, чтобы у нас была возможность отпустить слушающий поток
        thread(
                name = "${client.name}-async-ussd-response",
                block = { client.respondUssd(pdu, responseText) }
        )
    }

    private fun guessCharset(dataCoding: Byte): String {
        return when(dataCoding) {
            SmppConstants.DATA_CODING_UCS2 -> CharsetUtil.NAME_UCS_2
            else -> CharsetUtil.NAME_ISO_8859_1
        }
    }
}
