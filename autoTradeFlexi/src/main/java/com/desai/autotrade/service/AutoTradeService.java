package com.desai.autotrade.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.desai.autotrade.helper.NseFutureListHelper;
import com.desai.autotrade.model.PreviousHLPrize;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Margin;
import com.zerodhatech.models.OHLCQuote;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import com.zerodhatech.models.Trade;
import com.zerodhatech.models.TriggerRange;
import com.zerodhatech.models.User;



@Service
@EnableScheduling
public class AutoTradeService {
	
	private Map<String, String> userMapToUserId = new HashMap<String, String>();
	private Map<String, String> userMapApiKey = new HashMap<String, String>();
	private Map<String, String> userMapApiSecret = new HashMap<String, String>();
	
	//private static List<String> listofNseFutureCompanies;	
	
	private Map<String, OHLCQuote> mapOfOHLCQuote;
	
	private Map<String, PreviousHLPrize> mapOfHLQuote = new HashMap<String, PreviousHLPrize>();
	
	private String[] instruments;
	
	private String todayDecession;
	
	private String todayInstrument;		
	
	private double todaysInstrumentOpen;	
	
	private Map<String,KiteConnect> mapOfConnections = new HashMap<String,KiteConnect>();
	
	Map<String, String> mapOfUserToOrderId = new HashMap<String,String>();
	
	private Map<String,KiteConnect> mapOfOrderToConnections = new ConcurrentHashMap<String,KiteConnect>();
	
	private Map<String, Integer> mapOfUserToLeverage = new HashMap<String, Integer>();
	
	private static final Logger log = Logger.getLogger(AutoTradeService.class.getName());
	
	private SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	
	@Autowired
	private MailJetService emailService;
	
