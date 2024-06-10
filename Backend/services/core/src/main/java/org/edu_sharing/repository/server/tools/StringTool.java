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
package org.edu_sharing.repository.server.tools;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class StringTool {
	
	private static final String PHRASE_DELIMITER = "\"";
	private static final String SPACE_DELIMITER = " ";
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public static String getEncoded(byte[] value){
		return new String(value,Charset.forName("UTF-8"));
	}
	
	public static String[] getPhrases(String str) {
		
		List<String> result = new ArrayList<>();
		
		String[] tokens = str.split(PHRASE_DELIMITER);
		
		boolean isPhrase = 
				(  tokens.length > 0 
				&& tokens[0].length() == 0 );
		
		for ( int i = (isPhrase ? 1 : 0)
				, c = tokens.length
			; i < c
			; ++i) {
			
			String token = tokens[i];
			
			if (isPhrase) {
				
				result.add(PHRASE_DELIMITER + token + PHRASE_DELIMITER);
				
			} else {
				
				for (String subToken : token.split(SPACE_DELIMITER)) {

					if (subToken.length() > 0) {
						
						result.add(subToken);
					}
					
				}
			}
			
			isPhrase = (! isPhrase);
		}
		
		return result.toArray(new String[0]);

	}
}
