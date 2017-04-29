package bassoon

import com.cloudhopper.smpp.pdu.DeliverSm

class NullCallback : ICallback {
    override fun run(pdu: DeliverSm): ICallbackResponse {
        return NullCallbackResponse()
    }
}
