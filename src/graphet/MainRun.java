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
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainRun {
	
	private static ArrayList<Double> 	ohms 				= new ArrayList<Double>();
	private static ArrayList<Double>	degC 				= new ArrayList<Double>();
	private static String 				targetDir 			= "C:\\CSVDownloader\\output";
	private static String 				processedDir		= "C:\\CSVDownloader\\output\\processed\\";
	private static File 				targetFolder		= new File(targetDir);
	private static File 				processedFolder		= new File(processedDir); 
	
	public static void main(String[] args) {
		
		setPaceLookupTable("C.RVT");
		
		
		// ask user what they want to do
		Scanner scan 	= new Scanner(System.in);
		boolean run 	= true;
		
		try {
			while (run) {
				System.out.println("What would you like to do?");
				System.out.println("calcVoltage, convertLogs, compareTemps, or exit");
				String s = scan.nextLine();
				if (s.equals("exit")) {
					scan.close();
					run = false;
				} else if (s.equals("calcVoltage")) {
					// prompt for back calculating degC or ohms based on a known value
					calculateVoltage();
				} else if (s.equals("convertLogs")) {
					convertLogs(false);
					System.out.println("finished");
				} else if (s.equals("compareTemps")) {
					convertLogs(true);
					System.out.println("finished");
				} else {
					System.out.println("unrecognized input");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			scan.close();
		}
		
		
		System.out.println("bye");
	}
	
	
	/**
	 * Perform actions to read in log files and convert values from analog read to desired format
	 * @param boolean compare - switch to enable comparing temperatue values with known readings
	 */
	public static void convertLogs(boolean compare) {
		
		List<File> files							= listFilesInFolder(targetFolder);
		String line									= "";
		List<String> l								= null;
		BufferedReader br							= null;
		BufferedWriter bw							= null;
		StringBuilder sb							= null;
		SimpleDateFormat sdf						= new SimpleDateFormat("yyyy-MM-dd H:m:s");
		ArrayList<Reading> readingList				= new ArrayList<Reading>();
		String colHeaders 							= "";
		ArrayList<SudhirReading> readingMap			= null;
		ArrayList<Pace> paceMap						= null;
		Calendar paceMinDate 						= null;
		
		
		// create folder if not exist
		if (!processedFolder.exists()) {
			processedFolder.mkdir();
		}
		
		// get mapping of Loggers to Sensors
		Logger[] loggerMap = getLoggerMap("loggerMap.json");
		System.out.println("Logger Map: "+loggerMap.length);
		
		if (compare) {
			// get mapping of current temperatures to back-calculate voltage
			readingMap = getConvertedReadings("readings.json");
			System.out.println("Reading Map: "+readingMap.size());
			
			// get mapping of Pace temperatures
			paceMap = getPaceReadings("PaceTempLogs.CSV");
			System.out.println("Pace Map: "+paceMap.size());
	
			for (Pace p : paceMap) {
				try {
					if (paceMinDate == null || p.getDate().compareTo(paceMinDate) < 0) {
						paceMinDate = p.getDate();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		
		for (File file : files) {
			
			System.out.println("Processing: "+file.getName());
			
			// first line of file is comment line
			// second line is [Data] string
			// third line is column headers: TagName, TimeStamp (UTC), Value
			
			// each TimeStamp has 6 entries (a0...a5) for each sensor
			
			colHeaders = "";
			
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
					colHeaders = line+",GraphetValue";
					if (compare) {
						colHeaders += ",HydroTrekTemp,CalculatedVoltage,PaceTemp";
					}
					sb.append(colHeaders);
					sb.append("\n");
				}
				

				// read each line of sensor data, formatting values
				while ((line = br.readLine()) != null) {
					l = new LinkedList<String>(Arrays.asList(line.split(",")));
					
					// find out which sensor this is
					String tagName		= l.get(0);
					String[] tmp 		= tagName.split("_");
					String deviceID 	= tmp[0];
					String node 		= tmp[2];
					String type			= getSensorType(loggerMap, deviceID, node);
					
					Calendar timeStampUTC	= Calendar.getInstance();
					timeStampUTC.setTimeInMillis(Long.parseLong(l.get(1))*1000);
					//Date timeStampUTC		= new Date((long)Long.parseLong(l.get(1))*1000);
					String timeStamp		= sdf.format(timeStampUTC.getTime());
					Double analogRead 		= Double.parseDouble(l.get(2));
					Double convertedValue 	= null;
					
					// building string
					sb.append(tagName+",");
					sb.append(timeStamp+",");
					sb.append(analogRead+",");
					
					Double SudhirVal = null;
					Double SudhirVoltage = null;
					Double PaceTemp = null;
					

					if (type != null && !type.isEmpty()) {
						switch (type) {
							case "Temperature":
								convertedValue = convertTempLI(analogRead);
								
								if (compare) {
									for (SudhirReading s_read : readingMap) {
										if (s_read.getTimeUTC().getTime() == timeStampUTC.getTimeInMillis()) {
											// found a matching date already defined
											SudhirVal = s_read.getDegC();
											Double s_ohms = calculateVoltage(SudhirVal, "degC");
											SudhirVoltage = s_ohms / analogRead;
											readingMap.remove(readingMap.indexOf(s_read)); // remove element from List to decrease list size and improve speed
											break; // break loop
										}
									}
									
									// only run if we have values for the time period
									// this step is taking the most time, need to make more efficient
									if (timeStampUTC.getTimeInMillis() >= paceMinDate.getTimeInMillis()) {
										for (Pace p_read : paceMap) {
											Calendar pDate = p_read.getDate();
											
											// times don't match up exactly but still in 10 second slices
											// create 10 second spread of pace value to match up with BLE logger data
											Calendar before = (Calendar) pDate.clone();
											Calendar after = (Calendar) pDate.clone();
											before.add(Calendar.SECOND, -5);
											after.add(Calendar.SECOND, 5);
											
											// find the first match to the time stamp within a 10 second window
											if (timeStampUTC.getTimeInMillis() >= before.getTimeInMillis() && timeStampUTC.getTimeInMillis() <= after.getTimeInMillis()) {
												PaceTemp = toCelsius(p_read.getTemperature());
												paceMap.remove(paceMap.indexOf(p_read)); // remove element from List to decrease list size and improve speed
												break; // break loop
											}
										}
									}
								}
								
								break;
							case "Pressure":
								convertedValue = convertPressure(analogRead);
								break;
							default:
								convertedValue = null;
								break;
						}
					}
					
					sb.append(convertedValue+",");
					if (compare) {
						sb.append(SudhirVal+",");
						sb.append(SudhirVoltage+",");
						sb.append(PaceTemp);
					}
					
					sb.append("\n");
					
					// add to ArrayList to track all entries for log camparisons
					if (compare && convertedValue != null && SudhirVal != null) {
						readingList.add(new Reading(tagName, timeStampUTC, analogRead, convertedValue, SudhirVal, SudhirVoltage, PaceTemp));
					}
				}
				
				bw.write(sb.toString()); // limit I/O by only writing out once to file
				
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
		
		
		if (compare) {
			System.out.println("Writing aggregate Excel file");
			List<String> columns = new LinkedList<String>(Arrays.asList(colHeaders.split(",")));
			new ExcelWriter("logComparison.xlsx", columns, readingList);
		}
	}
	
	
	
	
	/**
	 * Use linear interpolation to find missing value given known value
	 * @param Double i - known value
	 * @param String s - degC or ohms for what is the known value
	 * @return
	 */
	public static Double calculateVoltage(Double i, String s) {
		Integer[] k = getTableKeys(i,s);
		Double r = null;
		
		if (s.equals("degC")) {
			Double x0	= degC.get(k[0]);
			Double y0	= ohms.get(k[0]);
			Double x1	= degC.get(k[1]);			
			Double y1	= ohms.get(k[1]);
			
			r = linearInterpolate(x0,y0,x1,y1,i);
		} else {
			Double x0	= ohms.get(k[0]);
			Double y0	= degC.get(k[0]);
			Double x1	= ohms.get(k[1]);			
			Double y1	= degC.get(k[1]);
			
			r = linearInterpolate(x0,y0,x1,y1,i);
		}
		return r;
	}
	
	/**
	 * Prompt user for inputs to back-calculate degC or ohms from given values
	 */
	public static void calculateVoltage() {
		
		Scanner scan = new Scanner(System.in);
		boolean run = true;
		
		try {
			while (run) {
				System.out.println("Know degC or ohms or exit: ");
				String s = scan.nextLine();
				if (s.equals("exit")) {
					scan.close();
					run = false;
				} else {
					System.out.println("Enter known value: ");
					Double i = scan.nextDouble();
					scan.nextLine(); // consume new line
					
					if (!i.isNaN()) {
						Double r = calculateVoltage(i,s);
						
						System.out.println("returned: "+r.toString());
					}
					System.out.println("Go again? (y/n): ");
					String b = scan.nextLine();
					
					if (b.equals("n")) {
						scan.close();
						run = false;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			scan.close();
		}
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
	 * Get readings converted by Sudhir
	 * @param String fname
	 * @return Reading array
	 */
	public static ArrayList<SudhirReading> getConvertedReadings(String fname) {
		SudhirReading[] r_temp 		= null;
		ArrayList<SudhirReading> r 	= null;
		
		try {
			Gson gson 			= new GsonBuilder().registerTypeAdapterFactory(new ArrayAdapterFactory()).create();
			BufferedReader br 	= new BufferedReader(new FileReader(fname));
			String line 		= "";
			String json			= "";
			
			while ((line = br.readLine()) != null) {
				json += line.trim();
			}
			br.close();
			
			// convert JSON string to array of objects
			r_temp = gson.fromJson(json, SudhirReading[].class);
			r = new ArrayList<SudhirReading>(Arrays.asList(r_temp)); // convert array to ArrayList			
			
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find "+fname+". Please provide a valid file name/location:");
			
			Scanner s 		= new Scanner(System.in);
			String newFName = s.next();
			s.close();
			
			return getConvertedReadings(newFName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return r;
	}
	
	/**
	 * Get readings from Pace logger
	 * @param String fname
	 * @return ArrayList<Pace>
	 */
	public static ArrayList<Pace> getPaceReadings(String fname) {
		ArrayList<Pace> p 	= new ArrayList<Pace>();
		String line			= "";
		List<String> l		= null;
		
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(fname));

			/*
			// skip first 17 lines
			for (int i=0; i<17; i++) {
				if ((line = br.readLine()) != null) {
					// do nothing
				}
			}
			*/
			
			// skip first line as column headers
			if ((line = br.readLine()) != null) {
				// do nothing
			}
			
			while ((line = br.readLine()) != null) {
				l = new LinkedList<String>(Arrays.asList(line.split(",")));
				Pace pace = new Pace(l.get(0), l.get(1), l.get(2));
				p.add(pace);
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find "+fname+". Please provide a valid file name/location:");
			
			Scanner s 		= new Scanner(System.in);
			String newFName = s.next();
			s.close();
			
			return getPaceReadings(newFName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return p;
	}
	
	
	/**
	 * Read in Pace provided lookup table for converted 
	 * @param String fname
	 */
	public static void setPaceLookupTable(String fname) {
		try {
			BufferedReader br 	= new BufferedReader(new FileReader(fname));
			String line 		= "";
			
			// skip first 3 lines as column headers and other unknown information
			br.readLine();
			br.readLine();
			br.readLine();
			
			while ((line = br.readLine()) != null) {
				line = line.trim(); // remove leading spaces
				String[] l 		= line.split(" "); // lines are justified with spaces in between
				try {
					degC.add(Double.parseDouble(l[0])); // first element
					ohms.add(Double.parseDouble(l[l.length-1])); // last element
				} catch (Exception e) {
					System.out.println("Could not convert string to number:"+l[l.length-1]);
					e.printStackTrace();
				}
			}
			br.close();
		} catch(FileNotFoundException e) {
			System.out.println("Cannot find "+fname+". Please provide a valid file name/location:");
			
			Scanner s 		= new Scanner(System.in);
			String newFName = s.next();
			s.close();
			
			setPaceLookupTable(newFName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Use Pace provided lookup table to perform linear interpolation of read converted to ohms
	 * If no keys corresponding to lookup table are found, call convertTemp to use logarithmic equations
	 * @param HashMap<Double,Integer> lookupTable
	 * @param Double read
	 * @return Double|null
	 */
	public static Double convertTempLI(Double read) {
		// This step is currently a magic number that should be the voltage of the sensor at the time of the read.
		// Since we don't have the voltage at the read time have to use an approximation based on existing data. 
		// Haven't been able to get an explanation from this from Sudhir
		read *= 3.558300492; // Back calculated from conversions by Sudhir. Should yield ~0.15% average variance
		//read *= 3.65; // voltage of battery
		
		Integer[] k = getTableKeys(read,"ohms");
		
		if (k != null) {
			Double x0 = ohms.get(k[0]);
			Double y0 = degC.get(k[0]);
			Double x1 = ohms.get(k[1]);			
			Double y1 = degC.get(k[1]);
			
			return linearInterpolate(x0,y0,x1,y1,read);
		} else {
			// keys were not set in lookup table, must be out of bounds
			// send read to convertTemp function to use equations to find temperature value
			return convertTemp(read);
		}
	}
	
	/**
	 * Linear Interpolation function derived from https://en.wikipedia.org/wiki/Linear_interpolation
	 * @param Double x0 - ohms
	 * @param Double y0 - degC
	 * @param Double x1 - ohms
	 * @param Double y1 - degC
	 * @param Double x - reading ohms
	 * @return Double
	 */
	public static Double linearInterpolate(Double x0, Double y0, Double x1, Double y1, Double x) {
		return (y0*(x1-x) + y1*(x-x0))/(x1-x0);
	}
	
	/**
	 * Traverse through degC or ohms arrays to find keys corresponding to the value range the provided value falls between 
	 * @param Double r - value search for in range
	 * @param String s - degC or ohms
	 * @return
	 */
	public static Integer[] getTableKeys(Double r, String s) {
		Integer[] k = new Integer[2];
		
		// keys are traversed in order of insertion
		// key numbers will be in order numerically
		if (s.equals("degC")) {
			for (int i=0; i<degC.size(); i++) {
				if ((i+1) <= degC.size() && degC.get(i) <= r && degC.get(i+1) >= r) {
					k[0] = i;
					k[1] = i+1;
					i = degC.size(); // exit
				}
			}
		} else {
			for (int i=0; i<ohms.size(); i++) {
				if ((i+1) <= ohms.size() && ohms.get(i) >= r && ohms.get(i+1) <= r) {
					k[0] = i;
					k[1] = i+1;
					i = ohms.size(); // exit
				}
			}
		}
		
		if (k[0] == null) {
			return null; // couldn't find a match, return null to trigger conversion by equation
		}
		
		return k;
	}
	
	/**
	 * Convert analog read temperatue to Celsius. Equations received from Sudhir extrapolated from Pace logger lookup table
	 * @param read
	 * @return Double|null
	 */
	public static Double convertTemp(Double read) {
		Double t = null;
		if (read >= 0 && read <= 1000) {
			t = 799.82 * Math.pow(read,-0.266);
		} else if (read > 1000 && read <= 2000) {
			t = 856.87 * Math.pow(read,-0.278);
		} else if (read > 2000 && read <= 10000) {
			t = 0.0000000000000146133677120283 * Math.pow(read,4) - 
					0.000000000431703936797824 * Math.pow(read,3) + 
					0.0000050099630566131 * Math.pow(read,2) - 
					0.0308347369029266 * read + 
					145.741960429525;
		} else if (read > 10000 && read <= 20000) {
			t = -4e-12 * Math.pow(read,3) + 
					2e-07 * read * 2 + 
					94.402;
		} else if (read > 20000) {
			t = -0.000000000000000000000007331762 * Math.pow(read,5) + 
					0.000000000000000003098756319404 * Math.pow(read,4) - 
					0.000000000000525855411865501 * Math.pow(read,3) + 
					0.0000000465883023632026 * Math.pow(read,2) - 
					0.00246604466283715 * read + 
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