package bassoon

import bassoon.config.CallbackDto
import bassoon.config.Config
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler
import com.cloudhopper.smpp.pdu.DeliverSm
import com.cloudhopper.smpp.pdu.Pdu
import com.cloudhopper.smpp.pdu.PduRequest
import com.cloudhopper.smpp.pdu.PduResponse
import okhttp3.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread


val JSON: MediaType = MediaType.parse("application/json; charset=utf-8")


class SessionHandler(
        var client: Client,
        val logger: Logger = LoggerFactory.getLogger(DefaultSmppSessionHandler::class.java)
) : DefaultSmppSessionHandler(logger) {

    val httpClient: OkHttpClient = OkHttpClient()

    override fun firePduRequestReceived(pduRequest: PduRequest<*>): PduResponse? {
        return pduRequest.createResponse()
    }

    override fun firePduReceived(pdu: Pdu?): Boolean {
        if (pdu is DeliverSm) {
            logger.info("USSD received...")
            val responseText = if (Config.config.callback != null) {
                callback(pdu, Config.config.callback)
            } else {
                "OK"
            }

            thread(
                    name = "${client.name}-async-ussd-response",
                    block = { client.respondUssd(pdu, responseText) }
            )
            return false
        } else {
            return true
        }
    }

    override fun fireChannelUnexpectedlyClosed() {
        logger.info("Unexpected channel closed. Cleanup...")
        client.cleanup()
    }

    override fun fireUnknownThrowable(t: Throwable) {
        logger.info("WTF?!? UnknownThrowable: [${t::class}]")
    }

    private fun callback(pdu: DeliverSm, cfg: CallbackDto): String {
        val body = RequestBody.create(JSON, pduToJson(pdu))

        val url = HttpUrl.Builder()
                .scheme(cfg.scheme)
                .host(cfg.host)
                .port(cfg.port)
                .addPathSegments(cfg.path)
                .build()

        val requestBuilder = Request.Builder()
                .url(url)
                .post(body)

        for ((name, value) in cfg.headers) {
            requestBuilder.addHeader(name, value)
        }

        val request = requestBuilder.build()
        val response = httpClient.newCall(request).execute()
        logger.info("HTTP response: ${response.code()}")
        return response.body().string()
    }

    private fun pduToJson(pdu: DeliverSm): String {
        return "{}"
    }
}
