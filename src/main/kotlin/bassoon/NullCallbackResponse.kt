package bassoon

class NullCallbackResponse: ICallbackResponse {
    override fun responseText(): String {
        return DEFAULT_RESPONSE_TEXT
    }
}
