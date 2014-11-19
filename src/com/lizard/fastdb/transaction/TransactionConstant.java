package com.lizard.fastdb.transaction;

/**
 * 事务常量类
 * 
 * @author  SHEN.GANG
 */
public class TransactionConstant {
	
	/** 单数据源模式 */
	public final static int TRANS_MODE_SINGLEDATASOURCE = 0;
	
	/** 多数据源模式 */
	public final static int TRANS_MODE_MULTIDATASOURCE = 1;
	
	/** 无事务模式 */
	public final static int TRANS_MODE_NOTRANSACTION = -1;
	
}
