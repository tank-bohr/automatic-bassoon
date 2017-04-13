import basson.Client
import java.lang.Thread.sleep

fun main(args: Array<String>) {
    val client = Client()
    while (true) {
        client.connect()
        sleep(15_000)
    }
}
