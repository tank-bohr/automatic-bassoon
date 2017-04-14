package bassoon.config

data class ConfigDto(
        val clients: Array<ClientDto>,
        val callbak: CallbackDto?
)