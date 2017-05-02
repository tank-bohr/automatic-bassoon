package bassoon

import com.cloudhopper.smpp.pdu.DeliverSm

interface Callback {
    fun run(pdu: DeliverSm): CallbackResponse
}
