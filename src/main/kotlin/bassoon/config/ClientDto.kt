package bassoon.config

import com.cloudhopper.smpp.SmppConstants

/*

NOTE: Далее идет матерный коммент, объясняющий почему я сотворил такую порнографию

Единственное назначение этого класса хранить данные из конфига (это YAML-файл). При отсутсвии каких-то полей
ебучий Jackson пихает null-ы вместо того, чтобы использовать значения по умолчанию. И мы падаем при парсинге YAML-а,
потому что инициализируем null-ами не-nullable поля!

*/
class ClientDto(
        val name: String,
        val systemId: String,
        val password: String,
        val systemType: String? = null,
        val serviceType: String? = null,
        val from: String? = null,
        host: String? = null,
        port: Int? = null,
        sourceTon: Byte? = null,
        sourceNpi: Byte? = null,
        destTon: Byte? = null,
        destNpi: Byte? = null,
        dataCoding: Byte? = null,
        charset: String? = null,
        allowedConnections: Int? = null,
        useMessagePayload: Boolean? = null) {
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
