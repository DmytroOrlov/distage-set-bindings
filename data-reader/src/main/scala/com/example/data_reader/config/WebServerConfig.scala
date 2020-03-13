package com.example.data_reader.config

import scala.concurrent.duration.FiniteDuration

final case class HttpConfig(bindAddress: String, port: Int)

final case class CircuitBreakerConfig(
    maxFailures: Int,
    resetTimeout: FiniteDuration,
    maxResetTimeout: FiniteDuration,
    exponentialBackoffFactor: Double
)

final case class ApiConfig(requestTimeout: FiniteDuration, circuitBreaker: CircuitBreakerConfig)

final case class WebServerConfig(http: HttpConfig, api: ApiConfig)
