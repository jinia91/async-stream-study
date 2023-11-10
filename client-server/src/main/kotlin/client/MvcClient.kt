package client

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import javax.xml.crypto.Data

@RestController
class MvcClient {

    @RequestMapping("/list-list")
    fun restTemplate(): List<Data>{
        return RestTemplate().getForObject("http://localhost:8080/flux", List::class.java) as List<Data>
    }
}