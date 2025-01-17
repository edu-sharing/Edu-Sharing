package org.edu_sharing.repository.server.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.alfresco.util.ISO8601DateFormat;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.alfresco.repository.server.authentication.Context;

public class DateTool {

	
	Logger logger = Logger.getLogger(DateTool.class);
	
	
	public String formatDate(Long time){
		return formatDate(time,DateFormat.LONG,DateFormat.SHORT);
	}
	
	/**
	 * 
	 * @param time
	 * @param dateStyle
	 * @param timeStyle set to null if you want only a date without time
	 * @return
	 */
	public String formatDate(Long time,Integer dateStyle, Integer timeStyle){
		String locale = null;
		
		if(Context.getCurrentInstance() != null){
			locale = (String)Context.getCurrentInstance().getRequest().getSession(true).getAttribute(CCConstants.AUTH_LOCALE);
		}
		
		if(locale == null || !locale.matches("[a-z][a-z]_[A-Z][A-Z]")){
			locale = "en_US";
		}
		String[] splittedLocale = locale.split("_");
		
		DateFormat df = null;
		
		if(timeStyle == null){
			df = DateFormat.getDateInstance(dateStyle, new Locale(splittedLocale[0],splittedLocale[1]));
		}else{
			df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, new Locale(splittedLocale[0],splittedLocale[1]));
		}
		df.setTimeZone(TimeZone.getTimeZone("CET"));

		
		try {
			
			Date date = new Date(time);
			return df.format(date);
			

		} catch (Exception e) {
			logger.error("Exception", e);
			return time.toString();
		}
	}
	
	
	 public java.util.Date getDate(String dateString){
			Date result = null;
			try{
				result = ISO8601DateFormat.parse(dateString);
			}catch(Exception e){
				logger.debug(dateString +" is no ISO String");
				
				SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy",Locale.US);
				try{
					result = df.parse(dateString);
				}catch(ParseException pe){
					logger.debug(dateString+" is no \"EEE MMM dd hh:mm:ss z yyyy\",Locale.US");
					DateFormat cetFormat = new SimpleDateFormat();
					TimeZone cetTime = TimeZone.getTimeZone("CET");
					cetFormat.setTimeZone(cetTime);
					try{
						result = cetFormat.parse(dateString);
					}catch(ParseException pe2){
						logger.debug(dateString+" is no CET");
						DateFormat gmtFormat = new SimpleDateFormat();
						TimeZone gmtTime = TimeZone.getTimeZone("GMT");
						gmtFormat.setTimeZone(gmtTime);
						try{
							result = new Date(Long.parseLong(dateString));
						}catch(Throwable pe3){
							try{
								result = gmtFormat.parse(dateString);
							}catch(ParseException pe4){
								logger.info(dateString+" is no GMT. Dont know what to do!");
							}
						}
					}
				}
			}
			return result;
		}
}
