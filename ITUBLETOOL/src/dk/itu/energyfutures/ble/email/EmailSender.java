package dk.itu.energyfutures.ble.email;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import android.util.Log;

public class EmailSender {
	private final static String TAG = EmailSender.class.getSimpleName();

	private static Session createSessionObject() {
		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.smtp.port", "587");

		return Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("bleot.4d21@gmail.com", "Zaq12wsx12");
			}
		});
	}

	private static Message createMessage(String subject, String messageBody, Session session) throws MessagingException, UnsupportedEncodingException {
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress("bleot.4d21@gmail.com", "ITU 4D21"));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("bleot.4d21@gmail.com", "ITU 4D21"));
		message.setSubject(subject);
		message.setText(messageBody);
		return message;
	}

	public static void sendOffloadingMail(String messageBody, String location) {
		Session session = createSessionObject();
		try {
			Message message = createMessage("OFF-LOADING: " + location, messageBody, session);
			Transport.send(message);
		}
		catch (Exception e) {
			Log.e(TAG, "Error sending e-mail due to exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void sendMail(String messageBody, String subject) {
		Session session = createSessionObject();
		try {
			Message message = createMessage(subject, messageBody, session);
			Transport.send(message);
		}
		catch (Exception e) {
			Log.e(TAG, "Error sending e-mail due to exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
