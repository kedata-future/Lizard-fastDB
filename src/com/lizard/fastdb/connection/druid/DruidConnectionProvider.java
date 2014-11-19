package com.lizard.fastdb.connection.druid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.enhydra.jdbc.standard.StandardXADataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.lizard.fastdb.connection.ConnectionProvider;
import com.lizard.fastdb.datasource.DataSourceUtil;

/**
 * ConnectionProvicer 的  Druid 实现
 * 
 * @author SHEN.GANG
 */
public class DruidConnectionProvider implements ConnectionProvider {

	private static final long		serialVersionUID	= 1L;
	private static final Log		logger				= LogFactory.getLog(DruidConnectionProvider.class);
	
	private Properties				prop 				= null;
	
	private static  DruidDataSource  		ds 			= new DruidDataSource();  				
	
	@Override
	public void configure(Properties prop) {
		
		if (this.prop != null) return;
		
		this.prop = prop;
		
		try {
			// 转换prop属性
			convertProp();
			DruidDataSourceFactory.config(ds, prop);
			
			logger.info("Druid pool [" + prop.getProperty("name") + "] registered.");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			logger.error("Fail to register Druid pool [" + prop.getProperty("name") + "]", e);
		}
	}

	@Override
	public Connection getConnection() throws SQLException 
	{
		return ds.getConnection();
	}	

	@Override
	public Connection getXAConnection(TransactionManager tm) throws SQLException {
		StandardXADataSource sxd = DataSourceUtil.convertPropertiesToXADataSource(prop);
		sxd.setTransactionManager(tm);

		Connection conn = sxd.getXAConnection().getConnection();
		conn.setAutoCommit(false);

		return conn;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		if (conn != null && !conn.isClosed() )
		{
			conn.close();
		}
	}

	@Override
	public void shutdown() {
		try {
			ds.close();
		} catch( Exception e ) {
			e.printStackTrace();
			logger.error("Failed to close Druid pool.");
		}
	}

	/**
	 * 设置属性映射，用于通过方法反射设置属性
	 */
	private void convertProp()
	{
		// 参考 ：https://github.com/alibaba/druid/wiki/DruidDataSource%E9%85%8D%E7%BD%AE%E5%B1%9E%E6%80%A7%E5%88%97%E8%A1%A8
		this.prop.setProperty("driverClassName", this.prop.getProperty("driver-class"));
		this.prop.setProperty("name", this.prop.getProperty("name"));
		this.prop.setProperty("url", this.prop.getProperty("driver-url"));
		this.prop.setProperty("username", this.prop.getProperty("user"));
		this.prop.setProperty("password", this.prop.getProperty("password"));
		
		this.prop.setProperty("maxActive", this.prop.getProperty("max-connection-size"));
		this.prop.setProperty("minIdle", this.prop.getProperty("min-connection-size"));
		this.prop.setProperty("initialSize", this.prop.getProperty("init-connection-size"));
		this.prop.setProperty("maxWait", this.prop.getProperty("max-connection-idletime"));
//		this.prop.setProperty("validationQuery", null);
		this.prop.setProperty("filters", "stat");
	}
}
