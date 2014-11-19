package com.lizard.fastdb.connection.bonecp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.enhydra.jdbc.standard.StandardXADataSource;

import com.lizard.fastdb.DBException;
import com.lizard.fastdb.connection.ConfigureReflect;
import com.lizard.fastdb.connection.ConnectionProvider;
import com.lizard.fastdb.datasource.DataSourceException;
import com.lizard.fastdb.datasource.DataSourceUtil;
import com.lizard.fastdb.dialect.Dialect;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

/**
 * 数据库连接策略 BoneCP 实现
 * 
 * @author SHEN.GANG
 */
public class BoneCPConnectionProvider implements ConnectionProvider
{
	private static final long	serialVersionUID	= 8860369586785396697L;
	private static final Log	LOG					= LogFactory.getLog(BoneCPConnectionProvider.class);

	/**
	 * 数据源配置
	 */
	private Properties			ds;
	/**
	 * 连接池对象
	 */
	private BoneCP				pool;

	public void configure(Properties prop)
	{
		// 如果已经存在，则直接返回
		if (pool != null)
		{
			return;
		}

		try
		{
			// 加载数据库连接驱动类
			Class.forName(prop.getProperty("driver-class"));

			// 初始化配置
			BoneCPConfig config = initConfig(prop);

			// 创建连接池
			pool = new BoneCP(config);

			LOG.info("BoneCP pool [" + prop.getProperty("name") + "] registered.");
		}
		catch (ClassNotFoundException e)
		{
			LOG.error("Fail to load jdbc driver: " + prop.getProperty("driver-class"), e);
		}
		catch (SQLException e)
		{
			LOG.error("Fail to register BoneCP pool [" + prop.getProperty("name") + "]", e);
		}
	}

