package com.desai.autotrade.service;

import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.multipart.FormDataMultiPart;

@Component
public class MailGunService {

	private static final String MAILGUN_DOMAIN_NAME = "";
	private static final String MAILGUN_API_KEY = "";
	
	private static final Logger log = Logger.getLogger(MailGunService.class.getName());
	
	public void  sendEmail(String recipient, String subject, String body){
		Client client = Client.create();
		
		client.addFilter(new HTTPBasicAuthFilter("api", MAILGUN_API_KEY));
		WebResource webResource = client.resource("https://api.mailgun.net/v3/" + MAILGUN_DOMAIN_NAME
			      + "/messages");
		FormDataMultiPart formData = new FormDataMultiPart();
		
		formData.field("from", "Desai AutoTrading App <mailgun@" + MAILGUN_DOMAIN_NAME + ">");
		formData.field("to", recipient);
		formData.field("subject", subject);
		formData.field("html", "<html><strong>"+body+"</strong></html>");
		
		log.info("Sending Email");
		
		ClientResponse response =  webResource.type(MediaType.MULTIPART_FORM_DATA_TYPE)
			      .post(ClientResponse.class, formData);
		
		log.info("");
	}
}
