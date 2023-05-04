package com.github.dpratt747
package playground

import java.util.Properties
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, PasswordAuthentication, Session, Transport}

object EmailsPlayground extends App {

  // create Ethereal account

  // configs
  // user, pass, host, port
  //  Host smtp.ethereal.email
  //  Port 587
  //  Security STARTTLS
  //  Username audra.collins@ethereal.email
  //  Password DrxXJJ69YYfWQdFaG1

  private val host     = "smtp.ethereal.email"
  private val port     = 587
  private val user     = "audra.collins@ethereal.email"
  private val password = "DrxXJJ69YYfWQdFaG1"

  // properties file
  private val prop = new Properties()
  prop.put("mail.smtp.auth", true)
  prop.put("mail.smtp.starttls.enable", true)
  prop.put("mail.smtp.host", host)
  prop.put("mail.smtp.port", port)
  prop.put("mail.smtp.ssl.trust", host)

  // authentication
  private val auth = new Authenticator {
    override def getPasswordAuthentication: PasswordAuthentication =
      new javax.mail.PasswordAuthentication(user, password)
  }

  // session
  private val session = Session.getInstance(prop, auth)

  // email
  private val token   = "123456"
  private val subject = "Test email"
  private val content =
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
       Click <a href="http://localhost:8080/reset-password?token=$token">here</a> to return to the application.
      </p>
      <p> 🫣 This is a test email </p>
    </div>
    """

  // message - Mime message
  private val message = new MimeMessage(session)
  message.setFrom("dpratt747@yahoo.com")
  message.setRecipients(RecipientType.TO, "theUser@gmail.com")
  message.setSubject(subject)
  message.setContent(content, "text/html; charset=utf-8")

  // send email
  Transport.send(message)
}
