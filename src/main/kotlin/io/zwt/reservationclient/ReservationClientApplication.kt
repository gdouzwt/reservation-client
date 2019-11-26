package io.zwt.reservationclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders

@SpringBootApplication
class ReservationClientApplication {

	@Bean
	fun gateway(rlb: RouteLocatorBuilder): RouteLocator {
		return rlb
				.routes()
				.route( {
					routeSpect ->
					routeSpect
							.path("/proxy").and().host("*.spring.io")
							.filters( {
								filterSpec ->
										filterSpec
												.setPath("/reservations")
												.addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
							})
							.uri("http://localhost:8080")
				})
				.build()
	}

}

fun main(args: Array<String>) {
	runApplication<ReservationClientApplication>(*args)
}
