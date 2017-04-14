package bassoon

import bassoon.config.ClientDto
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

// Timeouts in milliseconds
const val BIND_TIMEOUT: Long = 300_000
const val UNBIND_TIMEOUT: Long = 300_000
const val SUBMIT_TIMEOUT: Long = 300_000

class Client(val config: ClientDto) {
    val name: String = config.name
    var zkNode: String? = null
        private set
    private val client: SmppClient = DefaultSmppClient()
    private val sessionConfig = buildSessionConfiguration()
    private val sessionHandler: SesssionHandler = SesssionHandler(this)
    private var session: SmppSession? = null
    private val logger: Logger = LoggerFactory.getLogger(DefaultSmppClient::class.java)
    private val pssrResponse: Tlv = Tlv(
            SmppConstants.TAG_USSD_SERVICE_OP,
            byteArrayOf(17),
            SmppConstants.TAG_NAME_MAP[SmppConstants.TAG_USSD_SERVICE_OP]
    )

    fun connect(zkNodePath: String? = null) {
        try {
            if (!isConnected()) {
                session = bind()
                zkNode = zkNodePath
            }
        }
        catch(e: SmppChannelConnectException) { }
        catch(e: SmppChannelConnectTimeoutException) { }
        catch(e: SmppBindException) { }
    }

    fun disconnect() {
        session?.unbind(UNBIND_TIMEOUT)
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
        val sm = SubmitSm()
        sm.sourceAddress = Address(config.sourceTon, config.sourceNpi, from)
        sm.destAddress = Address(config.destTon, config.destNpi, to)
        sm.dataCoding = config.dataCoding
        sm.shortMessage = CharsetUtil.encode(payload, config.charset)

        return session?.submit(sm, SUBMIT_TIMEOUT)
    }

    fun respondUssd(deliverSm: DeliverSm, responseText: String = "OK") {
        val submitSm = SubmitSm()
        submitSm.sourceAddress = deliverSm.destAddress
        submitSm.destAddress = deliverSm.sourceAddress
        submitSm.dataCoding = SmppConstants.DATA_CODING_DEFAULT
        submitSm.shortMessage = CharsetUtil.encode(responseText, "ISO-8859-1")
        if (isPssrIndication(deliverSm)) {
            submitSm.addOptionalParameter(pssrResponse)
        }
        logger.info("[fun respondUssd] Sending SubmitSm...")
        val pduResponse = session?.submit(submitSm, SUBMIT_TIMEOUT)
        logger.info("[fun respondUssd] Received response: [${pduResponse.toString()}]")
        session?.sendResponsePdu(deliverSm.createResponse())
    }

    private fun buildSessionConfiguration(): SmppSessionConfiguration {
        val sc = SmppSessionConfiguration()
        sc.name = name
        sc.type = SmppBindType.TRANSCEIVER
        sc.host = config.host
        sc.port = config.port
        sc.systemId = config.systemId
        sc.password = config.password
        sc.bindTimeout = BIND_TIMEOUT
        return sc
    }

    private fun bind(): SmppSession {
        return client.bind(sessionConfig, sessionHandler)
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
