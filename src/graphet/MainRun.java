package graphet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainRun {
	public static void main(String[] args) {
		
		String targetDir 							= "C:\\CSVDownloader\\output";
		String processedDir							= "C:\\CSVDownloader\\output\\processed\\";
		File targetFolder							= new File(targetDir);
		File processedFolder						= new File(processedDir);
		List<File> files							= listFilesInFolder(targetFolder);
		String line									= "";
		List<String> l								= null;
		BufferedReader br							= null;
		BufferedWriter bw							= null;
		StringBuilder sb							= null;
		SimpleDateFormat sdf						= new SimpleDateFormat("yyyy-MM-dd H:m:s");
		
		// create folder if not exist
		if (!processedFolder.exists()) {
			processedFolder.mkdir();
		}
		
		// get mapping of Loggers to Sensors
		Logger[] loggerMap = getLoggerMap("loggerMap.json");
		System.out.println("Logger Map: "+loggerMap.length);
		
		
		for (File file : files) {
			System.out.println("Processing: "+file.getName());
			
			// first line of file is comment line
			// second line is [Data] string
			// third line is column headers: TagName, TimeStamp (UTC), Value
			
			// each TimeStamp has 6 entries (a0...a5) for each sensor
			
			String colHeaders = "";
			
			try {
				br = new BufferedReader(new FileReader(file.getPath()));
				bw = new BufferedWriter(new FileWriter(new File(processedDir+file.getName())));
				sb = new StringBuilder();
				
				
				if ((line = br.readLine()) != null) {
					// first line, don't do anything for now
				}
				if ((line = br.readLine()) != null) {
					// second line, don't do anything for now
				}
				if ((line = br.readLine()) != null) {
					// third line, assuming column order won't change but might want
					// to add check here to assign column order for below
					colHeaders = line;
					colHeaders += ",FormattedValue"; 
					sb.append(colHeaders);
					sb.append("\n");
				}
				

				// read each line of sensor data, formatting values
				while ((line = br.readLine()) != null) {
					l = new LinkedList<String>(Arrays.asList(line.split(",")));
					
					// find out which sensor this is
					String[] tmp 		= l.get(0).split("_");
					String deviceID 	= tmp[0];
					String node 		= tmp[2];
					String type			= getSensorType(loggerMap, deviceID, node);
					
					Date timeStampUTC		= new Date((long)Long.parseLong(l.get(1))*1000);
					String timeStamp		= sdf.format(timeStampUTC);
					Double analogRead 		= Double.parseDouble(l.get(2));
					Double convertedValue 	= null;
					
					// building string
					sb.append(l.get(0)+",");
					sb.append(timeStamp+",");
					sb.append(analogRead+",");
					

					if (type != null && !type.isEmpty()) {
						switch (type) {
							case "Temperature":
								convertedValue = convertTemp(analogRead);
								break;
							case "Pressure":
								convertedValue = convertPressure(analogRead);
								break;
							default:
								convertedValue = null;
								break;
						}
					}
					
					sb.append(convertedValue+"\n");
				}
				
				bw.write(sb.toString()); // limit IO by only writing out once to file
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (br != null || bw != null) {
					try {
						br.close(); // close file 
						bw.close();
						sb = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} // end try/catch
		} // end for loop
		
		System.out.println("done");
	}
	
	/**
	 * Find a match in Logger Map based on Device ID and Node. Return null if no match found
	 * @param Logger[] loggerMap
	 * @param String deviceID
	 * @param String node
	 * @return String|null
	 */
	public static String getSensorType(Logger[] loggerMap, String deviceID, String node) {
		for (Logger l : loggerMap) {
			if (l.getDeviceID().equals(deviceID)) {
				Sensor[] sensors = l.getSensors();
				
				for (Sensor s : sensors) {
					if (s.getNode().equals(node)) {
						return s.getType();
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * List all CSV files in a folder
	 * @param File folder
	 * @return List<File>
	 */
	public static List<File> listFilesInFolder(final File folder) {
		List<File> files = new ArrayList<File>();
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				// listFilesInFolder(fileEntry); // recursively list files in sub-folders
			} else if (fileEntry.getName().contains(".csv")) {
				files.add(fileEntry);
			}
		}
		return files;
	}
	
	/**
	 * read in JSON file of logger/sensor definitions
	 * @param fname
	 * @return Logger[]
	 */
	public static Logger[] getLoggerMap(String fname) {
		Logger[] l = null;
		
		try {
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(new ArrayAdapterFactory()).create();
			BufferedReader br 	= new BufferedReader(new FileReader(fname));
			String line 		= "";
			String json			= "";
			
			while ((line = br.readLine()) != null) {
				json += line.trim();
			}
			br.close();
			
			// convert JSON string to array of objects
			l = gson.fromJson(json, Logger[].class);
			
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find "+fname+". Please provide a valid file name/location:");
			
			Scanner s 		= new Scanner(System.in);
			String newFName = s.next();
			s.close();
			
			return getLoggerMap(newFName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return l;
	}
	
	/**
	 * Convert analog read temperatue to Celsius. Equations received from Sudhir extrapolated from Pace logger lookup table
	 * Analog read needs to be multiplied by the voltage fed in to the sensor to get Ohms
	 * @param temp
	 * @return String|null
	 */
	public static Double convertTemp(Double temp) {
		temp *= 3.553619604316547; // multiple by voltage of sensor. Back calculated from equations from Sudhir
		//temp *= 3.7; // voltage of battery
		Double t = null;
		if (temp >= 0 && temp <= 1000) {
			t = 799.82 * Math.pow(temp,-0.266);
		} else if (temp > 1000 && temp <= 2000) {
			t = 856.87 * Math.pow(temp,-0.278);
		} else if (temp > 2000 && temp <= 10000) {
			t = 0.0000000000000146133677120283 * Math.pow(temp,4) - 
					0.000000000431703936797824 * Math.pow(temp,3) + 
					0.0000050099630566131 * Math.pow(temp,2) - 
					0.0308347369029266 * temp + 
					145.741960429525;
		} else if (temp > 10000 && temp <= 20000) {
			t = -4e-12 * Math.pow(temp,3) + 
					2e-07 * temp * 2 + 
					94.402;
		} else if (temp > 20000) {
			t = -0.000000000000000000000007331762 * Math.pow(temp,5) + 
					0.000000000000000003098756319404 * Math.pow(temp,4) - 
					0.000000000000525855411865501 * Math.pow(temp,3) + 
					0.0000000465883023632026 * Math.pow(temp,2) - 
					0.00246604466283715 * temp + 
					68.9550882416556;
		}
		
		return t;
	}
	
	
	public static Double convertPressure(Double press) {
		return null;
	}
	
	private static Double toCelsius(Double t) {
		return ((0.5556)*(t-32.0));
	}
	 
	private static Double toFahrenheit(Double t) {
		return ((1.8*t)+32.0);
	}
}