package com.github.dpratt747
package jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class ApplicationConfig(securityConfig: SecurityConfig, emberConfig: EmberConfig, postgresConfig: PostgresConfig) derives ConfigReader
