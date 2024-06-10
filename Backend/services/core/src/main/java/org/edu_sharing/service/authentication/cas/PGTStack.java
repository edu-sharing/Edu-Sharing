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
package org.edu_sharing.service.authentication.cas;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 
 * Klasse die einen in Memory Stack von ProxyGrantingTickets
 *         verwaltet
 */
public class PGTStack {

	private static Map<String, String> map = new HashMap<>();

	/**
	 * 
	 * @param pgtIou
	 * @return an Proxy Granting Ticket for an ProxyTicket
	 */
	
	public static void add(String pgtIou, String pgt){
		map.put(pgtIou, pgt);
		for(String key :map.keySet()){
			System.out.println("PGT: "+map.get(key));
		}
	}
	
	/**
	 * 
	 * @param pgtIou
	 * @return
	 */
	public static String getPGT(String pgtIou) {
		return map.get(pgtIou);
	}
	
	public static void remove(String pgtIou){
		map.remove(pgtIou);
	}
	
	public static Set<String> getPgtIous(){
		return map.keySet();
	}
}
