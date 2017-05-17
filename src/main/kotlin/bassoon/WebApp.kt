package bassoon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import spark.Request
import spark.Response
import spark.Spark.*

class WebApp(private val registry: ClientsRegistry) {

    private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    fun run() {
        val httpPort = System.getenv("HTTP_PORT")?.toInt() ?: 8080
        port(httpPort)

        // Routes
        get("/") { _, _ -> "OK" }
        get("/health", this::health)
        post("/send_sms/:name", this::sendSms)
    }

    private fun sendSms(request: Request, response: Response) {
        val name = request.params("name")
        val client = registry.fetch(name)
        when {
            client !is Client -> halt(404)
            !client.isConnected() -> halt(503)
            else -> {
                val to = request.queryParams("to")
                val text = request.queryParams("text")
                val pdu = client.submit(to = to, payload = text)
                response.run {
                    header("x-result", pdu?.resultMessage)
                    header("x-command-status", pdu?.commandStatus.toString())
                    status(204)
                }
            }
        }
    }

    private fun health(request: Request, response: Response): String {
        val check = registry.healthCheck()
        val ok = check.values.all { it }
        if (!ok) {
            response.status(503)
        }
        response.header("content-type", "application/json")
        return mapper.writeValueAsString(check)
    }
}
