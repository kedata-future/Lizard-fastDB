package com.lizard.fastdb.transaction;


/**
 * 记录当前事务的状态
 *
 * @author  SHEN.GANG
 */
public class Transaction
{
	/** 事务总次数 */
	private int transCount = 0;
	
	/** 事务提交次数 */
	private int commitCount = 0;
	
	/** 事务嵌套层次 */
	private int transDeep = 0;

	/** 事务模式 */
	private final static ThreadLocal<Integer> trans_mode = new ThreadLocal<Integer>();
	
	public Transaction()
	{
		transCount = 1;
		transDeep = 1;
	}
	/**
	 * 判断事务是否完全正确执行<br>
	 * 通过提交次数和事务次数来判断<br>
	 * @return  true -- 完全执行， false -- 未完全执行
	 */
	public boolean hasFullExecute()
	{
		return  transCount == commitCount ;
	}
	
	public int getTransCount() {
		return transCount;
	}
	
	public int getCommitCount() {
		return commitCount;
	}
	
	public int getTransDeep() {
		return transDeep;
	}
	
	public void setTransCount(int transCount) {
		this.transCount = transCount;
	}
	
	public void setCommitCount(int commitCount) {
		this.commitCount = commitCount;
	}
	
	public void setTransDeep(int transDeep) {
		this.transDeep = transDeep;
	}
	
	/**
	 * 获得事务模式
	 * 
	 * @return 事务模式类型
	 */
	public static int getTransMode()
	{
		Integer model = trans_mode.get();
		if(model != null)
		{
			return model.intValue();
		}
		return TransactionConstant.TRANS_MODE_NOTRANSACTION;
	}
	
	/**
	 * 设置事务模式
	 * 
	 * @param mode  事务模式
	 */
	public static void setTransMode(Integer mode)
	{
		trans_mode.set(mode);
	}

}
