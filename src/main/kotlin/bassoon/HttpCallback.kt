package bassoon

import bassoon.config.CallbackDto
import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.smpp.pdu.DeliverSm
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HttpCallback(
        private val config: CallbackDto,
        private val smsc: String,
        private val charset: String,
        private val httpClient: Call.Factory = OkHttpClient()
) : Callback {
    private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val JSON: MediaType = MediaType.parse("application/json; charset=utf-8")
    }

    override fun run(pdu: DeliverSm): CallbackResponse {
        val body = RequestBody.create(JSON, pduToJson(pdu))

        val url = HttpUrl.Builder()
                .scheme(config.scheme)
                .host(config.host)
                .port(config.port)
                .addPathSegments(config.path)
                .build()

        val requestBuilder = Request.Builder()
                .url(url)
                .post(body)

        config.headers.forEach {
            (name, value) -> requestBuilder.addHeader(name, value)
        }

        val request = requestBuilder.build()
        val response = httpClient.newCall(request).execute()
                .also { logger.info("HTTP response: ${it.code()}") }

        if (!response.isSuccessful) {
            return ErrorCallbackResponse()
        }

        return response.body().string().trim().takeIf(String::isNotEmpty)
            ?.also { logger.info("responseJson is $it") }
            ?.let { mapper.readValue(it, JsonCallbackResponse::class.java) }
            ?: NullCallbackResponse()
    }

    private fun pduToJson(pdu: DeliverSm): String {
        val optionalParameters: HashMap<String, ByteArray> = hashMapOf()
        pdu.optionalParameters?.forEach {
            tlv -> optionalParameters.put(tlv.tagName, tlv.value)
        }
        val mobileOriginated = MoData(
                source_address = pdu.sourceAddress.address,
                dest_address = pdu.destAddress.address,
                service_type = pdu.serviceType,
                short_message = CharsetUtil.decode(pdu.shortMessage, charset),
                optional_parameters = optionalParameters
        )
        val smData = SmData(smsc = smsc, mobile_originated = mobileOriginated)
        return mapper.writeValueAsString(smData)
    }
}
