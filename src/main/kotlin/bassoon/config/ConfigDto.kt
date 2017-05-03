package bassoon.config

data class ConfigDto(
        val clients: List<ClientDto>,
        val callback: CallbackDto?
)
