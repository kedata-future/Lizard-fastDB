package com.lizard.fastdb.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.jotm.Jotm;
import org.objectweb.jotm.TimerManager;
import org.objectweb.transaction.jta.TMService;

import com.lizard.fastdb.util.StringUtils;

/**
 * 多数据源事务控制容器
 * 
 * @author  SHEN.GANG
 */
public class MultiTransactionController
{
	private final static Log logger = LogFactory.getLog( MultiTransactionController.class );
	
	/** 事务 */
	private final static ThreadLocal<Transaction> LOCAL_TRANS = new ThreadLocal<Transaction>();
	
	/** 事务所用数据库操作对象池 */
	private final static ThreadLocal<Map<String,Connection>> LOCAL_CONNECTIONPOOL = new ThreadLocal<Map<String,Connection>>();
	
	/** 负责事务数据操作提交、回滚的管理器 */
	public static TransactionManager transMgr ;
	
	/** 事务助手 */
	private static TMService transMgr_service;
	
	/**
	 * 开启事务
	 * 
	 * @throws TransactionException 
	 */
	public static void  beginTransaction() throws TransactionException
	{
		// 判断事务嵌套模式，如果在单数据源事务内部使用了多数据源事务，则抛出异常
		if(Transaction.getTransMode() == TransactionConstant.TRANS_MODE_SINGLEDATASOURCE)
		{
			throw new TransactionException("单数据源事务模式内部不能使用多数据源事务...");
		}
		
		Transaction _trans= LOCAL_TRANS.get();
		try
		{
			// 判断此事务是否属于全局事务
			if( _trans == null )
			{
				if(transMgr == null)
				{
					// 开启事务助手服务
					transMgr_service = new Jotm(true,false);
					transMgr = transMgr_service.getTransactionManager();
				}
				transMgr.begin();
				_trans = new Transaction();
				Transaction.setTransMode(TransactionConstant.TRANS_MODE_MULTIDATASOURCE);
				
				// 全局事务所用数据库操作对象池
				LOCAL_TRANS.set( _trans );
				LOCAL_CONNECTIONPOOL.set(new HashMap<String,Connection>());
				
			} else {
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
		// 判断事务嵌套模式，如果在单数据源事务内部使用了多数据源事务，则直接退出
		if(Transaction.getTransMode() == TransactionConstant.TRANS_MODE_SINGLEDATASOURCE)
		{
			return;
		}
		
		Transaction _trans = LOCAL_TRANS.get();
		
		// 提交次数+1
		_trans.setCommitCount( _trans.getCommitCount()+1 ); 
		
		try {
			// 事务全部正确执行，提交数据
			if( _trans.hasFullExecute()) 
			{
				transMgr.commit();
				logger.info("多数据源事务管理器提交数据...");
			}
			
		} catch (Exception e) {
			try {
				transMgr.rollback();
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
		// 判断事务嵌套模式，如果在单数据源事务内部使用了多数据源事务，则直接退出
		if(Transaction.getTransMode() == TransactionConstant.TRANS_MODE_SINGLEDATASOURCE)
		{
			return;
		}
		
		Transaction _trans = LOCAL_TRANS.get();
		
		// 如果事务深度>1,则是嵌套事务,不关闭连接，直接将事务深度-1
		if( _trans.getTransDeep()>1 )
		{
			_trans.setTransDeep(_trans.getTransDeep()-1);
			return;
		}
		
		Map<String,Connection> connPool = LOCAL_CONNECTIONPOOL.get();
		
		// 全局事务已经提交，清空ThreadLocal变量
		LOCAL_TRANS.set( null );
		LOCAL_CONNECTIONPOOL.set(null);
		Transaction.setTransMode(null);
		
		try
		{
			// 事务未全部正确执行，回滚数据
			if( !_trans.hasFullExecute()) 
			{
				transMgr.rollback();
				logger.info("多数据源事务管理器回滚数据...");
			}
			
			// 关闭事务助手服务
			if(transMgr_service != null) 
			{
				transMgr_service.stop();
				TimerManager.stop();
				transMgr_service = null;
			}
			
			// 释放所有数据库连接
			//for(String ds_name:connPool.keySet())
			for( Map.Entry<String,Connection> e : connPool.entrySet() )
			{
				Connection conn = e.getValue(); //connPool.get(ds_name);
				if(conn != null && !conn.isClosed())
				{
					conn.close();
				}
				conn = null;
			}
			
			connPool = null;
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
				conn.setAutoCommit( false );
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			MultiTransactionController.LOCAL_CONNECTIONPOOL.get().put(ds_name, conn);
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
		return MultiTransactionController.LOCAL_CONNECTIONPOOL.get().containsKey(ds_name);
	}
	
	/**
	 * 获得事务管理器控制的某数据源下的数据库连接对象
	 * 
	 * @param ds_name	数据源名称
	 * @return 数据连接
	 */
	public static Connection getConnection(String ds_name)
	{
		return MultiTransactionController.LOCAL_CONNECTIONPOOL.get().get(ds_name);
	}
}