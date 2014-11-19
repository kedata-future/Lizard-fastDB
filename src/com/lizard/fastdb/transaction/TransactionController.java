package com.lizard.fastdb.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lizard.fastdb.util.StringUtils;


/**
 * 单数据源事务控制容器
 * 
 * @author  SHEN.GANG
 */
public class TransactionController
{
	private final static Log logger = LogFactory.getLog( TransactionController.class );
	
	/** 事务 */
	private final static ThreadLocal<Transaction> LOCAL_TRANS = new ThreadLocal<Transaction>();
	
	/** 事务所用数据库连接对象 */
	private final static ThreadLocal<Connection> LOCAL_CONNECTION = new ThreadLocal<Connection>();
	
	/** 事务所用数据源 */
	public final static ThreadLocal<String> LOCAL_DSNAME = new ThreadLocal<String>();
	
	/**
	 * 开启事务
	 * 
	 * @throws TransactionException 
	 */
	public static void  beginTransaction() throws TransactionException
	{
		// 判断事务嵌套模式，多数据事务模式内部使用单数据源事务，自动切换为多数据源事务模式
		if(Transaction.getTransMode() == TransactionConstant.TRANS_MODE_MULTIDATASOURCE)
		{
			MultiTransactionController.beginTransaction();
			return ;
		}
		
		Transaction _trans= LOCAL_TRANS.get();
		try
		{
			// 判断此事务是否属于全局事务
			if( _trans == null )
			{
				_trans = new Transaction();
				Transaction.setTransMode(TransactionConstant.TRANS_MODE_SINGLEDATASOURCE);
				LOCAL_TRANS.set( _trans );
			}else 
			{
				// 事务已经开启，将事务深度+1， 事务次数+1
				_trans.setTransCount(_trans.getTransCount()+1);
				_trans.setTransDeep(_trans.getTransDeep()+1);
			}
		}catch (Exception e)
		{
			logger.error("开启事务失败！");
			throw new TransactionException("Error to start transaction!", e);
		}
	}
	
	/**
	 * 提交事务
	 * 
	 * @throws TransactionException 
	 */
	public static void commitTransaction() throws TransactionException
	{
		// 判断事务嵌套模式，多数据事务模式内部使用单数据源事务，自动切换为多数据源事务模式
		if(Transaction.getTransMode() == TransactionConstant.TRANS_MODE_MULTIDATASOURCE)
		{
			MultiTransactionController.commitTransaction();
			return ;
		}
		
		Transaction _trans = LOCAL_TRANS.get();

		// 将提交次数+1
		_trans.setCommitCount(_trans.getCommitCount()+1); 
		Connection conn = LOCAL_CONNECTION.get();
		
		try {
			// 事务全部正确执行，提交数据
			if( _trans.hasFullExecute()) 
			{
				if(conn != null)
				{
					conn.commit();
					logger.info("单数据源事务管理器提交数据，数据源【"+LOCAL_DSNAME.get()+"】...");
				}
			}
			
		} catch (Exception e) {
			try {
				// 提交数据失败，回滚数据
				conn.rollback(); 
				logger.error("提交事务失败！");
				
				throw new TransactionException("Error to commit transaction!", e);
			} catch (Exception e1) {
				logger.error("回滚事务失败！");
				throw new TransactionException("Error to rollback transaction!",e1);
			}
		}
	}
	
	/**
	 * 关闭事务
	 * 
	 * @throws TransactionException 
	 */
	public static void closeTransaction() throws TransactionException
	{
		// 判断事务嵌套模式，多数据事务模式内部使用单数据源事务，自动切换为多数据源事务模式
		if(Transaction.getTransMode() == TransactionConstant.TRANS_MODE_MULTIDATASOURCE)
		{
			MultiTransactionController.closeTransaction();
			return ;
		}
		
		Transaction _trans = LOCAL_TRANS.get();
		
		// 如果事务深度>1,则是嵌套事务,不关闭连接，直接将事务深度-1
		if( _trans.getTransDeep()>1 )
		{
			_trans.setTransDeep(_trans.getTransDeep()-1);
			
			return;
		}
		
		Connection conn = LOCAL_CONNECTION.get();
		String ds_name = LOCAL_DSNAME.get();
		
		// 全局事务已经提交，清空ThreadLocal变量
		LOCAL_TRANS.set( null );
		LOCAL_CONNECTION.set(null);
		LOCAL_DSNAME.set(null);
		Transaction.setTransMode(null);
		
		try
		{
			// 事务未全部正确执行，回滚数据
			if( !_trans.hasFullExecute()) 
			{
				if(conn != null)
				{
					conn.rollback();
					logger.info("单数据源事务管理器回滚数据，数据源【"+ds_name+"】");
				}
			}
			
			// 释放数据库连接
			if(conn!=null && !conn.isClosed())
			{
				conn.close();
			}
			conn = null;
			_trans = null;
			
		}
		catch (Exception e)
		{
			logger.error("关闭事务失败！");
			throw new TransactionException("Error to close transaction!", e);
		}
	}
	
	/**
	 * 加载某数据源下的connection对象
	 * 
	 * @param ds_name	数据源名称
	 * @param conn		数据库连接对象
	 */
	public static void load(String ds_name, Connection conn)
	{
		if( !StringUtils.isEmpty( ds_name ) && conn != null )
		{
			try {
				conn.setAutoCommit(false);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			LOCAL_CONNECTION.set( conn );
			LOCAL_DSNAME.set( ds_name );
		}
	}
	
	/**
	 * 事务管理器是否已经控制某数据源
	 * 
	 * @param ds_name	数据源名称
	 * @return			true 已经控制、false 没有控制
	 */
	public static boolean contains(String ds_name)
	{
		if( !StringUtils.isEmpty( ds_name ))
		{
			return ds_name.equals( LOCAL_DSNAME.get() );
		}
		return false ;
	}
	
	/**
	 * 获得事务管理器控制的数据库连接对象
	 * 
	 * @return 当前线程中保存的数据库连接
	 */
	public static Connection getConnection()
	{
		return LOCAL_CONNECTION.get();
	}
	
	/**
	 * 判读事务管理器是否已经被启用
	 * 
	 * @return true -- 已经被使用， false -- 未被使用
	 */
	public static boolean isUsed()
	{
		return !StringUtils.isEmpty( LOCAL_DSNAME.get() ) && ( LOCAL_CONNECTION != null );
	}
	
}