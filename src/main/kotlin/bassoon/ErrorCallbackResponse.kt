package bassoon

class ErrorCallbackResponse : ICallbackResponse {
    override fun responseText(): String {
        return "Error occurred"
    }
}