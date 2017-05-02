package bassoon

import bassoon.CallbackResponse.Constants.DEFAULT_RESPONSE_TEXT

data class JsonCallbackResponse(val response_text: String?) : CallbackResponse {
    override fun responseText(): String = response_text?.takeIf(String::isNotBlank)?.trim() ?: DEFAULT_RESPONSE_TEXT
}
