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
package org.edu_sharing.webservices.crud;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import org.edu_sharing.repository.client.tools.CCConstants;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		CrudServiceLocator locator = new CrudServiceLocator();
		locator.setcrudEndpointAddress("http://localhost:8080/edu-sharing/services/crud");

		File file = new File("C:/Users/rudi/Documents/AddUser.jpg");
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fileInputStream.read(data);
			fileInputStream.close();
			
			
			Crud crud = locator.getcrud();
			Map<String,String> properties = new HashMap<>();
	        properties.put(CCConstants.CM_PROP_C_TITLE, "3AddUser.jpg"+System.currentTimeMillis());
	        properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, "3AddUser.jpg"+System.currentTimeMillis());
	        properties.put(CCConstants.CCM_PROP_IO_OBJECTTYPE, "0");
			
	        //crud.create("admin", "TICKET_d4929bdf56c8f9af0a2c4994583bf0ab44bbbecc", CCConstants.CCM_TYPE_IO, "local", properties, data, null);
			
	        crud.update("admin", "TICKET_d4929bdf56c8f9af0a2c4994583bf0ab44bbbecc", CCConstants.CCM_TYPE_IO, "local", "2fb74718-2790-4c81-a311-1f3f8104e19d", properties, data, null);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
