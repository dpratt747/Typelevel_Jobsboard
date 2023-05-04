package com.github.dpratt747
package jobsboard.core.program

import jobsboard.config.EmailServiceConfig
import jobsboard.domain.job.Email.Email
import playground.EmailsPlayground.session

import cats.effect.{MonadCancelThrow, Resource}
import cats.implicits.*

import java.util.Properties
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, PasswordAuthentication, Session, Transport}
trait EmailsProgramAlg[F[_]] {
  def sendEmail(to: Email, subject: String, content: String): F[Unit]

  def sendPasswordRecoveryEmail(to: Email, token: String): F[Unit]
}

final case class EmailsProgram[F[_]: MonadCancelThrow] private (
    private val emailServiceConfig: EmailServiceConfig
) extends EmailsProgramAlg[F] {
  override def sendEmail(to: Email, subject: String, content: String): F[Unit] = {
    val messageR: Resource[F, MimeMessage] = for {
      props   <- propsResource
      auth    <- authResource
      session <- createSession(props, auth)
      message <- createMessage(session)(emailServiceConfig.username, to.value, subject, content)
    } yield message

    messageR.use { msg => Transport.send(msg).pure[F] }
  }

  override def sendPasswordRecoveryEmail(to: Email, token: String): F[Unit] = {
    val subject = "Test email"
    val content =
      s"""
      <div style="
      border: 1px solid black;
      padding: 20px;
      font-family: sans-serif;
      color: #888;
      line-height: 1.5em;
      ">
        <h1> Password Recovery </h1>
        <p> Your password recovery token is: $token </p>
        <p>
         Click <a href="${emailServiceConfig.frontEndUrl}">here</a> to return to the application.
        </p>
        <p> 🫣 This is a password recovery email </p>
      </div>
      """

    sendEmail(to, subject, content)
  }

  val propsResource: Resource[F, Properties] = Resource.pure {
    val props = new Properties()
    props.put("mail.smtp.auth", true)
    props.put("mail.smtp.starttls.enable", true)
    props.put("mail.smtp.host", emailServiceConfig.host)
    props.put("mail.smtp.port", emailServiceConfig.port)
    props.put("mail.smtp.ssl.trust", emailServiceConfig.host)
    props
  }

  val authResource: Resource[F, Authenticator] = Resource.pure(
    new Authenticator {
      override def getPasswordAuthentication: PasswordAuthentication =
        new javax.mail.PasswordAuthentication(
          emailServiceConfig.username,
          emailServiceConfig.password
        )
    }
  )

  def createSession(props: Properties, auth: Authenticator): Resource[F, Session] =
    Resource.pure(Session.getInstance(props, auth))

  def createMessage(
      session: Session
  )(from: String, to: String, subject: String, content: String): Resource[F, MimeMessage] =
    Resource.pure {
      val message = new MimeMessage(session)
      message.setFrom(from)
      message.setRecipients(RecipientType.TO, to)
      message.setSubject(subject)
      message.setContent(content, "text/html; charset=utf-8")
      message
    }

}

object EmailsProgram {
  def make[F[_]: MonadCancelThrow](emailServiceConfig: EmailServiceConfig): F[EmailsProgramAlg[F]] =
    EmailsProgram[F](emailServiceConfig).pure[F]
}
