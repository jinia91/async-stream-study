package client

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Client

fun main(args: Array<String>) {
//    System.setProperty("reactor.netty.ioSelectCount", "1")
    runApplication<Client>(*args)
}
