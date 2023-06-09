package com.github.dpratt747
package jobsboard.domain

import jobsboard.domain.job.CompanyName.CompanyName
import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.FirstName.FirstName
import jobsboard.domain.job.LastName.LastName
import jobsboard.domain.job.Password.Password

import com.github.dpratt747.jobsboard.domain.job.Job
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

package object user {
  final case class User(
      email: Email,
      hashedPassword: Password,
      firstName: Option[FirstName],
      lastName: Option[LastName],
      company: Option[CompanyName],
      role: Role
  ) {
    def owns(job: Job): Boolean = job.ownerEmail == email
    def isAdmin: Boolean        = role == Role.ADMIN
    def isRecruiter: Boolean    = role == Role.RECRUITER
  }

  final case class NewUserInfo(
      email: Email,
      password: Password,
      firstName: Option[FirstName],
      lastName: Option[LastName],
      company: Option[CompanyName]
  )

  enum Role {
    case ADMIN, RECRUITER
  }

  object Role {
    given metaRole: doobie.Meta[Role] = doobie.Meta[String].timap(Role.valueOf)(_.toString)

    given roleAuthEnum: SimpleAuthEnum[Role, String] with {
      override val values: AuthGroup[Role] = AuthGroup(Role.ADMIN, Role.RECRUITER)

      override def getRepr(role: Role): String = role.toString
    }
  }
}
