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
package org.edu_sharing.repository.server.importer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;

public class PersistentHandlerDB implements PersistentHandlerInterface{
	
	public void initTable() throws Exception{
		DBStuff dbStuff = new DBStuff();
		ResultSet rs = dbStuff.dbStatement("drop table if exists TEST_TABLE");
		dbStuff.cleanUp();
	}
	
	public String safe(RecordHandlerInterfaceBase recordHandler, String cursor, String set) throws Throwable{
		
		Map<String,String> tableStruct = this.getTableStructure(null, recordHandler.getProperties(), null);
			
		String checkTableExistsSql = "show tables";
		
		DBStuff dbStuff = new DBStuff();
		ResultSet rs = dbStuff.dbStatement(checkTableExistsSql);
		if(rs == null || !rs.next()){
			String createtableStatement = "create table TEST_TABLE (";
			
			int count = 0;
			for(String colName:tableStruct.keySet()){
				if(tableStruct.keySet().size() > 1){
					if(count < (tableStruct.keySet().size() -1)){
						createtableStatement += colName +" VARCHAR(255), ";
					}else{
						createtableStatement += colName +" VARCHAR(255))";
					}
				}else{
					createtableStatement += colName +" VARCHAR(255))";
				}
				
				count++;
			}
			DBStuff dBStuffCreate = new DBStuff();
			System.out.println(createtableStatement);
			dBStuffCreate.dbStatement(createtableStatement);
			dBStuffCreate.cleanUp();
		}
		dbStuff.cleanUp();
		
		int count = 0;
		String insert = "INSERT INTO TEST_TABLE";
		String cols ="";
		String vals ="";
		for(Map.Entry<String,String> entry: tableStruct.entrySet()){
			
			//check if column excist
			String checkColSQL = "describe TEST_TABLE";
			
			DBStuff dbStuffCheckColsSQL = new DBStuff();
			ResultSet rsCCSQL = dbStuffCheckColsSQL.dbStatement(checkColSQL);
			boolean colExists = false;
			while(rsCCSQL != null && rsCCSQL.next()){
				String colname = rsCCSQL.getString("Field");
				if(colname.equals(entry.getKey())){
					colExists = true;
				}
			}
			dbStuffCheckColsSQL.cleanUp();
			
			if(!colExists){
				String addColSQL = "alter table TEST_TABLE ADD COLUMN ("+ entry.getKey() +" VARCHAR(255))";
				System.out.println("addColSQL:"+addColSQL);
				DBStuff dbStuffAddColSQL = new DBStuff();
				dbStuffAddColSQL.dbStatement(addColSQL);
				dbStuffAddColSQL.cleanUp();
			}
			
			if(count < (tableStruct.size() - 1)){
				cols+=entry.getKey()+", ";
				vals+="'"+entry.getValue()+"', ";
			}else{
				cols+=entry.getKey();
				vals+="'"+entry.getValue()+"'";
			}
			count++;
		}
		
		insert +="("+cols+") VALUES(" +vals+")";
		DBStuff dbStuffInsert = new DBStuff();
		dbStuffInsert.dbStatement(insert);
		dbStuffInsert.cleanUp();
		
		return "";
	}

	class DBStuff{
		
		Connection con = null;
		Statement statement = null;
		public ResultSet dbStatement(String sql) throws Exception{
			
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection("jdbc:mysql://localhost/testdb","root","");
			con.setAutoCommit(true);
			statement = con.createStatement();
			statement.execute(sql);
			return statement.getResultSet();
		}
		
		public void cleanUp() throws Exception{
			if(statement != null){
				statement.close();
			}
			if(con != null){
				con.close();
			}
		}
	}
	
	Map<String,String>  getTableStructure(Map<String,String> tablestructure, Map<String, Object> props, String colNamePrefix){
		if(tablestructure == null){
			tablestructure  = new HashMap<>();
		}
		
		Map<String,Object> simpleProps = new HashMap<>();
		Map<String,Object> nodeProps = new HashMap<>();
		for(Object key:props.keySet()){
			String propKey = (String)key;
			if(propKey.startsWith("TYPE#")){
				nodeProps.put(propKey, props.get(propKey));
			}else{
				simpleProps.put(propKey, props.get(propKey));
			}
		}
		
		for(String key:simpleProps.keySet()){
			if(key == null || key.equals("")) continue;
			QName qName = QName.createQName(key);
			String colName = (colNamePrefix != null) ? colNamePrefix+qName.getLocalName() : qName.getLocalName();
			if(tablestructure.keySet().contains(colName)){
				colName = colName + Math.abs(key.hashCode());
			}
			Object tmpValue = simpleProps.get(key);
			String theVal = getValue(tmpValue);
			
			if(theVal != null){
				if(theVal.length() > 255) theVal = theVal.substring(0, 250)+"...";
				theVal = theVal.replace("'", "\\'");
				
				tablestructure.put(colName, theVal );
			}else{
				System.out.println("value for"+key +"is null");
			}
		}
		
		for(String key:nodeProps.keySet()){
			String typeName = key.split("#")[1];
			QName qName = QName.createQName(typeName);
			String subColNamePrefix = qName.getLocalName();
			
			Object nodePropsVal = nodeProps.get(key);
			if(nodePropsVal instanceof ArrayList){
				List l = (ArrayList) nodePropsVal;
				for(Object o:l){
					Map subProps = (Map)o;
					getTableStructure(tablestructure, subProps, subColNamePrefix);
				}
			}else{
				Map subProps = (Map)nodeProps.get(key);
				getTableStructure(tablestructure, subProps, subColNamePrefix);
			}
			
		}
		
		return tablestructure;
	}
	
	
	protected String getValue(Object _value){
		
		if (_value instanceof List && ((List)_value).size() > 0) {
			String result = null;
			for(Object value : (List)_value){
				if(result != null) result += "[#]";
				if(value instanceof HashMap){
					Map tmp = (Map)value;
					for(Object tmpKey : tmp.keySet()){
						 result += tmpKey.toString()+"="+tmp.get(tmpKey);
					}
				}else{	
					if(result != null) result += value.toString();
					else result = value.toString();
				}
			}
			return result;
			
		}else if(_value instanceof List && ((List)_value).size() == 0){
			//cause empty list toString returns "[]"
			return "";
		}else if(_value instanceof String){
			return (String) _value;
		}else if(_value instanceof Number){
			return _value.toString();
		}else if(_value != null){
			return _value.toString();
		}
		
		return null;
	}
	
	@Override
	public boolean mustBePersisted(String replId, String timeStamp) {
		return true;
	}
	
	@Override
	public boolean exists(String replId) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
