package com.github.dpratt747
package jobsboard.fixtures

import jobsboard.domain.job.*
import jobsboard.domain.job.CompanyName.CompanyName
import jobsboard.domain.job.Country.Country
import jobsboard.domain.job.Currency.Currency
import jobsboard.domain.job.Description.Description
import jobsboard.domain.job.Image.Image
import jobsboard.domain.job.JobId.JobId
import jobsboard.domain.job.Location.Location
import jobsboard.domain.job.Other.Other
import jobsboard.domain.job.Seniority.Seniority
import jobsboard.domain.job.Tags.Tags
import jobsboard.domain.job.Title.Title
import jobsboard.domain.job.URL.URL
import jobsboard.domain.user.*

import org.scalacheck.Gen

trait UsersGenerators extends JobGenerators {
  val usersGenerators = for {
    email <- emailGen
    hashedPassword <- Gen.alphaStr
    firstName <- Gen.option(Gen.alphaStr)
    lastName <- Gen.option(Gen.alphaStr)
    company <- Gen.option(companyNameGen)
    role <- Gen.oneOf(Role.values)
  } yield User(email, hashedPassword, firstName, lastName, company, role)
}
