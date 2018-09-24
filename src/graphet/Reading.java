package graphet;

import java.util.Calendar;

public class Reading {
	
	private String TagName 			= null;
	private Calendar TimeStamp 		= null;
	private Double AnalogRead 		= null;
	private Double FormattedValue 	= null;
	private Double SudhirValue 		= null;
	private Double SudhirVoltage 	= null;
	private Double PaceValue 		= null;
	
	
	public Reading(String t, Calendar ts, Double ar, Double fv, Double sva, Double svo, Double pv) {
		TagName 		= t;
		TimeStamp 		= ts;
		AnalogRead 		= ar;
		FormattedValue 	= fv;
		SudhirValue		= sva;
		SudhirVoltage	= svo;
		PaceValue 		= pv;
	}
	
	public String getTagName() {
		return TagName;
	}
	public Calendar getTimeStamp() {
		return TimeStamp;
	}
	public Double getAnalogRead() {
		return AnalogRead;
	}
	public Double getFormattedValue() {
		return FormattedValue;
	}
	public Double getSudhirValue() {
		return SudhirValue;
	}
	public Double getSudhirVoltage() {
		return SudhirVoltage;
	}
	public Double getPaceValue() {
		return PaceValue;
	}
}