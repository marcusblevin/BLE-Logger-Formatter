package graphet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class Pace {
	private String date = null;
	private String time = null;
	private Double degF = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	
	public Pace(String d, String t, String f) {
		date = d;
		time = t;
		degF = Double.parseDouble(f);
	}
	
	public Calendar getDate() throws ParseException {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(sdf.parse(date+" "+time));
		return cal;
	}
	public Double getTemperature() {
		return degF;
	}
}