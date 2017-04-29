package bassoon

import com.cloudhopper.smpp.pdu.DeliverSm

class NullCallback : Callback {
    override fun run(pdu: DeliverSm): CallbackResponse {
        return NullCallbackResponse()
    }
}
