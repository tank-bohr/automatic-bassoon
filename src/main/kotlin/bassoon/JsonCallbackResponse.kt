package bassoon

data class JsonCallbackResponse(val response_text: String?) : CallbackResponse {
    override fun responseText(): String {
        if ((response_text == null) || (response_text.isBlank())) {
            return DEFAULT_RESPONSE_TEXT
        } else {
            return response_text.trim()
        }
    }
}