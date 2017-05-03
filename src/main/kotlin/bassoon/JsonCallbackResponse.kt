package bassoon

import bassoon.CallbackResponse.Constants.DEFAULT_RESPONSE_TEXT

data class JsonCallbackResponse(val responseText: String?) : CallbackResponse {

    override fun responseText() = responseText?.takeIf(String::isNotBlank)?.trim() ?: DEFAULT_RESPONSE_TEXT
}
