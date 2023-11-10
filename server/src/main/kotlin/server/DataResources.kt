package server

import kotlinx.coroutines.delay
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.*

private val log = mu.KotlinLogging.logger {}

@RestController
class DataResources {

    @RequestMapping("/flux")
    fun getData(): Flux<Data> {
        return Flux.generate<Data> { sink ->
            sink.next(Data())
        }
            .delayElements(Duration.ofMillis(500))
            .take(100)
    }

    @RequestMapping("/mono")
    suspend fun getMonoData(): Data {
        log.info { "request data time" }
        delay(5000)
        return Data()
    }

    @RequestMapping("/mono2")
    suspend fun getMonoData2(): Data {
        log.info { "request data time" }
        delay(10000)
        return Data()
    }
}
data class Data(
// 요청당 10mb
    val data: ByteArray = ByteArray(100 * 1024),
//    val data: Long = Random().nextLong(),
)