package com.lizard.fastdb.jdbc;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * 自定义转换结果集ResultSet为其他对象的实现接口
 * 
 * @author SHEN.GANG
 */
public interface RowCallbackHandler<T> extends ResultSetHandler<T> 
{

}
