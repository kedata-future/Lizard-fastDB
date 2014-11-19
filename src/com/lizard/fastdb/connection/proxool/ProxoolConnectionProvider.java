package com.lizard.fastdb.connection.proxool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.enhydra.jdbc.standard.StandardXADataSource;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;

import com.lizard.fastdb.connection.ConnectionProvider;
import com.lizard.fastdb.datasource.DataSourceUtil;

/**
 * ConnectionProvider的 Proxool 实现类
 * 
 * @author SHEN.GANG
 */
public class ProxoolConnectionProvider implements ConnectionProvider
{
	private static final long	serialVersionUID		= 1L;
	private static final Log	logger					= LogFactory.getLog(ProxoolConnectionProvider.class);

	private Properties			prop					= null;												// 当前ConnectionProvider对应的数据源

	private String				name					= null;												// 当前数据源名称
	private int					connectionTimeOut		= 0;													// 设置驱动程序试图连接到某一数据库时将等待的最长时间，以秒为单位
	private int					acquireRetryAttempts	= 0;													// 重试次数
	private long				acquireRetryDelay		= 0;													// 每次重试的间隔时间

	/**
	 * 根据给定的数据源配置信息初始化数据库连接池
	 * 
	 * @param prop 数据源配置信息
	 */
	public void configure(Properties prop)
	{
		if (this.prop != null)
		{
			return;
		}
		this.prop = prop;

		this.name = this.prop.getProperty("name");
		this.connectionTimeOut = Integer.valueOf(this.prop.getProperty("connection-timeout"));
		this.acquireRetryAttempts = Integer.parseInt(this.prop.getProperty("acquire-retry-attempts"));
		this.acquireRetryDelay = Long.parseLong(this.prop.getProperty("acquire-retry-delay"));

		// 未注册
		if (!isDataSourceRegistered(name))
		{
			DriverManager.setLoginTimeout(connectionTimeOut);
			String url = "proxool." + name + ":" + this.prop.getProperty("driver-class") + ":" + this.prop.getProperty("driver-url");
			try
			{
				ProxoolFacade.registerConnectionPool(url, convertToProxoolProperties(this.prop));
				logger.info("Register proxool pool [" + name + "] success！");
			}
			catch (ProxoolException e)
			{
				logger.info("Fail to register proxool pool [" + name + "]！");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 根据数据源名称获取数据库连接
	 * 
	 * @return 数据库连接
	 * @throws SQLException
	 */
	@SuppressWarnings("static-access")
	public Connection getConnection()
	{
		int _acquireRetryAttempts = acquireRetryAttempts;
		long _acquireRetryDelay = acquireRetryDelay;

		Connection conn = null;
		boolean tryAgain = false;
		do
		{
			try
			{
				conn = DriverManager.getConnection("proxool." + prop.getProperty("name"));

				tryAgain = false;
				logger.debug("Successfully establish connectoin to DB");
			}
			catch (SQLException e)
			{
				// 如果重试次数大于0，则重试次数减1后，休眠一段时间后再次重试
				if (_acquireRetryAttempts-- > 0)
				{
					logger.error("Failed to aquire connection. Sleep for " + _acquireRetryDelay + " ms. Attempts left: " + _acquireRetryAttempts, e);
					try
					{
						Thread.currentThread().sleep(_acquireRetryDelay);
						tryAgain = true;
					}
					catch (InterruptedException e2)
					{
						tryAgain = false;
						logger.error("Thread occur InterruptedException, stop to aquire connection", e2);
					}
				}
				else
				{
					tryAgain = false;
				}
			}
		}
		while (tryAgain);

		return conn;
	}

	/**
	 * 根据指定的数据源获得该数据源的事务Connection对象，并将其设置给指定的事务管理器管理
	 * 
	 * @param tm 事务管理器
	 * @return 由XAConnection创建的Connection对象
	 * @throws SQLException
	 */
	public Connection getXAConnection(TransactionManager tm) throws SQLException
	{
		StandardXADataSource sxd = DataSourceUtil.convertPropertiesToXADataSource(prop);
		sxd.setTransactionManager(tm);

		Connection conn = sxd.getXAConnection().getConnection();
		conn.setAutoCommit(false);

		return conn;
	}

	/**
	 * 关闭数据库连接
	 * 
	 * @param conn 要关闭的连接
	 * @throws SQLException
	 */
	public void closeConnection(Connection conn) throws SQLException
	{
		if (conn != null)
		{
			conn.close();
		}
	}

	/**
	 * 销毁数据源
	 */
	private void destory()
	{
		// 已注册
		if (isDataSourceRegistered(name))
		{
			try
			{
				ProxoolFacade.removeConnectionPool(name);
				logger.info("Destroy proxool pool [" + name + "] success！");
			}
			catch (ProxoolException e)
			{
				logger.info("Fail to destroy proxool pool [" + name + "]！");
				e.printStackTrace();
			}
			this.prop = null;
			this.name = null;
		}
	}

	/**
	 * 关闭连接池
	 */
	public void shutdown()
	{
		// 销毁当前数据源连接池
		destory();

		// 如果当前没有其它Proxool数据源连接池存在的话，则调用 shutdown() 彻底销毁整个Proxool连接池（包括销毁守护线程等资源）
		String[] aliases = ProxoolFacade.getAliases();
		if (null != aliases && aliases.length == 0)
		{
			ProxoolFacade.shutdown();
		}
	}

	/**
	 * 检查指定名称的数据源是否已经注册
	 * 
	 * @param name 数据源名称
	 * @return true -- 已注册，false -- 未注册
	 */
	private boolean isDataSourceRegistered(String name)
	{
		// 获取系统已经注册的数据源信息
		String[] aliases = ProxoolFacade.getAliases();

		if (aliases == null || aliases.length == 0)
		{
			return false;
		}

		for (int i = 0; i < aliases.length; i++)
		{
			if (aliases[i] != null && name != null && aliases[i].equalsIgnoreCase(name))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * 将通用Properties转换为Proxool使用的Properties
	 * 
	 * @param ds 通用Properties
	 * @return Proxool转用Properties
	 */
	private Properties convertToProxoolProperties(Properties ds)
	{
		Properties prop = new Properties();

		// 以下属性不支持
		// init-connection-size
		// acquire-increment-size
		// max-connection-idletime

		@SuppressWarnings("rawtypes")
		Enumeration names = ds.propertyNames();
		while (names.hasMoreElements())
		{
			String name = (String) names.nextElement();
			String value = ds.getProperty(name);

			if ("user".equals(name) || "password".equals(name))
			{
				prop.setProperty(name, value);
			}
			else if ("driver-url".equals(name))
			{
				prop.setProperty("driverurl", value);
			}
			else if ("driver-class".equals(name))
			{
				prop.setProperty("driverclass", value);
			}
			else if ("max-connection-size".equals(name))
			{
				prop.setProperty("proxool.maximum-connection-count", value);
			}
			else if ("min-connection-size".equals(name))
			{
				prop.setProperty("proxool.minimum-connection-count", value);
			}
			else if ("available-connection-size".equals(name))
			{
				prop.setProperty("proxool.prototype-count", value);
			}
			else if ("max-connection-lifetime".equals(name))
			{
				prop.setProperty("proxool.maximum-connection-lifetime", String.valueOf(Integer.parseInt(value) * 1000));
			}
			else if ("test-connection-checkin".equals(name))
			{
				prop.setProperty("proxool.test-after-use", value);
			}
			else if ("test-connection-checkout".equals(name))
			{
				prop.setProperty("proxool.test-before-use", value);
			}
			else if ("test-sql".equals(name))
			{
				prop.setProperty("proxool.house-keeping-test-sql", value);
			}
			else
			{
				prop.setProperty("proxool." + name, value);
			}
		}

		return prop;
	}
}
