package io.zwt.reservationclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User

@SpringBootApplication
class ReservationClientApplication {

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
                }
            }
            uri("http://localhost:8080")
        }
    }
}


fun main(args: Array<String>) {
    runApplication<ReservationClientApplication>(*args)
}
