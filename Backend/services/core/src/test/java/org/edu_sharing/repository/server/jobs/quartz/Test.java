package org.edu_sharing.repository.server.jobs.quartz;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.importer.PersistentHandlerInterface;
import org.edu_sharing.repository.server.importer.RecordHandlerInterface;
import org.edu_sharing.repository.server.importer.RecordHandlerInterfaceBase;
import org.edu_sharing.repository.server.importer.RecordHandlerLOM;
import org.w3c.dom.Node;

public class Test {

	public static void main(String[] args) {
		PersistentHandlerInterface ph = new PersistentHandlerInterface() {
			@Override
			public boolean mustBePersisted(String replId, String timeStamp) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public String safe(RecordHandlerInterfaceBase recordHandler, String cursor, String set) throws Throwable {
				return null;
			}
			
			@Override
			public boolean exists(String replId) {
				// TODO Auto-generated method stub
				return false;
			}
		};
		
		TestRecordHandler trh = new TestRecordHandler();
		
		try {
			//OAIPMHLOMImporter imp = new OAIPMHLOMImporter("http://sodis.de/cp/oai_pmh/oai.php",ph, trh, null,-1, -1, "oai_lom-de", new String[] {"omega_activated"});
			//imp.startImport();
			System.out.println("count:" + trh.getCounter());
			for(Map.Entry<String,Integer> entry : trh.getReplicationIdCounter().entrySet()) {
				System.out.println("replid:" + entry.getKey() +": " + entry.getValue());
			}
			
			for(Map.Entry<String,Integer> entry : trh.getReplicationSourceCounter().entrySet()) {
				System.out.println("source:" + entry.getKey() +": " + entry.getValue());
			}
		
			
		}catch(Throwable e) {
			e.printStackTrace();
		}

	}
	
	private static class TestRecordHandler implements RecordHandlerInterface{
		
		@Getter
        Map<String,Integer> replicationIdCounter = new HashMap<>();
		
		@Getter
        Map<String,Integer> replicationSourceCounter = new HashMap<>();
		
		Log logger = LogFactory.getLog(RecordHandlerLOM.class);
		XPathFactory pfactory = XPathFactory.newInstance();
		XPath xpath = pfactory.newXPath();
		
		
		
		@Getter
        int counter = 0;
		@Override
		public Map<String, Object> getProperties() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {
			counter = counter++;
			
			String replicationId = (String) xpath.evaluate("metadata/lom/general/identifier/entry", nodeRecord, XPathConstants.STRING);
			String replicationIdPrefix = replicationId.split("-")[0];
			Integer replInt = replicationIdCounter.get(replicationIdPrefix);
			if(replInt == null) {
				replicationIdCounter.put(replicationIdPrefix, 1);
			}else {
				replicationIdCounter.put(replicationIdPrefix, replInt++);
			}
			
			
			String lomCatalogId = (String) xpath.evaluate("metadata/lom/general/identifier/catalog", nodeRecord, XPathConstants.STRING);
			Integer sourceCounter = replicationSourceCounter.get(lomCatalogId);
			if(sourceCounter == null) {
				replicationSourceCounter.put(lomCatalogId, 1);
			}else {
				replicationSourceCounter.put(lomCatalogId, sourceCounter++);
			}
		}

    }

}
