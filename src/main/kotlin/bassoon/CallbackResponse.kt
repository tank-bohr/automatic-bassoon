package bassoon

interface CallbackResponse {

    companion object Constants {
        const val DEFAULT_RESPONSE_TEXT = "OK"
    }

    fun responseText(): String = DEFAULT_RESPONSE_TEXT
}
