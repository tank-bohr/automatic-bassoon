import basson.Client
import java.lang.Thread.sleep

fun main(args: Array<String>) {
    val client = Client()
//    client.connect()
//    client.submit()
//    client.disconnect()
    while (true) {
        client.connect()
        sleep(15_000)
    }
}
