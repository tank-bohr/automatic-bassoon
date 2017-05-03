package bassoon

import bassoon.CallbackResponse.Constants.DEFAULT_RESPONSE_TEXT

data class JsonCallbackResponse(private val responseText: String?) : CallbackResponse {

    override fun responseText() = responseText?.takeIf(String::isNotBlank)?.trim() ?: DEFAULT_RESPONSE_TEXT
}
