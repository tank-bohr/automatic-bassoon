package bassoon

class ErrorCallbackResponse : CallbackResponse {
    override fun responseText(): String = "Error occurred"
}
