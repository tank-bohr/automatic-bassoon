package basson

import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.smpp.*
import com.cloudhopper.smpp.impl.DefaultSmppClient
import com.cloudhopper.smpp.pdu.*
import com.cloudhopper.smpp.tlv.Tlv
import com.cloudhopper.smpp.type.Address
import com.cloudhopper.smpp.type.SmppBindException
import com.cloudhopper.smpp.type.SmppChannelConnectException
import com.cloudhopper.smpp.type.SmppChannelConnectTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Client(var client: SmppClient = DefaultSmppClient()) {
    private val config = buildSessionConfiguration()
    private val sessionHandler: SesssionHandler = SesssionHandler(this)
    private var session: SmppSession? = null
    private val logger: Logger = LoggerFactory.getLogger(DefaultSmppClient::class.java)

    private val pssrResponse: Tlv = Tlv(
            SmppConstants.TAG_USSD_SERVICE_OP,
            byteArrayOf(17),
            SmppConstants.TAG_NAME_MAP[SmppConstants.TAG_USSD_SERVICE_OP]
    )

    fun connect() {
        try { if (!isConnected()) session = bind() }
        catch(e: SmppChannelConnectException) { }
        catch(e: SmppChannelConnectTimeoutException) { }
        catch(e: SmppBindException) { }
    }

    fun disconnect() {
        session?.unbind(300_000)
        cleanup()
    }

    fun cleanup() {
        session?.close()
        session?.destroy()
        session = null
        client.destroy()
    }

    fun isConnected(): Boolean {
        return session != null
    }

    fun submit(
            from: String = "Automatic Bassoon",
            to: String = "79261234567",
            payload: String = "Hello"
    ): PduResponse? {
        var sm = SubmitSm()
        sm.sourceAddress = Address(SmppConstants.TON_ALPHANUMERIC, SmppConstants.NPI_UNKNOWN, from)
        sm.destAddress = Address(SmppConstants.TON_INTERNATIONAL, SmppConstants.NPI_E164, to)
        sm.dataCoding = SmppConstants.DATA_CODING_UCS2
        sm.shortMessage = CharsetUtil.encode(payload, "UCS-2")

        return session?.submit(sm, 300_000)
    }

    fun respondUssd(deliverSm: DeliverSm, responseText: String = "OK") {
        var submitSm = SubmitSm()
        submitSm.sourceAddress = deliverSm.destAddress
        submitSm.destAddress = deliverSm.sourceAddress
        submitSm.dataCoding = SmppConstants.DATA_CODING_DEFAULT
        submitSm.shortMessage = CharsetUtil.encode(responseText, "ISO-8859-1")
        if (isPssrIndication(deliverSm)) {
            submitSm.addOptionalParameter(pssrResponse)
        }
        logger.info("[fun respondUssd] Sending SubmitSm...")
        val future = session?.sendRequestPdu(submitSm, 300_000, false)
        future ?: return
        val completedWithinTimeout = future.await()
        if (!completedWithinTimeout) {
            logger.info("[fun respondUssd] Failed to receive response within specified time")
            future.cancel()
        } else if (future.isSuccess) {
            val pduResponse = future.response
            logger.info("[fun respondUssd] Received response: [$pduResponse]")
        } else {
            logger.error("[fun respondUssd] Failed: [${future.cause}]")
        }
    }

    private fun buildSessionConfiguration(): SmppSessionConfiguration {
        var sc = SmppSessionConfiguration()
        sc.name = "basson"
        sc.type = SmppBindType.TRANSCEIVER
        sc.host = "localhost"
        sc.port = 2775
        sc.systemId = "smppclient1"
        sc.password = "password"
        sc.bindTimeout = 300_000
        return sc
    }

    private fun bind(): SmppSession {
        return client.bind(config, sessionHandler)
    }

    private fun isPssrIndication(pdu: Pdu): Boolean {
        val targetValue: Byte = 1
        val ussdServiceOp = pdu.getOptionalParameter(SmppConstants.TAG_USSD_SERVICE_OP)
        if (ussdServiceOp == null) {
            return false
        }
        else {
            return targetValue == ussdServiceOp.value?.get(0)
        }
    }
}
