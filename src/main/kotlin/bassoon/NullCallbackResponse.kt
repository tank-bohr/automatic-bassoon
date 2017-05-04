package bassoon

class NullCallbackResponse : CallbackResponse {
    override fun responseText(): String? {
        return null
    }
}
