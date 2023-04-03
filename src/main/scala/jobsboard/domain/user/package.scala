package com.github.dpratt747
package jobsboard.domain

import jobsboard.domain.job.CompanyName.CompanyName
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.FirstName.FirstName
import jobsboard.domain.job.LastName.LastName
import jobsboard.domain.job.Password.Password

package object user {
  final case class User(
                         email: Email,
                         hashedPassword: Password,
                         firstName: Option[FirstName],
                         lastName: Option[LastName],
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
