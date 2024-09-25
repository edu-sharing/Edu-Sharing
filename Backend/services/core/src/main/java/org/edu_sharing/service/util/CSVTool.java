package org.edu_sharing.service.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.edu_sharing.service.foldertemplates.LoggingErrorHandler;

import java.io.*;
import java.util.*;

public class CSVTool {

	private static Logger logger= Logger.getLogger(CSVTool.class);

	public static class CSVResult{
		List<String> headers;
		List<Map<String,String>> lines;

		public List<String> getHeaders() {
			return headers;
		}

		public void setHeaders(List<String> headers) {
			this.headers = headers;
		}

		public List<Map<String, String>> getLines() {
			return lines;
		}

		public void setLines(List<Map<String, String>> lines) {
			this.lines = lines;
		}
	}
	
	
	public static String ENC_ISO = "ISO-8859-1";
	
	public static String ENC_UTF8 = "UTF-8";

	public static CSVResult readCSV(BufferedReader in, char delimiter) {
		try {

			CSVParser parser = CSVParser.parse(in,CSVFormat.newFormat(delimiter).withQuote('"').withHeader());
			CSVResult result = new CSVResult();
			result.setHeaders(parser.getHeaderNames());
			ArrayList<Map<String, String>> lines = new ArrayList<>();
			for(CSVRecord record : parser.getRecords()){
				lines.add(record.toMap());
			}
			result.setLines(lines);
			return result;

		}catch(IOException e){
			logger.warn(e);
		}finally {
			try {
				in.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}
		return null;
	}


	public List<List<String>> getRecords(String file, String enc){
		try {
			return this.getRecords(new FileInputStream(file),enc);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<List<String>> getRecords(InputStream input, String enc){
		List<List<String>> result = new ArrayList<>();
		try {

			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							input, enc));

			CSVParser parser = CSVParser.parse(in,CSVFormat.newFormat(';'));
			for(CSVRecord record : parser.getRecords()){
				result.add(record.toList());
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}


	public static void main(String[] args){
		String file = "/Users/mv/git/Edu-Sharing-Internal/Backend/extensions/Mebis/schoolList.csv";

		new CSVTool().getRecords(file,ENC_UTF8);
	}

}
