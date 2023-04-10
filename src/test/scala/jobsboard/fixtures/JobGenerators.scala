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

import com.github.dpratt747.jobsboard.domain.job.Email.Email
import org.scalacheck.Gen

trait JobGenerators {

  val jobIdGen: Gen[JobId] = Gen.uuid.map(JobId(_))

  val nonEmptyStringGen: Gen[String] = for {
    n     <- Gen.chooseNum(5, 20)
    chars <- Gen.listOfN(n, Gen.asciiPrintableChar)
  } yield chars.mkString

  val companyNameGen: Gen[CompanyName] = nonEmptyStringGen.map(CompanyName(_))

  val titleGen: Gen[Title] = nonEmptyStringGen.map(Title(_))

  val descriptionGen: Gen[Description] = nonEmptyStringGen.map(Description(_))

  val validUrlGen: Gen[URL] =
    Gen.oneOf("https://www.google.com", "https://www.yahoo.com", "https://www.bing.com").map(URL(_))

  val locationGen: Gen[Location] = nonEmptyStringGen.map(Location(_))

  val currencyGen: Gen[Currency] = Gen
    .oneOf(
      "USD",
      "EUR",
      "GBP",
      "JPY",
      "AUD",
      "CAD",
      "CHF",
      "CNY",
      "SEK",
      "NZD",
      "MXN",
      "SGD",
      "HKD",
      "NOK",
      "KRW",
      "TRY",
      "RUB",
      "INR",
      "BRL",
      "ZAR",
      "TWD"
    )
    .map(Currency(_))

  val countryGen: Gen[Country] = nonEmptyStringGen.map(Country(_))

  val tagsGen: Gen[List[Tags]] = Gen.listOf(nonEmptyStringGen.map(Tags(_)))

  val nonEmptyTagsList: Gen[List[Tags]] = Gen.nonEmptyListOf(nonEmptyStringGen.map(Tags(_)))

  val imageGen: Gen[Image] = nonEmptyStringGen.map(Image(_))

  val seniorityGen: Gen[Seniority] = nonEmptyStringGen.map(Seniority(_))

  val otherGen: Gen[Other] = nonEmptyStringGen.map(Other(_))

  val emailGen: Gen[Email] = Gen.oneOf(Seq(Email("somemail@mail.com"), Email("live@mail.com")))

  val jobInfoGen: Gen[JobInfo] =
    for {
      company     <- companyNameGen
      title       <- titleGen
      description <- descriptionGen
      url         <- validUrlGen
      remote      <- Gen.prob(0.5)
      location    <- locationGen
      currency    <- Gen.option(currencyGen)
      salary      <- Gen.option(Gen.posNum[Int])
      country     <- Gen.option(countryGen)
      tags        <- Gen.option(tagsGen)
      image       <- Gen.option(imageGen)
      seniority   <- Gen.option(seniorityGen)
      other       <- Gen.option(otherGen)
    } yield JobInfo(
      company = company,
      title = title,
      description = description,
      externalUrl = url,
      remote = remote,
      location = location,
      currency = currency,
      salaryLo = salary.map(SalaryLow(_)),
      salaryHi = salary.map(SalaryHigh(_)),
      country = country,
      tags = tags,
      image = image,
      seniority = seniority,
      other = other
    )

  val jobGen: Gen[Job] =
    for {
      jobId   <- jobIdGen
      date    <- Gen.long
      email   <- emailGen
      jobInfo <- jobInfoGen
      active  <- Gen.prob(0.5)
    } yield Job(
      jobId,
      date,
      email,
      jobInfo,
      active
    )
}
