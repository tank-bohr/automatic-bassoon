package bassoon

import com.cloudhopper.smpp.pdu.DeliverSm

val DEFAULT_RESPONSE_TEXT = "OK"

interface Callback {
    fun run(pdu: DeliverSm): CallbackResponse
}