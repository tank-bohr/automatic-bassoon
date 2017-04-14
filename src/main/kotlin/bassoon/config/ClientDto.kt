package bassoon.config

import com.cloudhopper.smpp.SmppConstants

class ClientDto(
        val name: String,
        val systemId: String,
        val password: String,
        val systemType: String?,
        val serviceType: String?,
        host: String?,
        port: Int?,
        sourceTon: Byte?,
        sourceNpi: Byte?,
        destTon: Byte?,
        destNpi: Byte?,
        dataCoding: Byte?,
        charset: String?,
        allowedConnections: Int?,
        useMessagePayload: Boolean?
) {
    val host: String = host ?: "localhost"
    val port: Int = port ?: 2775
    val sourceTon: Byte = sourceTon ?: SmppConstants.TON_ALPHANUMERIC
    val sourceNpi: Byte = sourceNpi ?: SmppConstants.NPI_UNKNOWN
    val destTon: Byte = destTon ?: SmppConstants.TON_INTERNATIONAL
    val destNpi: Byte = destNpi ?: SmppConstants.NPI_E164
    val dataCoding: Byte = dataCoding ?: SmppConstants.DATA_CODING_UCS2
    val charset: String = charset ?: "UCS-2"
    val useMessagePayload: Boolean = useMessagePayload ?: false
    val allowedConnections: Int = allowedConnections ?: 1
}
