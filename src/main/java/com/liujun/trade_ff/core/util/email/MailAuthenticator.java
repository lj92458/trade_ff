package com.liujun.trade_ff.core.util.email;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class MailAuthenticator extends Authenticator {
	private String userName;
	private String password;

	public MailAuthenticator(String userName, String password) {
		super();
		this.userName = userName;
		this.password = password;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(userName, password);
	}
}
