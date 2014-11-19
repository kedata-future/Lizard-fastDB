package com.lizard.fastdb.connection;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.transaction.TransactionManager;

/**
 * 获取JDBC数据库连接策略接口 <br>
 * <br>
 * 实现者需要实现这个接口，比如提供的：<tt>ProxoolConnectionProvider</tt><br>
 * <br>
 * fastDB 通过<tt>ConnectionProvider的实现类提供JDBC连接</tt> <br>
 * 注意：实现者需要提供一个默认的（不带参数的）构造方法
 * 
 * @see ConnectionProviderFactory
 * @author SHEN.GANG
 */
public interface ConnectionProvider extends Serializable
{
	/**
	 * 根据给定的数据源配置信息初始化数据库连接池
	 * 
	 * @param prop 数据源配置信息
	 */
	public void configure(Properties prop);

	/**
	 * 根据数据源名称获取数据库连接
	 * 
	 * @return 数据库连接
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException;

	/**
	 * 根据指定的数据源获得该数据源的事务Connection对象，并将其设置给指定的事务管理器管理<br>
	 * 如果你的数据源不使用事务操作，则该接口可以空实现。
	 * 
	 * @param tm 事务管理器
	 * @return 由XAConnection创建的Connection对象
	 * @throws SQLException
	 */
	public Connection getXAConnection(TransactionManager tm) throws SQLException;;

	/**
	 * 关闭数据库连接
	 * 
	 * @param conn 要关闭的连接
	 * @throws SQLException
	 */
	public void closeConnection(Connection conn) throws SQLException;

	/**
	 * 销毁数据源
	 * 
	 * @deprecated 由 {@link #shutdown()} 替代
	 */
	// @Deprecated
	// public void destroy();

	/**
	 * 彻底关闭销毁连接池，并释放里面的所有物理连接
	 */
	public void shutdown();

	/**
	 * 检查指定名称的数据源是否已经该连接池中注册
	 * 
	 * @deprecated 无用，废弃
	 * @param name 数据源名称
	 * @return true -- 已注册，false -- 未注册
	 */
	// @Deprecated
	// public boolean isDataSourceRegistered(String name);
}
