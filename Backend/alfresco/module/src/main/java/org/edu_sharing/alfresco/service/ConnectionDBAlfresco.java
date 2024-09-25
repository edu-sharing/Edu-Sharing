package org.edu_sharing.alfresco.service;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionDBAlfresco {
	
	Logger logger = Logger.getLogger(ConnectionDBAlfresco.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	SqlSessionFactory sqlSessionFactoryBean = (SqlSessionFactory)applicationContext.getBean("repoSqlSessionFactory");

	public SqlSessionFactory getSqlSessionFactoryBean() {
		return sqlSessionFactoryBean;
	}

	public Connection getConnection() {
		return sqlSessionFactoryBean.openSession().getConnection();
	}
	
	public void cleanUp(Connection con) {
		this._cleanUp(con, null, null);
	}
	
	public void cleanUp(Connection con, Statement s) {
		this._cleanUp(con, s, null);
	}
	
	private void _cleanUp(Connection con, Statement s, ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		}
		catch (SQLException sqle) {
			logger.error(sqle);
		}

		try {
			if (s != null) {
				s.close();
			}
		}
		catch (SQLException sqle) {
			logger.error(sqle);
		}

		try {
			if (con != null) {
				con.close();
			}
		}
		catch (SQLException sqle) {
			logger.error(sqle);
		}
	}
}
