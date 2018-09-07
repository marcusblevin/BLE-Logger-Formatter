package graphet;

public class Sensor {
	
	private String type = "";
	private String node = "";
	
	public Sensor(String t, String n) {
		type = t;
		node = n;
	}
	
	public String getType() {
		return type;
	}
	public String getNode() {
		return node;
	}
	
}