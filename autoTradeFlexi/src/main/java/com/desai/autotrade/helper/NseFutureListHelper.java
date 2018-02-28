package com.desai.autotrade.helper;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class NseFutureListHelper {
	
	private static URL url;
	
	private static URLConnection urlConnection;
	
	public static List<String> getListOfNseFutureCompanies(){
		List<String> listofNseFutureCompanies = new ArrayList<String>();
		
		try {
			
			url = new URL("https://howutrade.in/indexdata/nse_fut_list.txt");
			
			urlConnection = url.openConnection();
			
			BufferedReader bufs = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			
			String s;
			
			while ((s = bufs.readLine()) != null){				
				listofNseFutureCompanies.add("NSE:"+s);
			}
			//Remove the Symbol Header from the list.			
			listofNseFutureCompanies.remove(0);
			
			bufs.close();
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			urlConnection = null;
			url = null;
		}
		
		return listofNseFutureCompanies;
	}
}
