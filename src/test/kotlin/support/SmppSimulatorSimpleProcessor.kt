package support

import com.cloudhopper.smpp.SmppConstants
import com.cloudhopper.smpp.pdu.*
import com.cloudhopper.smpp.simulator.SmppSimulatorPduProcessor
import com.cloudhopper.smpp.simulator.SmppSimulatorSessionHandler
import io.netty.channel.Channel

class SmppSimulatorSimpleProcessor(val systemId: String, val password: String) : SmppSimulatorPduProcessor {
    override fun process(session: SmppSimulatorSessionHandler?, channel: Channel?, pdu: Pdu?): Boolean {
        val response = when(pdu) {
            is BaseBind<*> -> onBind(pdu)
            is BaseSm<*> -> onSm(pdu)
            else -> genericNack(pdu)
        }
        if (session != null && response != null) {
            session.addPduToWriteOnNextPduReceived(response)
        }
        return true
    }

    private fun onBind(pdu: BaseBind<*>?): Pdu? {
        val response = pdu?.createResponse()
        if(pdu?.systemId != systemId) {
            response?.commandStatus = SmppConstants.STATUS_INVSYSID
        } else if (pdu.password != password) {
            response?.commandStatus = SmppConstants.STATUS_INVPASWD
        }
        return response
    }

    private fun onSm(pdu: BaseSm<*>): Pdu? {
        return pdu.createResponse()
    }

    private fun genericNack(pdu: Pdu?): Pdu? {
        if (pdu is PduRequest<*>) {
            return pdu.createGenericNack(SmppConstants.STATUS_INVCMDID)
        } else {
            return null
        }
    }
}