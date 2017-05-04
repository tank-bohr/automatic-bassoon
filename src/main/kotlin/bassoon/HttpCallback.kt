package bassoon

import bassoon.config.CallbackDto
import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.smpp.pdu.DeliverSm
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.*

class HttpCallback(
        private val config: CallbackDto,
        private val smsc: String,
        private val charset: String,
        private val httpClient: Call.Factory = OkHttpClient()
) : Callback {

    private val mapper = ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .registerKotlinModule()

    private val logger = logger<HttpCallback>()

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
                .apply { headers(Headers.of(config.headers)) }

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
        val mobileOriginated = MoData(
                sourceAddress = pdu.sourceAddress.address,
                destAddress = pdu.destAddress.address,
                serviceType = pdu.serviceType,
                shortMessage = CharsetUtil.decode(pdu.shortMessage, charset),
                optionalParameters = pdu.optionalParameters.orEmpty()
                        .filter { it.tagName != null }
                        .map { it.tagName to it.value }
                        .toMap()
        ).also { logger.debug("Send MO $it") }
        val smData = SmData(smsc = smsc, mobileOriginated = mobileOriginated)
        return mapper.writeValueAsString(smData)
    }
}