	@PostConstruct
	private void init(){
		
		
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));
	}
	
	
	
	public String doLogin(String user, String requestToken) throws JSONException, KiteException, IOException{
		
		if (mapOfConnections.get(user)!=null){
			/*mapOfConnections.get(user).logout();
			mapOfConnections.remove(user);*/
			return "Dear "+user+", You are already logged in for today. Please try again tommorow at 8:50 AM.";					
		}
		
		KiteConnect kiteConnect = new KiteConnect(userMapApiKey.get(user.toUpperCase()));
		
		kiteConnect.setUserId(userMapToUserId.get(user.toUpperCase()));
		
		kiteConnect.setSessionExpiryHook(new SessionExpiryHook() {
			
			@Override
			public void sessionExpired() {
				// TODO Auto-generated method stub
				log.info("Session Expired");
				
			}
		});
		 
		User userInstance = kiteConnect.generateSession(requestToken, userMapApiSecret.get(user.toUpperCase()));
		
		kiteConnect.setAccessToken(userInstance.accessToken);
		kiteConnect.setPublicToken(userInstance.publicToken);			
		
		
		mapOfConnections.put(user, kiteConnect);
		log.info("Connection Added for : "+user);
		log.info("Size of Connections Map: "+mapOfConnections.size());
		
		return "Dear "+user+", Your Trading Request has been accepted by Auto Trading App at "+sdf.format(Calendar.getInstance(TimeZone.getTimeZone("Asia/Calcutta")).getTime())+"."+"Thank You";
	}
	
	//Clear Connections
	@Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Calcutta")
	private void clearListOfCollections(){
		if (null!=mapOfConnections){
			Collection<KiteConnect> coll = mapOfConnections.values();
			
			for (KiteConnect k: coll){
				if (k!=null){
					try {
						k.logout();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						log.info("IOException :"+e.getMessage());
					} catch (KiteException e) {
						// TODO Auto-generated catch block
						log.info("KiteException :"+e.getMessage());
					}
				}
			}
			
			mapOfConnections.clear();
			log.info("Size of Connections Map: "+mapOfConnections.size());
			try{
				emailService.sendEmail("hardik.u.desai@gmail.com", "Clearing Cache for Desai Auto Trade Application", "Cache has been cleared at: "+sdf.format(Calendar.getInstance(TimeZone.getTimeZone("Asia/Calcutta")).getTime())+"</h3>");
			} catch(Exception e1){
				log.info("Exception while sending email : "+e1.getMessage());
			}
		}	
		
		if (null!=mapOfHLQuote)
			mapOfHLQuote.clear();
		
		if (null!=mapOfOHLCQuote)
			mapOfOHLCQuote.clear();
		
		if (null!=mapOfOrderToConnections)
			mapOfOrderToConnections.clear();
		
		List<String> listofNseFutureCompanies = NseFutureListHelper.getListOfNseFutureCompanies();
		
		instruments = new String[listofNseFutureCompanies.size()];
		instruments = listofNseFutureCompanies.toArray(instruments);
		
		log.info("Instrument Size: "+instruments.length);		
	}
	
	// Get Previous Day High And Low.
	@Scheduled(cron = "0 55 8 * * ?" , zone = "Asia/Calcutta")
	private void getPreviousDayHighAndLow(){
		if (mapOfConnections.isEmpty())
			return;
		
		
		KiteConnect kiteConnect = mapOfConnections.get(mapOfConnections.keySet().iterator().next());
				
		//mapOfHLQuote = new HashMap<String, PreviousHLPrize>();
		try {
			mapOfOHLCQuote = kiteConnect.getOHLC(instruments);
			
			Set<Entry<String,OHLCQuote>> entrySet = mapOfOHLCQuote.entrySet();
			for (Entry<String,OHLCQuote> e: entrySet){
				PreviousHLPrize ohPrize = new PreviousHLPrize();
				ohPrize.setKey(e.getKey());
				ohPrize.setYesterdayHigh(e.getValue().ohlc.high);
				ohPrize.setYesterdayLow(e.getValue().ohlc.low);
				
				mapOfHLQuote.put(e.getKey(), ohPrize);
			}
		} catch (IOException e1) {			
			e1.printStackTrace();
		} catch (KiteException e1) {			
			e1.printStackTrace();
		}
		
		log.info("Size of Yesterday's High and Low:"+mapOfHLQuote.size());
		
	}
	
	// Calculate Today's Decession.
	@Scheduled(cron = "0 9 9 * * ?", zone = "Asia/Calcutta")
	private void calculateTodaysDecession(){
		if (mapOfConnections.isEmpty())
			return;
		
		KiteConnect kiteConnect = mapOfConnections.get(mapOfConnections.keySet().iterator().next());
		Map<String, OHLCQuote> listOfSellInstruments = new HashMap<String,OHLCQuote>();
		//Map<String, OHLCQuote> listOfBuyInstruments = new HashMap<String, OHLCQuote>();
		double maxOfSell = 0.0;
		//double maxOfBuy = 0.0;	
		Entry<String,OHLCQuote> maxSellInstrument = null;
		//Entry<String,OHLCQuote> maxBuyInstrument = null;
		
		try {
			mapOfOHLCQuote = kiteConnect.getOHLC(instruments);
			
			Set<Entry<String,OHLCQuote>> entrySet = mapOfOHLCQuote.entrySet();
			for (Entry<String,OHLCQuote> e: entrySet){
				OHLCQuote ohlc = e.getValue();
				
				double yesterdayHigh = mapOfHLQuote.get(e.getKey()).getYesterdayHigh();
				
				double yesterdayLow = mapOfHLQuote.get(e.getKey()).getYesterdayLow();					
				
				double diffOfYestHighAndLow = Math.round(((yesterdayHigh - yesterdayLow)* 0.16)*100.0)/100.0;
				diffOfYestHighAndLow = Math.round(diffOfYestHighAndLow*100.0)/100.0;
				
				double upperBand = yesterdayHigh -  diffOfYestHighAndLow;
				upperBand = Math.round(upperBand*100.0)/100.0;
				mapOfHLQuote.get(e.getKey()).setUpperBand(upperBand);
				
				double lowerBand = yesterdayLow + diffOfYestHighAndLow;
				lowerBand = Math.round(lowerBand*100.0)/100.0;
				mapOfHLQuote.get(e.getKey()).setLowerBand(lowerBand);
				
				if (ohlc.lastPrice > upperBand){
					/*log.info("Decession for :"+e.getKey()+" is Sell");
					log.info("Last Price for :"+e.getKey()+" is :"+ohlc.lastPrice);*/
					listOfSellInstruments.put(e.getKey(), ohlc);
				}
				/*else if (ohlc.lastPrice < lowerBand){
					log.info("Decession for :"+e.getKey()+" is Buy");
					log.info("Last Price for :"+e.getKey()+" is :"+ohlc.lastPrice);
					listOfBuyInstruments.put(e.getKey(), ohlc);
				}
				else{
					//log.info("Decession for :"+e.getKey()+" is Wait");
				}*/
				//log.info("Symbol: "+e.getKey() + " - Open: "+ohlc.ohlc.open + " - Close: "+ohlc.ohlc.close+" - High: "+ohlc.ohlc.high+" - Low: "+ohlc.ohlc.low+ " - Last Price: "+ohlc.lastPrice);			
			}
			
			Set<Entry<String,OHLCQuote>> entrySetForSellInstruments = listOfSellInstruments.entrySet();
			//Set<Entry<String,OHLCQuote>> entrySetForBuyInstruments = listOfBuyInstruments.entrySet();
			
			for (Entry<String,OHLCQuote> e: entrySetForSellInstruments){
				OHLCQuote ohlc = e.getValue();
				
				double upperBand = mapOfHLQuote.get(e.getKey()).getUpperBand();
				
				double diff = Math.abs(((ohlc.lastPrice - upperBand)*100)/ohlc.lastPrice);
				diff = Math.round(diff*100.0)/100.0;
				if (diff > maxOfSell){
					maxOfSell = diff;
					maxSellInstrument = e;
				}					
			}				
			
			/*for (Entry<String,OHLCQuote> e: entrySetForBuyInstruments){
				OHLCQuote ohlc = e.getValue();
				
				double lowerBand = mapOfHLQuote.get(e.getKey()).getLowerBand();
				
				double diff = Math.abs(((lowerBand - ohlc.lastPrice)*100)/ohlc.lastPrice);
				diff = Math.round(diff*100.0)/100.0;
				if (diff > maxOfBuy){
					maxOfBuy = diff;
					maxBuyInstrument = e;
				}
			}*/
			
			
			//if (maxOfSell > maxOfBuy){
				todayDecession = Constants.TRANSACTION_TYPE_SELL;
				todayInstrument = maxSellInstrument.getKey();
				todaysInstrumentOpen = maxSellInstrument.getValue().lastPrice;
			/*}
			else if (maxOfBuy > maxOfSell){
				todayDecession = Constants.TRANSACTION_TYPE_BUY;
				todayInstrument = maxBuyInstrument.getKey();
				todaysInstrumentOpen = maxBuyInstrument.getValue().lastPrice;
			}*/
			log.info("Today's Decession: "+todayDecession);
			log.info("Today's Instrument: "+todayInstrument);
			log.info("Today's Instrument Last Price before Open: "+todaysInstrumentOpen);
			log.info("Max Sell  Lower Band: "+mapOfHLQuote.get(maxSellInstrument.getKey()).getLowerBand());
			log.info("Max Sell  Upper Band: "+mapOfHLQuote.get(maxSellInstrument.getKey()).getUpperBand());
			
			
			sendEmailsOfDecession();
			//log.info("Received OHLC Quotes: "+mapOfOHLCQuote.size());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.info("IO Exception: "+e1);
		} catch (KiteException e1) {
			// TODO Auto-generated catch block
			log.info("Kite Exception: "+e1);
		} catch (Exception e1){
			log.info("Exception :" +e1.getMessage());
		}
	}
	
	// Place Today's Order
	@Scheduled(cron = "2 15 9 * * ?", zone = "Asia/Calcutta")
	private void postOrder(){
		if (mapOfConnections.isEmpty())
			return;
		
		String [] instruments = {todayInstrument};
		double lastTradedPrice = 0.0;
		double stopLoss = 0.0;
		
		double target = 0.0;
			
		KiteConnect kiteConnect = mapOfConnections.get(mapOfConnections.keySet().iterator().next());
		
		try{
			lastTradedPrice = kiteConnect.getLTP(instruments).get(todayInstrument).lastPrice;
			
			stopLoss = calculateStopLoss(lastTradedPrice);
			
			target = calculateTarget(lastTradedPrice);
			
			log.info("Last Traded Price before rounding off:"+lastTradedPrice);
			/*if (todayDecession.equals(Constants.TRANSACTION_TYPE_BUY)){
				lastTradedPrice = Math.round((lastTradedPrice*1.02)*2)/2.0;
			}else if (todayDecession.equals(Constants.TRANSACTION_TYPE_SELL)){*/
			lastTradedPrice = ((lastTradedPrice*0.98)*2)/2.0;
			lastTradedPrice = Math.round(lastTradedPrice*100.0)/100.0;
			//}
			
			log.info("Last Traded Price after rounding off:"+lastTradedPrice);
		} catch (JSONException e1) {
			log.info("JSONException while getting Trigger Range: "+e1);
			return;
		} catch (IOException e) {
			log.info("IO Exception while getting Trigger Range: "+e);
			return;
		} catch (KiteException e) {
			log.info("Kite Exception while getting Trigger Range: "+e);
			return;
		}	
		
		
		
		Collection<KiteConnect> collectionOfKiteConnect = mapOfConnections.values();
		for (KiteConnect kc : collectionOfKiteConnect){
		 
			int cash = getMargin(kc);
			
			int userLeverage = 0;
			
			
			try {
				userLeverage = mapOfUserToLeverage.get(kc.getProfile().userShortname);
				log.info("Leverage for user "+kc.getProfile().userShortname+" is "+userLeverage);
			} catch (IOException e2) {
				log.info("Unable to get User Leverage due to IOException: "+e2.getMessage());
				emailService.sendEmail("hardik.u.desai@gmail.com", "Unable to calculate leverage.", "Unable to calculate leverage due to IOException for user");
				return;
			} catch (KiteException e2) {
				log.info("Unable to get User Leverage due to KiteException: "+e2.getMessage());
				emailService.sendEmail("hardik.u.desai@gmail.com", "Unable to calculate leverage.", "Unable to calculate leverage due to KiteException for user");
				return;
			}
			
			
			int quantity = (int)Math.round((cash * userLeverage)/todaysInstrumentOpen);				
			
			OrderParams orderParams = new OrderParams();
			orderParams.quantity = (quantity-1);
			orderParams.orderType = Constants.ORDER_TYPE_LIMIT;
			orderParams.price = lastTradedPrice;
			orderParams.transactionType = todayDecession;
			orderParams.tradingsymbol = todayInstrument.split(":")[1];
			orderParams.stoploss = stopLoss;
			
			orderParams.exchange = Constants.EXCHANGE_NSE;
	        orderParams.validity = Constants.VALIDITY_DAY;
	        //orderParams.triggerPrice = triggerRange.percent; 
	        orderParams.squareoff = target;
	        
	        orderParams.product = Constants.PRODUCT_MIS;
	        
	        try{
	        
		        log.info("Today's Trading Details for User:"+kc.getProfile().userShortname);
		        log.info("*************************************************************");
		        log.info("Order Quantity: "+orderParams.quantity+" Type: "+orderParams.orderType+" Price: "+orderParams.price
		        		+" TransactionType: "+orderParams.transactionType+" Trading Symbol: "+orderParams.tradingsymbol+" Stop Loss:"+orderParams.stoploss
		        		+" Validity: "+orderParams.validity+" Square Off: "+orderParams.squareoff+" Product: "+orderParams.product);
		        
		        Order order = kc.placeOrder(orderParams, Constants.VARIETY_BO);
		        
		        log.info("Order Id:"+order.orderId);
		        
		        log.info("*************************************************************");
		        //mapOfUserToOrderId.put(kc.getProfile().userShortname,order10.orderId);		        
		        
		        mapOfOrderToConnections.put(order.orderId, kc);
	        } catch (JSONException e1) {
				log.info("Unable to place Order due to JSONException: "+e1.getMessage());					
			} catch (IOException e) {
				log.info("Unable to place Order due to JSONException: "+e.getMessage());					
			} catch (KiteException e) {
				log.info("Unable to place Order due to JSONException: "+e.message);					
			}   
	        
	    }		
	}
	
	@Scheduled(cron = "0 17 9 * * ?", zone = "Asia/Calcutta")
	private void sendEmailsOfTrade() throws JSONException, IOException, KiteException{
		
		if (mapOfOrderToConnections.isEmpty()){
			return;
		}	
		
		Set<Entry<String,KiteConnect>> entrySet = mapOfOrderToConnections.entrySet();
		
		for (Entry<String,KiteConnect> en: entrySet){
		
			String order = en.getKey();
			KiteConnect kc = en.getValue();
			List<Trade> trades = kc.getOrderTrades(order);
			
			String subject = "Today's Trading Details";
			String body = "";
			for (Trade t: trades){
				body = body.concat("Trading Timestamp: "+sdf.format(t.exchangeTimestamp));
				body = body.concat("<br/>");
				body = body.concat(" Symbol: "+t.tradingSymbol);
				body = body.concat("<br/>");
				body = body.concat(" Quantity: "+t.quantity);
				body = body.concat("<br/>");
				body = body.concat(" Transaction Type: "+t.transactionType);
				body = body.concat("<br/>");
				body = body.concat(" Trade Id: "+t.tradeId);
				body = body.concat("<br/> Thank You.<br/>Hardik Desai.");
			}
			
			if (!body.equals("")){
				log.info("Sending Trading Information Email to "+kc.getProfile().email);
				emailService.sendEmail(kc.getProfile().email, subject, body);
			}
			else{
				log.info("No Trade information to be sent");
			}
		}	
	}
	
	@Scheduled(cron = "0 15 15 * * ?", zone = "Asia/Calcutta")
	private void doSquareOff(){
		
		if (mapOfOrderToConnections == null || mapOfOrderToConnections.isEmpty())
			return;
		
		log.info("Starting Auto Square Off");
		Set<Entry<String,KiteConnect>> entrySet = mapOfOrderToConnections.entrySet();
		String subject = "Square Off Completed for Today's Trading";
		
		for (Entry<String,KiteConnect> e: entrySet){
			String order = e.getKey();
			KiteConnect kc = e.getValue();
			
			try {
				log.info("Proceeding with Auto Square off for user "+kc.getProfile().userName);
				kc.cancelOrder(order, Constants.VARIETY_BO);
				
				//Send Email
				
				//String body = "<h3>Dear "+kc.getProfile().userName+",</h3><br/> Today's order has been square off. The order id is "+order+". <br/> Thank You.<br/>Hardik Desai.";
				//emailService.sendEmail(kc.getProfile().email, subject, body);
								
			} catch (JSONException e1) {
				log.info("JSONException: "+e1);
			} catch (IOException e1) {
				log.info("IO Exception: "+e1);
			} catch (KiteException e1) {
				log.info("KiteException: "+e1);
			} catch(Exception e1){
				log.info("Exception: "+e1);
			}
			
			mapOfOrderToConnections.remove(order);
		}
	}
	
		
	public double calculateStopLoss(double lastTradedPrice){
		//double stopLoss = 0.0;
		
		//if (todayDecession.equals(Constants.TRANSACTION_TYPE_SELL)){
		double	stopLoss = Math.abs((lastTradedPrice * 1.02)-lastTradedPrice);
		/*}
		else if (todayDecession.equals(Constants.TRANSACTION_TYPE_BUY)){
			stopLoss = Math.abs((lastTradedPrice * 0.98)-lastTradedPrice);
		}*/
		
		return Math.round(stopLoss * 2) / 2.0;
	}
	
	public double calculateTarget(double lastTradedPrice){
		//double target = 0.0;
		
		//if (todayDecession.equals(Constants.TRANSACTION_TYPE_SELL)){
			double	target = Math.abs((lastTradedPrice * 0.90)-lastTradedPrice);
		/*}
		else if (todayDecession.equals(Constants.TRANSACTION_TYPE_BUY)){
			target = Math.abs((lastTradedPrice * 1.10)-lastTradedPrice);
		}*/
		
		return Math.round(target * 2) / 2.0;			
	}
	
	public int getMargin(KiteConnect kiteConnect){
		String cash = "";
		try {
			Margin margins = kiteConnect.getMargins("equity");
			
			cash = margins.available.cash;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return (int)Math.round(Double.parseDouble(cash));
	}
	
	
	private void sendEmailsOfDecession() throws IOException, KiteException{
		Collection<KiteConnect> collectionOfKiteConnect = mapOfConnections.values();
		String subject = "Today's Decession for Auto Trading";
		for (KiteConnect kc : collectionOfKiteConnect){
			
			String body = "<h3>Dear "+kc.getProfile().userName+", </h3><br/>"+							
							"Today's Instrument: "+todayInstrument+"<br/>"+
							"Today's Decession: "+todayDecession+"<br/>"+
							"Today's Instrument LTP in Pre-Open Market: "+todaysInstrumentOpen+"<br/><br/>"+
							"Thank You.<br/> Hardik Desai.";
						
			log.info("Sending Email To :"+kc.getProfile().email);
			emailService.sendEmail(kc.getProfile().email, subject, body);
		}
	}
		
	
}
