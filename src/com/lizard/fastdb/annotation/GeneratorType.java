package com.lizard.fastdb.annotation;

/**
 * 主键生成类型
 *
 * @author  SHEN.GANG
 */
public enum GeneratorType
{
	/**
	 * 手工分配
	 */
	ASSIGN,
	
	/**
	 * 自动增长方式，适用于MySQL等支持主键自增特性的数据库
	 */
	AUTO_INCREMENT,
	
	/**
	 * Sequence方式，适用于Oracle
	 */
	SEQUENCE;
}
