package bassoon

import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.smpp.pdu.DeliverSm

interface Callback {
    fun run(pdu: DeliverSm, charset: String = CharsetUtil.NAME_ISO_8859_1): CallbackResponse
}
