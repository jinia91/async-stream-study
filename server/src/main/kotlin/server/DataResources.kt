package server

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.*

@RestController
class DataResources {

    @RequestMapping("/flux")
    fun getData(): Flux<Data> {
        return Flux.generate<Data> { sink ->
            sink.next(Data())
        }
            .delayElements(Duration.ofMillis(500))
            .take(10)
    }
}
data class Data(
// 요청당 10mb
//    val data: ByteArray = ByteArray(100 * 1024),
    val data: Long = Random().nextLong(),
)