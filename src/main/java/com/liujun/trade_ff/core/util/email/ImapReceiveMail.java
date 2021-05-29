package com.liujun.trade_ff.core.util.email;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeUtility;
import java.security.Security;
import java.util.Properties;

/**
 * 
 * @author Administrator
 * @See 
 *      http://outofmemory.cn/code-snippet/2408/JavaMail-course-usage-IMAP-protocol
 *      -receive-parse-dianziyoujian
 * @See http://blog.csdn.net/danile2009/article/details/6047481
 * @See http://itindex.net/blog/2012/02/02/1328168422890.html
 */
public class ImapReceiveMail {

	public static void main(String[] args) throws Exception {
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
		// 准备连接服务器的会话信息
		Properties props = new Properties();
		props.setProperty("mail.imap.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.imap.socketFactory.fallback", "false");
		props.setProperty("mail.store.protocol", "imap");
		props.setProperty("mail.imap.host", "imap.qq.com");
		props.setProperty("mail.imap.port", "993");
		props.setProperty("mail.imap.socketFactory.port", "993");

		// 创建Session实例对象
		Session session = Session.getInstance(props);

		IMAPStore store = (IMAPStore) session.getStore("imap");// 郵件服務器

		store.connect("2683873595@qq.com", "liujun924586");
		// 创建IMAP协议的Store对象
		// 获得收件箱
		IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
		// 以读写模式打开收件箱
		folder.open(Folder.READ_WRITE);

		// 获得收件箱的邮件列表
		Message[] messages = folder.getMessages();

		// 打印不同状态的邮件数量
		System.out.println("收件箱中共" + messages.length + "封邮件!");
		System.out.println("收件箱中共" + folder.getUnreadMessageCount() + "封未读邮件!");
		System.out.println("收件箱中共" + folder.getNewMessageCount() + "封新邮件!");
		System.out.println("收件箱中共" + folder.getDeletedMessageCount() + "封已删除邮件!");

		System.out.println("------------------------开始解析邮件----------------------------------");

		// 解析邮件
		for (Message message : messages) {
			IMAPMessage msg = (IMAPMessage) message;
			String subject = MimeUtility.decodeText(msg.getSubject());
			System.out.println("[" + subject + "]未读");

			// 第二个参数如果设置为true,则将修改反馈给服务器。false则不反馈给服务器
			// msg.setFlag(Flag.SEEN, false); //设置已读标志
			System.out.println("发送时间：" + message.getSentDate());
			System.out.println("主题：" + message.getSubject());
			System.out.println("内容：" + message.getContent());
			Flags flags = message.getFlags();
			if (flags.contains(Flag.SEEN)){
				System.out.println("这是一封已读邮件");
			}
			if (flags.contains(Flag.FLAGGED)){
				System.out.println("flagged");
			}
			if (flags.contains(Flag.RECENT)){
				System.out.println("recent");
			}
			if (flags.contains(Flag.USER)){
				System.out.println("user");
			}
			
			 
			 
			System.out.println("========================================================");

		}

		// 关闭资源
		if (folder != null)
			folder.close(true);
		if (store != null)
			store.close();
	}
}
