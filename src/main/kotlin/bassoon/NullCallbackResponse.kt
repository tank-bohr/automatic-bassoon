package bassoon

class NullCallbackResponse: CallbackResponse {
    override fun responseText(): String {
        return DEFAULT_RESPONSE_TEXT
    }
}
