package bassoon

import com.cloudhopper.smpp.pdu.DeliverSm

class NullCallback : Callback {
    override fun run(pdu: DeliverSm, charset: String): CallbackResponse = NullCallbackResponse()
}
