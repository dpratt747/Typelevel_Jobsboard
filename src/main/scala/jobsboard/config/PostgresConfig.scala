package com.github.dpratt747
package jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class PostgresConfig(numberOfThreads: Int, driver: String, url: String, user: String, password: String) derives ConfigReader
