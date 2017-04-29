package bassoon

import spark.Request
import spark.Response
import spark.Spark.*

class WebApp(val registry: ClientsRegistry) {
    fun run() {
        val httpPort = System.getenv("HTTP_PORT")?.toInt() ?: 8080
        port(httpPort)

        // Routes
        get("/") { _, _ -> "OK" }
        post("/send_sms/:name", this::sendSms)
    }

    fun sendSms(request: Request, response: Response) {
        val name = request.params("name")
        val client = registry.fetch(name)
        if (client !is Client) {
            throw RuntimeException("Not a SMPP client")
        } else if (client == null) {
            halt(404)
        } else if (!client.isConnected()) {
            halt(503)
        }
        val to = request.queryParams("to")
        val text = request.queryParams("text")
        val pdu = client?.submit(to = to, payload = text)
        response.header("x-result", pdu?.resultMessage)
        response.header("x-command-status", pdu?.commandStatus.toString())
        response.status(204)
    }
}
