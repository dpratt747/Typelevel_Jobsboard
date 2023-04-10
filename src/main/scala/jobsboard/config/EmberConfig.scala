package com.github.dpratt747
package jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import com.comcast.ip4s.*
import pureconfig.error.CannotConvert

final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig {
  given ConfigReader[Port] = ConfigReader[Int].emap(port =>
    Port
      .fromInt(port)
      .toRight(CannotConvert(port.toString, Port.getClass.toString, "Invalid port number"))
  )

  given ConfigReader[Host] = ConfigReader[String].emap(hostString =>
    Host
      .fromString(hostString)
      .toRight(CannotConvert(hostString, Host.getClass.toString, "Invalid host"))
  )

}
