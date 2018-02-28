package com.desai.autotrade.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.desai.autotrade.helper.NseFutureListHelper;
import com.desai.autotrade.service.AutoTradeService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;


@RestController
public class AutoTradeController {	
	
	@Autowired
	private AutoTradeService eTradeService;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	
	private static final Logger log = Logger.getLogger(AutoTradeService.class.getName());
	
	@RequestMapping(value="/init/{user}")
	public void init(@PathVariable String user){
		
	}

	@PostConstruct
	private void init(){
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));
	}
	
	@RequestMapping(value="/autotrade/v1/{user}")
	public String doLogin(@PathVariable String user, @RequestParam Map<String, String> queryParams) {	   	
		try {
			return eTradeService.doLogin(user, queryParams.get("request_token"));
		} catch (JSONException e) {
			log.warning("JSON Exception: "+e.getMessage());
			return "Something went wrong while logging in. Please try again later" + e.getMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.warning("IOException: "+e.getMessage());
			return "Something went wrong while logging in. Please try again later" + e.getMessage();
		} catch (KiteException e) {
			// TODO Auto-generated catch block
			log.warning("KiteException: "+e.getMessage());
			return "Something went wrong while logging in. Please try again later" + e.getMessage();
		}		
	}
	
	@RequestMapping(value="/checkStatus")
	public String checkStatus(){
		
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Calcutta"));
		//now.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));
		
		
		return "Hello! The connection to eTrade Application is successfull at "+sdf.format(now.getTime());
	}
}
