package com.github.dpratt747
package jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class ApplicationConfig(emberConfig: EmberConfig, postgresConfig: PostgresConfig) derives ConfigReader
