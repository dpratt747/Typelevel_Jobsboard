package com.github.dpratt747
package jobsboard.domain

import jobsboard.domain.job.Email.Email
import jobsboard.domain.job.Password.Password

package object auth {
  final case class LoginInfo(
                              email: Email,
                              password: Password
                            )

  final case class NewPasswordInfo(
                                    oldPassword: Password,
                                    newPassword: Password
                                  )
}
