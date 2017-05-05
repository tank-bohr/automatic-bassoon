package bassoon

import bassoon.config.CallbackDto
import bassoon.config.ClientDto
import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.commons.gsm.GsmUtil
import com.cloudhopper.smpp.*
import com.cloudhopper.smpp.impl.DefaultSmppClient
import com.cloudhopper.smpp.pdu.DeliverSm
import com.cloudhopper.smpp.pdu.Pdu
import com.cloudhopper.smpp.pdu.SubmitSm
import com.cloudhopper.smpp.pdu.SubmitSmResp
import com.cloudhopper.smpp.tlv.Tlv
import com.cloudhopper.smpp.type.Address
import com.cloudhopper.smpp.type.SmppBindException
import com.cloudhopper.smpp.type.SmppChannelConnectException
import java.util.*

class Client(
        private val config: ClientDto,
        private val registry: ClientsRegistry? = null,
        callbackConfig: CallbackDto? = null
) : RegistrableClient {

    companion object {
        const val BIND_TIMEOUT: Millis = 300_000
        const val UNBIND_TIMEOUT: Millis = 300_000
        const val SUBMIT_TIMEOUT: Millis = 300_000
    }

    override val name: String = config.name
    override val allowedConnections: Int = config.allowedConnections
    val charset: String = config.charset

    private val logger = logger<Client>()
    private val random = Random()

    private val client: SmppClient = DefaultSmppClient()
    private val sessionConfig = buildSessionConfiguration()
    private val sessionHandler = SessionHandler(client = this, callbackConfig = callbackConfig)
    private val pssrResponse: Tlv = buildTlv(SmppConstants.TAG_USSD_SERVICE_OP, byteArrayOf(17))

    private var session: SmppSession? = null

    override fun connect(): Boolean =
            try {
                if (!isConnected()) {
                    session = bind()
                }
                true
            } catch(e: SmppChannelConnectException) {
                false
            } catch(e: SmppBindException) {
                false
            }


    override fun isConnected(): Boolean {
        return session?.isBound ?: false
    }

    fun disconnect() {
        session?.unbind(UNBIND_TIMEOUT)
        cleanup()
    }

    fun cleanup() {
        session?.run {
            close()
            destroy()
        }
        session = null
        registry?.cleanup(name)
    }

    fun submit(to: String = "79261234567", payload: String = "Hello"): SubmitSmResp? {
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
        submitOnce(
                sourceAddress = deliverSm.destAddress,
                destAddress = deliverSm.sourceAddress,
                shortMessage = CharsetUtil.encode(responseText, config.charset),
                pssr = true
        ).also { logger.info("[fun respondUssd] Received response: [$it]") }
        session?.sendResponsePdu(deliverSm.createResponse())
    }

    private fun submitWithUdhi(sourceAddress: Address, destAddress: Address, encodedText: ByteArray): SubmitSmResp? =
            GsmUtil.createConcatenatedBinaryShortMessages(encodedText, referenceNumber())
                    ?.map { submitOnce(sourceAddress, destAddress, it, udhi = true) }?.last()
                    ?: submitOnce(sourceAddress, destAddress, encodedText)

    private fun submitOnce(
            sourceAddress: Address,
            destAddress: Address,
            shortMessage: ByteArray,
            udhi: Boolean = false,
            pssr: Boolean = false,
            useMessagePayload: Boolean = false
    ): SubmitSmResp? {
        val sm = SubmitSm().also {
            it.sourceAddress = sourceAddress
            it.destAddress = destAddress
            it.dataCoding = config.dataCoding
            it.registeredDelivery = SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_NOT_REQUESTED
            if (udhi) {
                it.registeredDelivery = SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED
                it.esmClass = SmppConstants.ESM_CLASS_UDHI_MASK
            }
            if (config.serviceType != null) {
                it.serviceType = config.serviceType
            }
            if (pssr) {
                it.addOptionalParameter(pssrResponse)
            }
            if (useMessagePayload) {
                it.addOptionalParameter(messagePayloadTlv(shortMessage))
            } else {
                it.shortMessage = shortMessage
            }
        }
        return session?.submit(sm, SUBMIT_TIMEOUT)
    }

    private fun buildSessionConfiguration(): SmppSessionConfiguration =
            SmppSessionConfiguration().also {
                it.name = name
                it.type = SmppBindType.TRANSCEIVER
                it.host = config.host
                it.port = config.port
                it.systemId = config.systemId
                it.password = config.password
                it.bindTimeout = BIND_TIMEOUT
                if (config.systemType != null) {
                    it.systemType = config.systemType
                }
            }

    private fun bind(): SmppSession = client.bind(sessionConfig, sessionHandler)

    private fun referenceNumber(): Byte = byteArrayOf(0).also(random::nextBytes).first()

    private fun messagePayloadTlv(payload: ByteArray): Tlv = buildTlv(SmppConstants.TAG_MESSAGE_PAYLOAD, payload)

    private fun buildTlv(tag: Short, value: ByteArray): Tlv = Tlv(tag, value, SmppConstants.TAG_NAME_MAP[tag])
}
