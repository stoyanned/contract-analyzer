package com.softwareag.metering.contracts;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class EmailSender {

    public static void sendEmailWithAttachments(String host, String port, final String userName, final String password,
                                                List<String> toAddresses, String subject, String message, List<File> attachFiles)
            throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Authenticator auth = new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        };

        Session session = Session.getInstance(properties, auth);
        session.setDebug(true);

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(userName));
        InternetAddress[] addressArray = new InternetAddress[toAddresses.size()];
        for (int i = 0; i < toAddresses.size(); i++) {
            addressArray[i] = new InternetAddress(toAddresses.get(i));
        }
        msg.setRecipients(Message.RecipientType.TO, addressArray);
        msg.setSubject(subject);
        msg.setSentDate(new java.util.Date());

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(message, "text/html");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        if (attachFiles != null && !attachFiles.isEmpty()) {
            for (File filePath : attachFiles) {
                MimeBodyPart attachPart = new MimeBodyPart();
                try {
                    attachPart.attachFile(filePath);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                multipart.addBodyPart(attachPart);
            }
        }

        msg.setContent(multipart);

        try {
            Transport.send(msg);
            System.out.println("Email sent successfully.");
        } catch (AuthenticationFailedException e) {
            System.err.println("Authentication failed. Please check the username and password.");
            e.printStackTrace();
        } catch (MessagingException e) {
            System.err.println("Failed to send email.");
            e.printStackTrace();
        }
    }
}