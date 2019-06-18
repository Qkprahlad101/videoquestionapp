package com.drupal.events;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import com.drupal.dao.UserRepo;
import com.drupal.dao.VerificationTokenRepo;
import com.drupal.models.VerificationToken;

@Component
public class RegistrationEmailListener implements ApplicationListener<OnRegistrationSuccessEvent> {

	@Value("${env.ip}")
	private String ip;
	
	@Autowired
	private MailSender mailSender;

	@Autowired
	private VerificationTokenRepo verificationTokenRepo;

	@Autowired
	UserRepo userRepo;

	@Override
	public void onApplicationEvent(OnRegistrationSuccessEvent event) {
		this.confirmRegistration(event);
	}

	private void confirmRegistration(OnRegistrationSuccessEvent event) {
		String tokenId = UUID.randomUUID().toString();
		VerificationToken token = new VerificationToken(event.getUserId(), tokenId);
		verificationTokenRepo.save(token);

		String message = "Click on this link to confirm your registration:\n";
		String trailingMessage = " \nThe link will expire after 24 hours";
		SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(userRepo.findById(event.getUserId()).orElse(null).getEmail());
		email.setSubject("Confirm registration to videoquestion app");
		email.setText(message + "http://"+ip+":8080/confirmtoken?token=" + tokenId + trailingMessage);
		try {
			mailSender.send(email);
		} catch (Exception e) {
			System.out.println("Email sending failed exception ocured");
			System.out.println(e.toString());
			userRepo.deleteById(event.getUserId());
			verificationTokenRepo.delete(verificationTokenRepo.findByToken(tokenId));
			throw new MailException("Can't send verification mail") {
				private static final long serialVersionUID = 2L;
			};
		}
	}
}