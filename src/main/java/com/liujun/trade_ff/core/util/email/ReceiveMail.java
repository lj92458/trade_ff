package com.liujun.trade_ff.core.util.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.Properties;

/**
 * 
 * @author Administrator
 * 
 */
public class ReceiveMail {
	private static final Logger log = LoggerFactory.getLogger(ReceiveMail.class);

	/** 
	 * 获取邮件列表。最新的邮件在数组最后面
	 * @param debug 是否输出调试信息
	 * @return
	 * @throws Exception
	 */
	public static Folder receive(boolean debug) throws Exception {
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
		// Get a Properties object
		Properties props = System.getProperties();
		props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.pop3.socketFactory.fallback", "false");
		props.setProperty("mail.pop3.port", "995");
		props.setProperty("mail.pop3.socketFactory.port", "995");

		// 以下步骤跟一般的JavaMail操作相同
		Session session = Session.getInstance(props, null);
		// 请将红色部分对应替换成你的邮箱帐号和密码
		URLName urln = new URLName("pop3", "pop.qq.com", 995, null, "2683873595@qq.com", "liujun924586");
		Store store = session.getStore(urln);
		Folder inbox = null;
		try {
			store.connect();
			inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);
			FetchProfile profile = new FetchProfile();
			profile.add(FetchProfile.Item.ENVELOPE);
			Message[] messages = inbox.getMessages();
			inbox.fetch(messages, profile);
			if (debug) {
				System.out.println("收件箱的邮件数：" + messages.length);
				System.out.println("邮件数量" + inbox.getMessageCount() + ",新邮件数量" + inbox.getNewMessageCount() + ",未读邮件数量" + inbox.getUnreadMessageCount());
				for (int i = 0; i < messages.length; i++) {
					// 邮件发送者
					String from = decodeText(messages[i].getFrom()[0].toString());
					System.out.println("from:"+from);
					InternetAddress ia = new InternetAddress(from);
					System.out.println("FROM:" + ia.getPersonal() + '(' + ia.getAddress() + ')');
					// 邮件标题
					System.out.println("TITLE:" + messages[i].getSubject());

				}
			}// end if
			return inbox;
		} finally {
			/*
			try {
				inbox.close(false);
			} catch (Exception e) {
			}
			
			try {
				store.close();
			} catch (Exception e) {
			}
			*/
		}
	}

	public static String decodeText(String text) throws UnsupportedEncodingException {
		if (text == null)
			return null;
		if (text.startsWith("=?GB") || text.startsWith("=?gb"))
			text = MimeUtility.decodeText(text);
		else
			text = new String(text.getBytes("ISO8859_1"));
		return text;
	}

	public static void main(String[] args) throws Exception {
		Folder inbox = receive(true);
		inbox.close(false);
		inbox.getStore().close();
	}
}