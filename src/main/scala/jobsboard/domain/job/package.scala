package com.github.dpratt747
package jobsboard.domain

import jobsboard.domain.job.*
import jobsboard.domain.job.CompanyName.*
import jobsboard.domain.job.Title.*
import jobsboard.domain.job.Description.*
import jobsboard.domain.job.JobId.*
import jobsboard.domain.job.URL.*
import jobsboard.domain.job.Email.*
import jobsboard.domain.job.Location.*
import jobsboard.domain.job.SalaryLow.*
import jobsboard.domain.job.SalaryHigh.*
import jobsboard.domain.job.Country.*
import jobsboard.domain.job.Tags.*
import jobsboard.domain.job.Image.*
import jobsboard.domain.job.Seniority.*
import jobsboard.domain.job.Other.*
import jobsboard.domain.job.Currency.*

import java.util.Locale.IsoCountryCode

package object job {

  final case class Job(
      id: JobId,
      date: Long,
      ownerEmail: Email,
      jobInfo: JobInfo,
      active: Boolean = false
  )

  final case class JobInfo(
      company: CompanyName,
      title: Title,
      description: Description,
      externalUrl: URL,
      remote: Boolean,
      location: Location,
      currency: Option[Currency],
      salaryLo: Option[SalaryLow],
      salaryHi: Option[SalaryHigh],
      country: Option[Country],
      tags: Option[List[Tags]],
      image: Option[Image],
      seniority: Option[Seniority],
      other: Option[Other]
  )

  object JobInfo {

    def minimal(
        company: CompanyName,
        title: Title,
        description: Description,
        externalUrl: URL,
        remote: Boolean,
        location: Location
    ): JobInfo = JobInfo(
      company = company,
      title = title,
      description = description,
      externalUrl = externalUrl,
      remote = remote,
      location = location,
      salaryLo = None,
      salaryHi = None,
      currency = None,
      country = None,
      tags = None,
      image = None,
      seniority = None,
      other = None
    )
  }

  final case class JobFilter(
      companies: Option[List[CompanyName]] = None,
      locations: Option[List[Location]] = None,
      countries: Option[List[Country]] = None,
      seniorities: Option[List[Seniority]] = None,
      tags: Option[List[Tags]] = None,
      maxSalary: Option[SalaryHigh] = None,
      remote: Option[Boolean] = None
  )
}
