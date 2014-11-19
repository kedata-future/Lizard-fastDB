package com.lizard.fastdb.persistence;

import java.util.LinkedList;
import java.util.List;

public class Persistence
{
	// 数据库表名
	private String tableName;
		
	
	// 存放普通列容器
	private  List<ColumnField> normal_fields = null;
	
	public Persistence()
	{
		normal_fields = new LinkedList<ColumnField>();
	}
	
	/**
	 * 添加列字段
	 * 
	 * @param cf
	 */
	protected void addColumnField( ColumnField cf )
	{
		this.normal_fields.add( cf );
	}
	
	
	/**
	 * 获取列字段
	 * 
	 * @return 列字段
	 */
	public List<ColumnField> getColumnFields()
	{
		return this.normal_fields;
	}

	/**
	 * 获取数据库表名
	 * 
	 * @return
	 */
	public String getTableName()
	{
		return tableName;
	}

	protected void setTableName(String tableName)
	{
		this.tableName = tableName;
	}
	
	/**
	 * 清除存储信息
	 */
	protected void clear()
	{
		if( normal_fields != null )
		{
			normal_fields.clear();
			normal_fields = null;
		}
	}
}
