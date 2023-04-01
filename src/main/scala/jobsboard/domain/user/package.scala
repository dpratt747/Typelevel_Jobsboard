package com.github.dpratt747
package jobsboard.domain

import jobsboard.domain.job.Email.Email

import com.github.dpratt747.jobsboard.domain.job.CompanyName.CompanyName

package object user {
  final case class User(
                         email: Email,
                         hashedPassword: String,
                         firstName: Option[String],
                         lastName: Option[String],
                         company: Option[CompanyName],
                         role: Role
                       )

  enum Role {
    case ADMIN, RECRUITER
  }

  object Role {
    given metaRole: doobie.Meta[Role] = doobie.Meta[String].timap(Role.valueOf)(_.toString)
  }
}
