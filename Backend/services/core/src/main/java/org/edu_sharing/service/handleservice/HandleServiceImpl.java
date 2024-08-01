package org.edu_sharing.service.handleservice;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.ibatis.session.SqlSession;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;

import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.AuthenticationInfo;
import net.handle.hdllib.Common;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Resolver;
import net.handle.hdllib.Util;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HandleServiceImpl implements HandleService{

	private final Config config;
	String privkeyPath = null;
	
	
	static String handleServerPrefix = null;
	static String handleServerRepoId = null;
	
	String id = null;
	Integer idIndex = 300;
	
	
	Resolver resolver = new Resolver();

	@Override
	public boolean available() {
		try{
			handleServiceAvailable();
			return true;
		}catch (Exception e){
			return false;
		}
	}

	@Override
	public String generateId() throws Exception {
		return this.generateHandle();
	}

	@Override
	public String create(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
		return createHandle(handleId, getDefautValues(getContentLink(properties)));
	}

	@Override
	public String update(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
		return updateHandle(handleId, getDefautValues(getContentLink(properties)));
	}

	@Override
	public String delete(String handleId, String nodeId) throws Exception {
		//not implemented
		return "";
	}

	@Override
	public String getHandleIdProperty() {
		return CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID;
	}

	public HandleServiceImpl() throws HandleServiceNotConfiguredException{
		config = LightbendConfigLoader.get().getConfig("repository.handleservice");
		if(config.hasPath("repoid")) {
			try {
				handleServerPrefix = config.getString("prefix");
				handleServerRepoId = config.getString("repoid");
				privkeyPath = config.getString("privkey");
				/**
				 * config dir: must be writeable
				 */
				String configDir = config.getString("configDir");
				if (configDir != null) {
					System.setProperty("net.handle.configDir", configDir);
				}
				id = "0.NA/" + handleServerPrefix;
			} catch (Throwable t) {
				log.error("Could not initialize handle service properly cause of error in config, please check config for \"repository.handleservice\"", t);
			}
		}
	}
	
	
	public HandleValue[] getHandleValues(String handle, String[] values) {
		try {
			HSAdapter adapter = HSAdapterFactory.newInstance(id,idIndex,getPrivateKeyBytes(),null);
			return adapter.resolveHandle(handle, values, null);
			
		} catch (HandleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return null;
	}
	
	public void handleServiceAvailable() throws HandleException, IOException{
		HSAdapterFactory.newInstance(id,idIndex,getPrivateKeyBytes(),null);
	}
	
	public String createHandle(String handle, final HandleValue[] values) throws Exception {
		HSAdapter adapter = HSAdapterFactory.newInstance(id,idIndex,getPrivateKeyBytes(),null);
		adapter.createHandle(handle, values);
		return handle;
	}
	public String updateHandle(String handle, final HandleValue[] values) throws Exception {
		HSAdapter adapter = HSAdapterFactory.newInstance(id,idIndex,getPrivateKeyBytes(),null);
		adapter.updateHandleValues(handle, values);
		return handle;
	}
	
	
	public void deleteHandle(String handle) throws Exception {
		HSAdapter adapter = HSAdapterFactory.newInstance(id,idIndex,getPrivateKeyBytes(),null);
		adapter.deleteHandle(handle);
	}
	

	public AuthenticationInfo getAuthenticationInfo(String id, Integer idIndex) {
		PrivateKey privKey = loadPrivateKeyFromFile();
		if (privKey == null) {
			return null;
		}
		return new PublicKeyAuthenticationInfo(Util.encodeString(id), idIndex, privKey);
	}

	private PrivateKey loadPrivateKeyFromFile() {

		try {

			return Util.getPrivateKeyFromBytes(getPrivateKeyBytes(), 0);

		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}
	
	byte[] getPrivateKeyBytes() throws IOException {
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte buf[] = new byte[4096];
		FileInputStream fin = new FileInputStream(privkeyPath);
		try {
			int r = 0;
			while ((r = fin.read(buf)) >= 0)
				bout.write(buf, 0, r);
		} finally {
			fin.close();
		}
		buf = bout.toByteArray();
		return buf;
	}
	
	
	public static void main(String[] args) {
		
		String handleId = RandomStringUtils.randomAlphabetic(HANDLE_ID_LENGTH);
		System.out.println(handleId);
		if(true) return;
		HandleServiceImpl main;
		try {
			main = new HandleServiceImpl();
		
		HandleValue[] hvs =  main.getHandleValues(handleServerPrefix + "/901-LEO-ORG",
				new String[] {new String(Common.STD_TYPE_EMAIL), new String(Common.STD_TYPE_URL)});
		
		for(HandleValue hv : hvs) {
			System.out.println(hv.getDataAsString() + " " +  hv.getIndex());
		}
		
		HandleValue hv = new HandleValue(1,Common.STD_TYPE_URL,"https://google.de".getBytes());

		main.createHandle(handleServerPrefix +"/905-google",new HandleValue[] {hv});
				
		} catch (HandleServiceNotConfiguredException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean enabled() {
		return config.getBoolean("enabled");
	}
	
	public String generateHandle() throws SQLException  {
		
		String id = HandleServiceImpl.generateUniqueHandleId();
		return handleServerPrefix +"/" + handleServerRepoId + id;
	}
	
	public String getId() {
		return id;
	}
	
	
	public HandleValue[] getDefautValues(String url) {
		HandleValue hvUrl = new HandleValue(1,Common.STD_TYPE_URL, url.getBytes());
		
		String mail = config.getString("email");
		mail = (mail == null) ? "info@edu-sharing.com" : mail;
		
		HandleValue hvMail = new HandleValue(2,Common.STD_TYPE_EMAIL, mail.getBytes());
		HandleValue hvAdmin = new HandleValue();
		hvAdmin.setIndex(100);
		hvAdmin.setType(Common.ADMIN_TYPE);
		hvAdmin.setData(Encoder.encodeAdminRecord(new AdminRecord(Util.encodeString(getId()), 200, 
                true, // addHandle
                true, // deleteHandle
                false, // addNA
                false, // deleteNA
                true, // readValue
                true, // modifyValue
                true, // removeValue
                true, // addValue
                true, // modifyAdmin
                true, // removeAdmin
                true, // addAdmin
                false  // listHandles
                )));
		
		return new HandleValue[] {hvUrl,hvMail,hvAdmin};
	}
	
	
	

	public static String HANDLE_TABLE = "EDU_HANDLE";
	
	public static String HANDLE_TABLE_ATT_ID = "ID";
	
	public static String HANDLE_QUERY_EXISTS = "select " + HANDLE_TABLE_ATT_ID + " from " + HANDLE_TABLE + " where lower(" + HANDLE_TABLE_ATT_ID +") = lower(?)";
	
	public static String HANDLE_INSERT = "insert into " + HANDLE_TABLE +" (" + HANDLE_TABLE_ATT_ID + ") VALUES (?)";
	
	public static int HANDLE_ID_LENGTH = 8;
	
	public static synchronized String generateUniqueHandleId() throws SQLException{
		String handleId = RandomStringUtils.randomAlphabetic(HANDLE_ID_LENGTH);
		
		while(handleIdExists(handleId)) {
			handleId = RandomStringUtils.randomAlphabetic(HANDLE_ID_LENGTH);
		}
		
		//handle server does not differentiate upper/lowercase
		handleId = handleId.toUpperCase();
		
		insertHandleId(handleId);
		return handleId;
	}
	
	private static boolean handleIdExists(String id) throws SQLException{
		try (SqlSession session = new ConnectionDBAlfresco().getSqlSessionFactoryBean().openSession()) {
			PreparedStatement statement = session.getConnection().prepareStatement(HANDLE_QUERY_EXISTS);
			id = StringEscapeUtils.escapeSql(id);
			statement.setString(1, id);
			java.sql.ResultSet resultSet = statement.executeQuery();
			if(resultSet.next()){
				return true;
			}else {
				return false;
			}
		}
	}
	
	private static void insertHandleId(String id) throws SQLException{
		try (SqlSession session = new ConnectionDBAlfresco().getSqlSessionFactoryBean().openSession()) {

			PreparedStatement statement = session.getConnection().prepareStatement(HANDLE_INSERT);
			id = StringEscapeUtils.escapeSql(id);
			statement.setString(1, id);

			statement.executeUpdate();
			statement.getConnection().commit();
		}
	}
}



