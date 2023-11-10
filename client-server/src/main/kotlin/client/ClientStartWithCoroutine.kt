package client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.selects.select
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
    fun flux_flux(): Flow<String> {
        return webClient
            .get()
            .uri("/flux")
            .retrieve()
            .bodyToFlux(Data::class.java)
            .asFlow()
            .map { it.data.toString() }
    }

    /**
     * mono 구독은 결국 deferred.await할때 전파되어 구독되므로 호출을
     * concat하는 aData.await() 때 호출한다.
     * 여기서 문제는 해당 호출은 코루틴을 멈추게 하기때문에 결국 a 호출 후 b를 호출하는 순서로 동작!
     * 매우 잘못된 사용법!!!
     * @return
     */
    @RequestMapping("/flux-asnyc-mono")
    suspend fun flux_asnyc_mono(): Flow<String> = coroutineScope {
        val aData: Deferred<Mono<Data>> = async {
            log.info { "callAsync" }
            // 5초 걸리며 MONO로 반환
            callMono()
        }

        val bData = async {
            log.info { "callAsync" }
            callMono()
        }

        return@coroutineScope Flux.concat(bData.await(), aData.await())
            .asFlow()
            .map {
                log.info { it.data.size }
                return@map it.data.toString()
            }
    }

    // 이 패턴이 call을 하는건 맞는패턴
    // 다만 이방식은 비동기 논블로킹으로 동작할뿐이고 flow로 반환하지만 애플리케이션 로직을 스트림으로서 처리한게 아니므로 실제론 중간에 await시점에 toCollect하는셈
    @RequestMapping("/flux-asnyc-await")
    suspend fun flux_asnyc_await(): Flow<String> = coroutineScope {
        val aData: Deferred<Data> = async {
            log.info { "callAsync" }
            callMono().awaitSingle()
        }

        val bData: Deferred<Data> = async {
            log.info { "callAsync" }
            delay(3000)
            callMono().awaitSingle()
        }

        return@coroutineScope flowOf(
            bData.await(),
            aData.await()
        )
            .map {
                log.info { it.data.size }
                return@map it.data.toString()
            }
    }

    // 이 패턴은 데이터를 스트림으로 바라보는 패러다임
    // 비동기 논블로킹 데이터 스트림처리이므로 리액티브 프로그래밍으로 응답처리
    @RequestMapping("/flux-asnyc-await2")
    suspend fun flux_asnyc_await2(): Flow<String> {
        log.info { "callAsync A" }
        val aData = callMono().asFlow()

        log.info { "callAsync B" }
        val bData = callMono2().asFlow()

        return merge(aData, bData)
            .map {
                log.info { it.data.size }
                return@map it.data.toString()
            }
    }

    @RequestMapping("/flux-monos")
    fun monos(): Flux<String> {
        val aMono = callMono()
        val bMono = callMono2()

        return Flux.merge(aMono, bMono)
            .map {
                log.info { it.data.size }
                return@map it.data.toString()
            }
    }


    private fun callMono(): Mono<Data> {
        log.info { "callMono" }
        return webClient
            .get()
            .uri("/mono")
            .retrieve()
            .bodyToMono<Data>()
    }

    private fun callMono2(): Mono<Data> {
        log.info { "callMono" }
        return webClient
            .get()
            .uri("/mono2")
            .retrieve()
            .bodyToMono<Data>()
    }
}

data class Data(
    val data: ByteArray,
)