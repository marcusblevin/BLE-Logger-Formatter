package graphet;

public class Logger {
	
	private String 		deviceID 		= "";
	private Sensor[] 	sensors 		= null; 
	private String 		location		= "";
	
	public Logger(String d, Sensor[] s, String l) {
		deviceID = d;
		sensors = s;
		location = l;
	}
	
	public String getDeviceID() {
		return deviceID;
	}
	public Sensor[] getSensors() {
		return sensors;
	}
	public String getLocation() {
		return location;
	}
}