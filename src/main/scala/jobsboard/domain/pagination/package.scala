package com.github.dpratt747
package jobsboard.domain

package object pagination {
  val defaultPageSize = 20L

  final case class Pagination(limit: Long, offset: Long)

  object Pagination {
    def apply(limit: Option[Long], offset: Option[Long]): Pagination =
      Pagination(limit.getOrElse(defaultPageSize), offset.getOrElse(0L))
      
    def default: Pagination = Pagination(defaultPageSize, 0L)
  }

}


