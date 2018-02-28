package com.desai.autotrade.service;

import org.springframework.stereotype.Service;


import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;

import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

@Service
public class MailJetService {
	
	private static final String MAILJET_API_KEY = "";
	private static final String MAILJET_SECRET_KEY = "";
	private MailjetClient client =
	      new MailjetClient(MAILJET_API_KEY, MAILJET_SECRET_KEY, new ClientOptions("v3.1"));
	
	private static final Logger log = Logger.getLogger(MailJetService.class.getName());
	
	
	public void sendEmail(String recipient, String subject, String body){
		MailjetRequest email = new MailjetRequest(Emailv31.resource)
		        .property(Emailv31.MESSAGES, new JSONArray()
		        .put(new JSONObject()
		          .put(Emailv31.Message.FROM, new JSONObject()
		            .put("Email", "hardik.u.desai@gmail.com")
		            .put("Name", "Desai Auto Trading App"))
		          .put(Emailv31.Message.TO, new JSONArray()
		            .put(new JSONObject()
		              .put("Email", recipient)))
		          .put(Emailv31.Message.SUBJECT, subject)
		          .put(Emailv31.Message.TEXTPART,
		        		  "")
		          .put(Emailv31.Message.HTMLPART,
		        		  body)));

		    try {
		      // trigger the API call
		      MailjetResponse response = client.post(email);
		      // Read the response data and status
		      log.info("Response Status: "+response.getStatus());		      
		      
		      //log.info(response.getData().);
		    } catch (MailjetException e) {
		      log.warning("Mailjet Exception"+ e.getMessage());
		    } catch (MailjetSocketTimeoutException e) {
		    	log.warning("Mailjet socket timed out"+e.getMessage());
		    }
	}
	
}
