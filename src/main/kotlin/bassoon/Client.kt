package bassoon

import bassoon.config.CallbackDto
import bassoon.config.ClientDto
import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.commons.gsm.GsmUtil
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
import java.util.*

// Timeouts in milliseconds
const val BIND_TIMEOUT: Long = 300_000
const val UNBIND_TIMEOUT: Long = 300_000
const val SUBMIT_TIMEOUT: Long = 300_000

class Client(
        private val config: ClientDto,
        private val registry: ClientsRegistry? = null,
        private val callbackConfig: CallbackDto? = null
) : RegistrableClient {
    override val name: String = config.name
    override val allowedConnections: Int = config.allowedConnections
    val charset: String = config.charset

    private val client: SmppClient = DefaultSmppClient()
    private val sessionConfig = buildSessionConfiguration()
    private val sessionHandler: SessionHandler = SessionHandler(client = this, callbackConfig = callbackConfig)
    private var session: SmppSession? = null
    private val logger: Logger = LoggerFactory.getLogger(DefaultSmppClient::class.java)
    private val pssrResponse: Tlv = buildTlv(SmppConstants.TAG_USSD_SERVICE_OP, byteArrayOf(17))
    private val rand: Random = Random()

    override fun connect(): Boolean {
        return try {
            if (!isConnected()) {
                session = bind()
            }
            true
        }
        catch(e: SmppChannelConnectException) { false }
        catch(e: SmppChannelConnectTimeoutException) { false }
        catch(e: SmppBindException) { false }
    }

    override fun isConnected(): Boolean {
        return session != null
    }

    fun disconnect() {
        session?.unbind(UNBIND_TIMEOUT)
        cleanup()
    }

    fun cleanup() {
        session?.close()
        session?.destroy()
        session = null

        registry?.cleanup(name)
    }

    fun submit(
            to: String = "79261234567",
            payload: String = "Hello"
    ): SubmitSmResp? {
        val from = config.from ?: "Automatic Bassoon"
        val sourceAddress = Address(config.sourceTon, config.sourceNpi, from)
        val destAddress = Address(config.destTon, config.destNpi, to)
        val encodedText = CharsetUtil.encode(payload, config.charset)
        return if (config.useMessagePayload) {
            submitOnce(sourceAddress, destAddress, encodedText, useMessagePayload = true)
        } else {
            submitWithUdhi(sourceAddress, destAddress, encodedText)
        }
    }

    fun respondUssd(deliverSm: DeliverSm, responseText: String = "OK") {
        logger.info("[fun respondUssd] Sending SubmitSm...")
        val submitSmResp = submitOnce(
                sourceAddress = deliverSm.destAddress,
                destAddress = deliverSm.sourceAddress,
                shortMessage = CharsetUtil.encode(responseText, config.charset),
                pssr = isPssrIndication(deliverSm)
        )
        logger.info("[fun respondUssd] Received response: [${submitSmResp.toString()}]")
        session?.sendResponsePdu(deliverSm.createResponse())
    }

    private fun submitWithUdhi(sourceAddress: Address, destAddress: Address, encodedText: ByteArray): SubmitSmResp? {
        val referenceNum = referenceNumber()
        val parts = GsmUtil.createConcatenatedBinaryShortMessages(encodedText, referenceNum)
        return if (parts == null) {
            submitOnce(sourceAddress, destAddress, encodedText)
        } else {
            parts.map { submitOnce(sourceAddress, destAddress, it, udhi = true) }.last()
        }
    }

    private fun submitOnce (
            sourceAddress: Address,
            destAddress: Address,
            shortMessage: ByteArray,
            udhi: Boolean = false,
            pssr: Boolean = false,
            useMessagePayload: Boolean = false
    ): SubmitSmResp? {
        val sm = SubmitSm()
        sm.sourceAddress = sourceAddress
        sm.destAddress = destAddress
        sm.dataCoding = config.dataCoding
        if (udhi) {
            sm.registeredDelivery = SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED
            sm.esmClass = SmppConstants.ESM_CLASS_UDHI_MASK
        }
        if (config.serviceType != null) {
            sm.serviceType = config.serviceType
        }
        if (pssr) {
            sm.addOptionalParameter(pssrResponse)
        }
        if (useMessagePayload) {
            sm.addOptionalParameter(messagePayloadTlv(shortMessage))
        } else {
            sm.shortMessage = shortMessage
        }
        return session?.submit(sm, SUBMIT_TIMEOUT)
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

    private fun referenceNumber(): Byte {
        val bytes = byteArrayOf(0)
        rand.nextBytes(bytes)
        return bytes.first()
    }

    private fun messagePayloadTlv(payload: ByteArray): Tlv {
        return buildTlv(SmppConstants.TAG_MESSAGE_PAYLOAD, payload)
    }

    private fun buildTlv(tag: Short, value: ByteArray): Tlv {
        return Tlv(tag, value, SmppConstants.TAG_NAME_MAP[tag])
    }
}
