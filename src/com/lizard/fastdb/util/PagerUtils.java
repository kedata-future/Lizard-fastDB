package com.lizard.fastdb.util;

import java.io.Serializable;


/**
 * 分页操作类
 * 
 * @author SHEN.GANG
 */
@SuppressWarnings("serial")
public class PagerUtils implements Serializable
{
	// 页码，默认为1
	private int pageNo = 1;
	// 页面显示的数据量
	private int pageSize = 20;
	// 数据总量
	private int totalRows = 0;
	// 总页数
	private int totalPages = 0;
	
	// 数据库游标位置
	private int startIndex = 0;
	private int endIndex = 0;
	
	// 显示第 from - to 条数据
	private int from = 0;
	private int to = 0;
	
	// 排序字段名称
	private String sortname;
	// 排序顺序
	private String sortorder;
	
	public PagerUtils(){}
	
	public PagerUtils( int pageSize, int totalRows )
	{
		this.pageSize = pageSize;
		this.totalRows = totalRows;
	}
	
	public PagerUtils( String pageNo, String pageSize )
	{
		if( !StringUtils.isEmptyString( pageNo ) )
		{
			this.pageNo = Integer.valueOf( pageNo );
		}
		if( !StringUtils.isEmptyString( pageSize ) )
		{
			this.pageSize = Integer.valueOf( pageSize );
		}
	}
	
	public PagerUtils( String pageNo, String pageSize, int totalRows )
	{
		if( !StringUtils.isEmptyString( pageNo ) )
		{
			this.pageNo = Integer.valueOf( pageNo );
		}
		if( !StringUtils.isEmptyString( pageSize ) )
		{
			this.pageSize = Integer.valueOf( pageSize );
		}
		
		this.totalRows = totalRows;
	}
	
	public PagerUtils( int pageNo, int pageSize, int totalRows )
	{
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.totalRows = totalRows;
	}
	
	public PagerUtils( int pageNo, int pageSize, int totalRows, String sortname, String sortorder )
	{
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.totalRows = totalRows;
		this.sortname = sortname;
		this.sortorder = sortorder;
	}

	/**
	 * 获取页码
	 * 
	 * @return 页码
	 */
	public int getPageNo()
	{
		// 如果没有数据，则返回0
		if( this.getTotalRows() <= 0 )
		{
			return 1;
		}
		
		return this.pageNo;
	}

	/**
	 * 设置页码
	 * 
	 * @param pageNo 页码
	 */
	public void setPageNo( int pageNo )
	{
		this.pageNo = pageNo;
	}

	/**
	 * 获取页码显示的数据量大小
	 * 
	 * @return 页码显示的数据量大小
	 */
	public int getPageSize()
	{
		return this.pageSize;
	}

	/**
	 * 设置页码显示的数据量大小
	 * 
	 * @param pageSize 页码显示的数据量大小
	 */
	public void setPageSize( int pageSize )
	{
		this.pageSize = pageSize;
	}

	/**
	 * 获取数据总量
	 * 
	 * @return 数据总量
	 */
	public int getTotalRows()
	{
		return this.totalRows;
	}

	/**
	 * 设置数据总量
	 * 
	 * @param totalRows 数据总量
	 */
	public void setTotalRows( int totalRows )
	{
		this.totalRows = totalRows;
	}

	/**
	 * 获取总的分页数量
	 * 
	 * @return 总的分页数量
	 */
	public int getTotalPages()
	{
		if( getTotalRows() <= 0 || getPageSize() <= 0 )
		{
			return 0;
		}
		
		this.totalPages = (getTotalRows() % getPageSize()) == 0
					 ?(getTotalRows() / getPageSize())
					 :(getTotalRows() / getPageSize())+1;
		
		return this.totalPages;
	}

	/**
	 * 设置分页数量
	 * 
	 * @param totalPages 分页数量
	 */
	/*public void setTotalPages( int totalPages )
	{
		this.totalPages = totalPages;
	}*/

	/**
	 * 获取当前页码下显示数据的开始值
	 * 
	 * @return 当前页码下显示数据的开始值
	 */
	public int getFrom()
	{
		this.from = (getPageNo() - 1) * getPageSize() + 1;
		
		if( this.from <= 0 )
		{
			this.from = 0;
		}
		
		return this.from;
	}

	/**
	 * 获取当前页码下显示数据的结束值
	 * 
	 * @return 当前页码下显示数据的结束值
	 */
	public int getTo()
	{
		this.to = getPageNo() * getPageSize();
		if( this.to > getTotalRows() )
		{
			this.to = getTotalRows();
		}
		
		if( this.to <= 0 )
		{
			this.to = 0;
		}
		
		return this.to;
	}

	/**
	 * 获取当前页码下游标的开始位置
	 * 
	 * @return 当前页码下游标的开始位置
	 */
	public int getStartIndex()
	{
		this.startIndex = (getPageNo() -1) * getPageSize();
		
		if( this.startIndex <= 0 )
		{
			this.startIndex = 0;
		}
		
		return this.startIndex;
	}

	/**
	 * 设置游标的开始位置
	 * 
	 * @param startIndex 游标的开始位置
	 */
	/*public void setStartIndex( int startIndex )
	{
		this.startIndex = startIndex;
	}*/

	/**
	 * 获取当前页码下游标的结束位置
	 * 
	 * @return 当前页码下游标的结束位置
	 */
	public int getEndIndex()
	{
		this.endIndex = getPageNo() * getPageSize() - 1;
		
		if( this.endIndex <= 0 )
		{
			this.endIndex = 0;
		}
		
		if( this.endIndex >= getTotalRows() )
		{
			this.endIndex = getTotalRows() - 1;
		}
		
		return this.endIndex;
	}

	/**
	 * 设置游标的结束位置
	 * 
	 * @param endIndex 游标的结束位置
	 */
	/*public void setEndIndex( int endIndex )
	{
		this.endIndex = endIndex;
	}*/

	/**
	 * 获取排序字段名称
	 * 
	 * @return 排序字段名称
	 */
	public String getSortname()
	{
		return this.sortname;
	}

	/**
	 * 设置排序字段名称
	 * 
	 * @param sortname 排序字段名称
	 */
	public void setSortname( String sortname )
	{
		this.sortname = sortname;
	}

	/**
	 * 获取排序顺序，例如： ASC -- 升序， DESC -- 降序
	 * 
	 * @return 排序顺序
	 */
	public String getSortorder()
	{
		return this.sortorder;
	}

	/**
	 * 设置排序顺序
	 * 
	 * @param sortorder 排序顺序
	 */
	public void setSortorder( String sortorder )
	{
		this.sortorder = sortorder;
	}
	
}
