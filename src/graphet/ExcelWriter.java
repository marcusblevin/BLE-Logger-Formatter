/**
 * Uses Apache POI library to write Excel file
 * Each cell is formatted as a string
 * 
 * Taken from: https://www.callicoder.com/java-write-excel-file-apache-poi/
 */
package graphet;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;


public class ExcelWriter {
	private String				fileName		= null;
	private List<String>		columns 		= null; // column headers
	private ArrayList<Reading>  readingList		= null;
	
	public ExcelWriter(String f, List<String> c, ArrayList<Reading> rl) {
		fileName 	= f;
		columns 	= c;
		readingList	= rl;
		
		try {
			// create a workbook
			// new XSSFWorkbook() for generating '.csv' file
			// new HSSFWorkbook() for gerating '.xls' file
			Workbook workbook = new XSSFWorkbook(); 
			
			/* CreationHelper helps us create instances of various things like DataFormat, 
	           Hyperlink, RichTextString etc, in a format (HSSF, XSSF) independent way */
			CellStyle cs 		= workbook.createCellStyle();
			CreationHelper ch	= workbook.getCreationHelper();
			cs.setDataFormat(ch.createDataFormat().getFormat("m/d/yyyy h:mm:ss"));
			
			DataFormat format 	= workbook.createDataFormat();
			CellStyle dcs 		= workbook.createCellStyle();
			dcs.setDataFormat(format.getFormat("#.##########"));
			
			// create a sheet
			Sheet sheet = workbook.createSheet("Sheet 1");
			
			// create header row
			Row headerRow = sheet.createRow(0);
			
			// add column headers for formula columns
			columns.add("G->H Diff");
			columns.add("H->P Diff");
			columns.add("G->P Diff");
			
			for (int i=0; i<columns.size(); i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(columns.get(i));
			}
			
			// write rows of values
			int rowNum = 1;
			for (Reading reading : readingList) {
				Row row = sheet.createRow(rowNum);
				rowNum++; // increment row counter to match up with formulas
				
				// default, uses Excel formulas to determine cell data type
				Cell cell0 = row.createCell(0);
				Cell cell1 = row.createCell(1);
				Cell cell2 = row.createCell(2);
				Cell cell3 = row.createCell(3);
				Cell cell4 = row.createCell(4);
				Cell cell5 = row.createCell(5);
				Cell cell6 = row.createCell(6);
				Cell cell7 = row.createCell(7);
				Cell cell8 = row.createCell(8);
				Cell cell9 = row.createCell(9);
				
				cell2.setCellType(CellType.NUMERIC);
				cell3.setCellType(CellType.NUMERIC);
				cell4.setCellType(CellType.NUMERIC);
				cell5.setCellType(CellType.NUMERIC);
				cell6.setCellType(CellType.NUMERIC);
				
				cell0.setCellValue(reading.getTagName());
				cell1.setCellValue(reading.getTimeStamp());
				cell2.setCellValue(reading.getAnalogRead());
				cell3.setCellValue(reading.getFormattedValue()); // D
				cell4.setCellValue(nullify(reading.getSudhirValue())); // E
				cell5.setCellValue(nullify(reading.getSudhirVoltage())); // F
				cell6.setCellValue(nullify(reading.getPaceValue())); // G
				
				cell1.setCellStyle(cs);
				cell2.setCellStyle(dcs);
				cell3.setCellStyle(dcs);
				cell4.setCellStyle(dcs);
				cell5.setCellStyle(dcs);
				cell6.setCellStyle(dcs);
				
				cell7.setCellType(CellType.FORMULA);
				cell8.setCellType(CellType.FORMULA);
				cell9.setCellType(CellType.FORMULA);
				cell7.setCellFormula("IFERROR((((D"+rowNum+"-E"+rowNum+")/E"+rowNum+")*100),\"\")");
				cell8.setCellFormula("IFERROR((((E"+rowNum+"-G"+rowNum+")/G"+rowNum+")*100),\"\")");
				cell9.setCellFormula("IFERROR((((D"+rowNum+"-G"+rowNum+")/G"+rowNum+")*100),\"\")");
				
			}
			
			
			// resize all columsn to fit the content size
			for (int i=0; i<columns.size(); i++) {
				sheet.autoSizeColumn(i);
			}
			
			
			// write output to a file
			FileOutputStream fos = new FileOutputStream(fileName);
			workbook.write(fos);
			fos.close();
			
			// close the workbook
			workbook.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String nullify(Double d) {
		if (d == null || d.toString().isEmpty()) {
			return "";
		}
		return d.toString();
	}
	
}