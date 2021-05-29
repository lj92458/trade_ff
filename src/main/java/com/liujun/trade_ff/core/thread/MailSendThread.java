package com.liujun.trade_ff.core.thread;

import com.liujun.trade_ff.core.util.email.SendMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailSendThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(MailSendThread.class);
	String address;
	String subject;
	String content;

	public boolean succ = false;

	public MailSendThread(String address, String subject, String content) {
		this.address = address;
		this.subject = subject;
		this.content = content;
	}

	public void run() {
		try {
			log.debug("send开始");
			SendMail.send(address, subject, content);
			log.debug("send结束");
			succ = true;
		} catch (Exception e) {
			log.error("邮件发送异常", e);
		}
	}
}
