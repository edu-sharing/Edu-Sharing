/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.client.tools;

public class StringTool {
	/**
	 * returns false when one of the Strings is null or empty
	 * @param strings
	 * @return
	 */
	public static boolean notNullEmpty(String[] strings){
		if(strings != null && strings.length > 0){
			boolean returnVal = true;
			for(int i = 0; i < strings.length; i++ ){
				if(!notNullEmpty(strings[i])) returnVal = false;
			}
			return returnVal;
		}else{
			return false;
		}
	}
	
	public static boolean allNullOrEmpty(String[] strings){
		if(strings != null && strings.length > 0){
			boolean returnVal = true;
			for(int i = 0; i < strings.length; i++ ){
				if(notNullEmpty(strings[i])) return false;
			}
			return returnVal;
		}else{
			return true;
		}
	}
	/**
	 * returns false when String is null or empty
	 * @param string
	 * @return
	 */
	public static boolean notNullEmpty(String string){
		if(string != null && !string.trim().equals("")) return true;
		else return false;
	}
	
	/**
	 * escapes the following chars:
	 * [
	 * ]
	 * @param toEscpape
	 * @return
	 */
	public static String escape(String toEscpape){
		
		toEscpape = toEscpape.replace("[","\\[");
		toEscpape = toEscpape.replace("]","\\]");
		
		return toEscpape;
	}
}
