package com.liujun.trade_ff.core.util.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.Security;
import java.util.Date;
import java.util.Properties;

public class SendMail {
	private static final Logger log = LoggerFactory.getLogger(SendMail.class);
	static final String username = "2683873595@qq.com";

	static final String password = "liujun924586";

	/**
	 * 
	 * @param toAddressStr
	 *            字符串,用“;”隔开多个地址
	 * @param subject
	 * @param mailContent
	 * @throws AddressException
	 * @throws MessagingException
	 */
	public static void send(String toAddressStr, String subject, String mailContent) throws Exception {

		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

		// Get a Properties object

		Properties props = System.getProperties();

		props.setProperty("mail.smtp.host", "smtp.qq.com");

		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);

		props.setProperty("mail.smtp.socketFactory.fallback", "false");

		props.setProperty("mail.smtp.port", "465");

		props.setProperty("mail.smtp.socketFactory.port", "465");

		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable","true");
		Session session = Session.getInstance(props, new Authenticator() {

			protected PasswordAuthentication getPasswordAuthentication() {

				return new PasswordAuthentication(username, password);

			}
		});

		// -- Create a new message --

		Message msg = new MimeMessage(session);

		// -- Set the FROM and TO fields --

		msg.setFrom(new InternetAddress("2683873595@qq.com"));

		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddressStr, false));

		msg.setSubject(subject);

		msg.setText(mailContent);

		msg.setSentDate(new Date());

		Transport.send(msg);

		log.info("已发送邮件到" + toAddressStr + ",主题:" + subject);
		log.debug(mailContent);
	}

	public static void main(String[] args) throws Exception {
		SendMail.send("627330472@qq.com", "test3", "test3");
	}
}
