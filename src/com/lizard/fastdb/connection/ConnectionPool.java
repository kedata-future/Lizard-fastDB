package com.lizard.fastdb.connection;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lizard.fastdb.datasource.DataSourceCache;
import com.lizard.fastdb.transaction.MultiTransactionController;
import com.lizard.fastdb.transaction.Transaction;
import com.lizard.fastdb.transaction.TransactionConstant;
import com.lizard.fastdb.transaction.TransactionController;

/**
 * 连接池管理器，用于从连接池获得和关闭连接，同时可用于获得和关闭非连接池中的连接
 * 
 * @author SHEN.GANG
 */
public class ConnectionPool
{
	private static final Log	logger	= LogFactory.getLog(ConnectionPool.class);

	/**
	 * 获得依赖于数据库连接池的连接
	 * 
	 * @param ds_name 数据源名称
	 * @return 当前数据源的数据库连接
	 */
	public static Connection getConnection(String ds_name)
	{
		// 确保ds_name是真实数据源名称
		Connection conn = null;
		ConnectionProvider connP = DataSourceCache.getConnectionProvider(ds_name);

		// 事务模式
		int trans_model = Transaction.getTransMode();

		// 无事务模式
		if (trans_model == TransactionConstant.TRANS_MODE_NOTRANSACTION)
		{
			try
			{
				conn = connP.getConnection();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		// 单数据源事务模式
		else if (trans_model == TransactionConstant.TRANS_MODE_SINGLEDATASOURCE)
		{
			// 单数据源事务管理器已经加载某数据源
			if (TransactionController.contains(ds_name))
			{
				conn = TransactionController.getConnection();
			}
			else
			{
				try
				{
					conn = connP.getConnection();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}

				// 单数据源事务第一次启动，没有数据源被加载
				if (!TransactionController.isUsed())
				{
					TransactionController.load(ds_name, conn);
				}
				else
				{
					logger.info("单数据源事务管理器[TransactionController]已经加载数据源[" + TransactionController.LOCAL_DSNAME.get() + "]，" + "不会对数据源[" + ds_name
							+ "]进行事务管理，如果需要在事务中使用多个数据源，请使用多数据源事务管理器[MultiTransactionController]！");
				}
			}
		}
		// 多数据源事务模式
		else if (trans_model == TransactionConstant.TRANS_MODE_MULTIDATASOURCE)
		{
			// 多数据源事务管理器已经加载某数据源
			if (MultiTransactionController.contains(ds_name))
			{
				conn = MultiTransactionController.getConnection(ds_name);
			}
			else
			{
				try
				{
					conn = connP.getXAConnection(MultiTransactionController.transMgr);
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				MultiTransactionController.load(ds_name, conn);
			}
		}

		return conn;
	}

	/**
	 * 直接从数据源获得连接，与事务无关
	 * 
	 * @param ds_name 数据源名称
	 * @return 数据库连接
	 */
	public static Connection getIndependConnection(String ds_name)
	{
		Connection conn = null;
		try
		{
			conn = DataSourceCache.getConnectionProvider(ds_name).getConnection();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return conn;
	}

	/**
	 * 关闭数据库连接池中的连接
	 * 
	 * @param name 数据源名称
	 * @param conn 数据库连接
	 */
	public static void closeConnection(String name, Connection conn)
	{
		if (Transaction.getTransMode() == TransactionConstant.TRANS_MODE_NOTRANSACTION)
		{
			try
			{
				DataSourceCache.getConnectionProvider(name).closeConnection(conn);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * 关闭一个数据库连接，该连接于事务无关
	 * 
	 * @param ds_name 数据源名称
	 * @param conn 数据库连接
	 */
	public static void closeIndependConnection(String ds_name, Connection conn)
	{
		if (conn != null)
		{
			try
			{
				DataSourceCache.getConnectionProvider(ds_name).closeConnection(conn);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			conn = null;
		}
	}

	/**
	 * 判断当前是否处于事务中
	 * 
	 * @return true 处于事务中，false 处于非事务中
	 */
	public static boolean isInTransaction()
	{
		return Transaction.getTransMode() != TransactionConstant.TRANS_MODE_NOTRANSACTION;
	}
}
