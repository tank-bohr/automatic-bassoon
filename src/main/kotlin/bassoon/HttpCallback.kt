package bassoon

import bassoon.config.CallbackDto
import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.smpp.pdu.DeliverSm
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val JSON: MediaType = MediaType.parse("application/json; charset=utf-8")

class HttpCallback(
        val config: CallbackDto,
        val smsc: String,
        val charset: String,
        val httpClient: OkHttpClient = OkHttpClient()
) : Callback {
    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    val logger: Logger = LoggerFactory.getLogger(javaClass)

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

        for ((name, value) in config.headers) {
            requestBuilder.addHeader(name, value)
        }

        val request = requestBuilder.build()
        val response = httpClient.newCall(request).execute()
        logger.info("HTTP response: ${response.code()}")

        if (!response.isSuccessful) {
            return ErrorCallbackResponse()
        }

        val responseJson = response.body().string().trim()
        return if (!responseJson.isEmpty()) {
            logger.info("responseJson is $responseJson")
            mapper.readValue(responseJson, JsonCallbackResponse::class.java)
        } else {
            NullCallbackResponse()
        }
    }

    private fun pduToJson(pdu: DeliverSm): String {
        val optionalParameters: HashMap<String, ByteArray> = hashMapOf()
        if (pdu.optionalParameters != null) {
            for (tlv in pdu.optionalParameters) {
                optionalParameters.put(tlv.tagName, tlv.value)
            }
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
