package bassoon

import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias Millis = Long

inline fun <reified T : Any> logger(): Logger = LoggerFactory.getLogger(T::class.java)