package client

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicLong

val log = mu.KotlinLogging.logger {}

@RestController
class ClientStart {
    private var atomicNumber: AtomicLong = AtomicLong(1)
    private val baseUrl = "http://localhost:8080"
    private val webClient = WebClient.builder().baseUrl(baseUrl)
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { configurer ->
                    configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 200)
                }
                .build()
        )
        .build()

    @RequestMapping("/flux-flux")
    fun flux_flux(): Flux<String> {
        val index = atomicNumber.getAndIncrement()
        log.info { "$index start" }
        return webClient
            .get()
            .uri("/flux")
            .retrieve()
            .bodyToFlux(Data::class.java)
            .map {
                log.info { "requestIndex: $index , " + it.data }
                for (i in 1 ..1000000) findLargePrimeFactor()
                return@map it.data.toString()
            }

    }

    fun findLargePrimeFactor(): Long {
        var n = 600851475143
        var i = 2L
        var maxPrime = 2L

        while (i <= n / i) {
            while (n % i == 0L) {
                maxPrime = i
                n /= i
            }
            i++
        }

        if (n > 1) {
            maxPrime = n
        }

        return maxPrime
    }

//
//    @RequestMapping("/flux-mono")
//    fun flux_mono() : Flux<String> {
//        return webClient
//            .get()
//            .uri("/flux")
//            .retrieve()
//            .bodyToMono<List<Data>>()
//            .flatMapMany {
//                Flux.fromIterable(it)
//            }
//            .map {
//                println(it.data.size)
//                String(it.data)
//            }
//    }
//
//    @RequestMapping("/mono-flux")
//    fun mono_flux(): Mono<List<String>> {
//        return webClient
//            .get()
//            .uri("/flux")
//            .retrieve()
//            .bodyToFlux(ByteArray::class.java)
//            .map {
//                println(it.size)
//                String(it)
//            }
//            .collectList()
//    }
//
//    @RequestMapping("/mono-mono")
//    fun mono(): Mono<List<String>> {
//        return webClient
//            .get()
//            .uri("/flux")
//            .retrieve()
//            .bodyToMono<List<Data>>()
//            .flatMapMany {
//                Flux.fromIterable(it)
//            }
//            .map {
//                println(it.data.size)
//                String(it.data)
//            }
//            .collectList()
//    }

}

data class Data(
    val data: Long,
)