package bassoon

class ErrorCallbackResponse : CallbackResponse {
    override fun responseText(): String {
        return "Error occurred"
    }
}