	/**
	 * 初始化配置
	 * 
	 * @param prop
	 * @throws Exception
	 */
	private BoneCPConfig initConfig(Properties prop)
	{
		/*
		 * 以下 连接属性 BoneCP 暂不支持，但是不影响使用
		 * -- init-connection-size
		 * -- test-connection-checkin
		 * -- test-connection-checkout
		 */
		
		BoneCPConfig config = new BoneCPConfig();

		ds = prop;

		// 数据源名称
		ds.setProperty("poolName", prop.getProperty("name"));
		// 数据库连接URL
		ds.setProperty("jdbcUrl", prop.getProperty("driver-url"));
		// 连接用户名
		ds.setProperty("username", prop.getProperty("user"));
		// 连接密码(与 BoneCP 名称一致)
		ds.setProperty("password", prop.getProperty("password"));

		// 分区数
		int _partition = config.getPartitionCount();
		String prop_partition = prop.getProperty("partitionCount");
		if( null != prop_partition && prop_partition.trim().matches("^\\d+$"))
		{
			_partition = Math.max(_partition, Integer.parseInt(prop_partition.trim()));
		}
		// 最大连接数
		int maxConns = Integer.parseInt(prop.getProperty("max-connection-size"));
		if( _partition > maxConns )
		{
			throw new DataSourceException("Your BoneCP[partitionCount] attribute value["+_partition+"] is more than max-connection-size["+ maxConns +"]");
		}
		// 每个分区的最大连接数 = 总连接数 / 分区总数
		ds.setProperty("maxConnectionsPerPartition", String.valueOf( maxConns/_partition ));
		
		// 最小连接数
		int minConns = Integer.parseInt(prop.getProperty("min-connection-size"));
		// 每个分区的最小连接数
		ds.setProperty("minConnectionsPerPartition", String.valueOf( Math.min(minConns, maxConns/_partition)));
		
		// 始终维持的可用连接数(BoneCP 为百分比格式)
		int availableSize = Integer.parseInt(prop.getProperty("available-connection-size"));
		if (availableSize > 0 && maxConns > availableSize)
		{
			ds.setProperty("poolAvailabilityThreshold", String.valueOf(availableSize * 100 / maxConns));
		}

		// 连接的最大空闲时间
		ds.setProperty("idleMaxAgeInSeconds", ds.getProperty("max-connection-idletime"));
		// 数据库连接的最大生命周期(包含了活动和空闲)
		ds.setProperty("maxConnectionAgeInSeconds", ds.getProperty("max-connection-lifetime"));
		// 当没有连接可用时，同时创建的连接数
		ds.setProperty("acquireIncrement", ds.getProperty("acquire-increment-size"));
		// 连接超时时间
		ds.setProperty("connectionTimeoutInMs", String.valueOf(TimeUnit.SECONDS.toMillis(Long.parseLong(ds.getProperty("connection-timeout")))));

		// 重试次数
		ds.setProperty("acquireRetryAttempts", ds.getProperty("acquire-retry-attempts"));
		// 每次重试间隔时间
		ds.setProperty("acquireRetryDelayInMs", ds.getProperty("acquire-retry-delay"));


		// 测试连接是否有效的sql语句（该语句应该被很快的执行，否则会造成性能损失）
		// 如果不定义， BoneCP默认采用 connection.getMetaData().getTables() 进行测试
		// 如果定义为空（不是null），BoneCP会检测出错(因为 stmt.execute("") 报异常 )，而关闭这个连接，造成误关闭；
		// 因为BoneCP使用 if(connectionTestStatement == null){} 来判断是否执行用户定义的test sql，代码片段是：
		//  	String testStatement = this.config.getConnectionTestStatement();
	    //  	ResultSet rs = null;
	    //  	if (testStatement == null){
	    //    		rs = connection.getMetaData().getTables(null, null, "BONECPKEEPALIVE", METADATATABLE);
	    //  	} else {
	    //    		stmt = connection.createStatement();
	    //    		stmt.execute(testStatement);
	    //  	}
		// 而 datasource-default.properties 中 test-sql 默认值是空（非null），则就会造成上面的检查异常失败
		// 为了避免这样的错误，下面将针对BoneCP使用 MySQL: SELECT 1, Oracle: SELECT 1 FROM dual 作为默认值
		// 注：只有当配置了 max-connection-idletime 或 test-connection-test-period 时(值>0)，下面的connectionTestStatement才会被执行
		String testSql = ds.getProperty("test-sql");
		ds.setProperty("connectionTestStatement", (null == testSql || testSql.trim().length() == 0 ) ? Dialect.getDialect(prop.getProperty("driver-class")).getTestSQL() : testSql);
		
		// 空闲连接的检查周期（包括 检查空闲连接是否超期 和 连接是否可用-使用connectionTestStatement测试Connection的有效性 ）
		ds.setProperty("idleConnectionTestPeriodInSeconds", ds.getProperty("idle-connection-test-period"));

		// 初始化BoneCP属性
		try
		{
			ConfigureReflect.setProperties(config, ds);
		}
		catch (Exception e)
		{
			LOG.error("Fail to initialize BoneCPConfig", e);
			throw new DBException(e);
		}

		return config;
	}

	public Connection getConnection() throws SQLException
	{
		return pool.getConnection();
	}

	public Connection getXAConnection(TransactionManager tm) throws SQLException
	{
		StandardXADataSource sxd = DataSourceUtil.convertPropertiesToXADataSource(ds);
		sxd.setTransactionManager(tm);

		Connection conn = sxd.getXAConnection().getConnection();
		conn.setAutoCommit(false);

		return conn;
	}

	public void closeConnection(Connection conn) throws SQLException
	{
		if (conn != null && !conn.isClosed())
		{
			conn.close();
		}

	}

	public void shutdown()
	{
		if (pool != null)
		{
			pool.shutdown();
		}
	}

	// public synchronized void destory()
	// {
	// if (pool != null)
	// {
	// pool.shutdown();
	// pool = null;
	// }
	// }

	// public boolean isDataSourceRegistered(String name)
	// {
	// // (BoneCP 暂不支持)
	// return false;
	// }
}
