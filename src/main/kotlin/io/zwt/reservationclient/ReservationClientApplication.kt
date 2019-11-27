package io.zwt.reservationclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.context.annotation.Bean
import org.springframework.context.support.beans
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlux
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import java.time.Duration


data class Reservation(val id: Integer, val name: String)
data class GreetingRequest(val name: String)
data class GreetingResponse(val message: String)

@SpringBootApplication
class ReservationClientApplication {

    @Bean
    fun webClient(builder: WebClient.Builder) = builder.build()

    @Bean
    fun rsocketClient(rsocketBuilder: RSocketRequester.Builder) = rsocketBuilder.connectTcp("localhost", 7777).block()

    @Bean
    fun route(webClient: WebClient, rsocketClient: RSocketRequester) = router {

        GET("greetings/{name}") {
            val request = GreetingRequest(it.pathVariable("name"))
            val greetings = rsocketClient.route("greetings").data(request).retrieveFlux<GreetingResponse>()
            ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(greetings)
        }


        GET("/reservations/names") {

            //            val call1: Flux<String>? = null // todo
//            val call2: Flux<String>? = null // todo
//            val call3: Flux<String>? = null // todo
//            val first: Flux<String> = Flux.first(call1, call2, call3)

            val reservations = webClient
                    .get()
                    .uri("http://localhost:8080/reservations").retrieve().bodyToFlux<Reservation>()
                    .map { it.name }
                    .retryBackoff(10, Duration.ofSeconds(1))
//                    .timeout(Duration.ofSeconds(1))
//                    .onErrorResume { Flux.empty() }
            ServerResponse.ok().body(reservations)
        }
    }

    @Bean
    fun authorization(http: ServerHttpSecurity) =
            http
                    .httpBasic(Customizer.withDefaults())
                    .csrf { it.disable() }
                    .authorizeExchange {
                        it
                                .pathMatchers("/proxy").authenticated()
                                .anyExchange().permitAll()
                    }
                    .build()

    @Bean
    fun authentication() = MapReactiveUserDetailsService(
            User.withDefaultPasswordEncoder().username("jlong").password("pw").roles("USER").build(),
            User.withDefaultPasswordEncoder().username("rwinch").password("pw").roles("ADMIN", "USER").build()
    )


    @Bean
    fun gateway(rlb: RouteLocatorBuilder, redisRateLimiter: RedisRateLimiter) = rlb.routes {

        route {
            path("/proxy") and host("*.spring.io")
            filters {
                setPath("/reservations")
                addRequestHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                requestRateLimiter {
                    it.rateLimiter = redisRateLimiter
                }
            }
            uri("http://localhost:8080")
        }
    }
}


fun main(args: Array<String>) {
    runApplication<ReservationClientApplication>(*args) {

        val context = beans {
            //            if (Math.random() > .5)
            bean {
                RedisRateLimiter(5, 7)
            }
        }
        addInitializers(context)
    }
}
