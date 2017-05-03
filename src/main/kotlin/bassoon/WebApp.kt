package bassoon

import spark.Request
import spark.Response
import spark.Spark.*

class WebApp(private val registry: ClientsRegistry) {

    fun run() {
        val httpPort = System.getenv("HTTP_PORT")?.toInt() ?: 8080
        port(httpPort)

        // Routes
        get("/") { _, _ -> "OK" }
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
}
