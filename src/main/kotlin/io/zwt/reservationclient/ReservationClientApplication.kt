package io.zwt.reservationclient

import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import reactor.core.publisher.Mono
import java.security.Principal

@SpringBootApplication
class ReservationClientApplication {

    @Bean
    fun redisRateLimiter() = RedisRateLimiter(5, 7)

    @Bean
    fun gateway(rlb: RouteLocatorBuilder) = rlb.routes {

        route {
            path("/proxy") and host("*.spring.io")
            filters {
                setPath("/reservations")
                addRequestHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                requestRateLimiter {

                    it.rateLimiter = redisRateLimiter()
                    it.keyResolver = PrincipalNameKeyResolver()
                }
            }
            uri("http://localhost:8080")
        }
    }
}


fun main(args: Array<String>) {
    runApplication<ReservationClientApplication>(*args)
}
