package bassoon

import com.cloudhopper.smpp.pdu.DeliverSm

val DEFAULT_RESPONSE_TEXT = "OK"

interface ICallback {
    fun run(pdu: DeliverSm): ICallbackResponse
}