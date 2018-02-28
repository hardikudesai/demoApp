package com.desai.autotrade.model;

public class PreviousHLPrize {
	
	private String key;
	
	private double yesterdayHigh;
	
	private double yesterdayLow;
	
	private double upperBand;
	
	private double lowerBand;
	

	public double getUpperBand() {
		return upperBand;
	}

	public void setUpperBand(double upperBand) {
		this.upperBand = upperBand;
	}

	public double getLowerBand() {
		return lowerBand;
	}

	public void setLowerBand(double lowerBand) {
		this.lowerBand = lowerBand;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public double getYesterdayHigh() {
		return yesterdayHigh;
	}

	public void setYesterdayHigh(double yesterdayHigh) {
		this.yesterdayHigh = yesterdayHigh;
	}

	public double getYesterdayLow() {
		return yesterdayLow;
	}

	public void setYesterdayLow(double yesterdayLow) {
		this.yesterdayLow = yesterdayLow;
	}
	
	
}
