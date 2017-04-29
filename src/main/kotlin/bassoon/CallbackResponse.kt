package bassoon

data class CallbackResponse(val foo: String? = null, val bar: String? = null, val response_text: String?) : ICallbackResponse {
    override fun responseText(): String {
        if ((response_text == null) || (response_text.isBlank())) {
            return DEFAULT_RESPONSE_TEXT
        } else {
            return response_text.trim()
        }
    }
}