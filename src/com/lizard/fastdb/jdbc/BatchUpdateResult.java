package com.lizard.fastdb.jdbc;

import java.io.Serializable;

/**
 * JDBC Handler 中 batchUpate 执行返回结果信息
 * 
 * @author SHEN.GANG
 */
public class BatchUpdateResult implements Serializable
{
	private static final long	serialVersionUID	= 6819342264807272756L;
	
	// 是否全部执行成功
	private boolean	success		= false;
	// 存储失败的分批批次信息
	private int[]	errorBatchs	= new int[0];
	// 成功影响的计数
	private int[]	batchRows	= null;

	public BatchUpdateResult()
	{
	}

	public BatchUpdateResult(boolean success, int[] errorBatchs, int[] batchRows)
	{
		super();
		this.success = success;
		this.errorBatchs = errorBatchs;
		this.batchRows = batchRows;
	}

	/**
	 * 批量更新结果
	 * 
	 * @return true -- 全部成功，false -- 失败
	 */
	public boolean isSuccess()
	{
		return success;
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	/**
	 * 获得成功执行的SQL语句的更新计数
	 * 
	 * @return 计数结果数组
	 */
	public int[] getBatchRows()
	{
		return batchRows;
	}

	public void setBatchRows(int[] batchRows)
	{
		this.batchRows = batchRows;
	}

	/**
	 * 获得失败的批次编号，批次编号从1开始
	 * 
	 * @return 失败的批次编号数组
	 */
	public int[] getErrorBatchs()
	{
		return errorBatchs;
	}

	public void setErrorBatchs(int[] errorBatchs)
	{
		this.errorBatchs = errorBatchs;
	}
}
