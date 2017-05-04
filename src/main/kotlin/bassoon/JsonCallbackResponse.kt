package bassoon

data class JsonCallbackResponse(val responseText: String?) : CallbackResponse {

    override fun responseText(): String? {
        return responseText?.takeIf(String::isNotBlank)?.trim()
    }
}
