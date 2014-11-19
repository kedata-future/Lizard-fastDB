package com.lizard.fastdb.jdbc;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import com.lizard.fastdb.datasource.DataSource;

/**
 *  fastDB JDBC 操作接口 定义并封装了一些常用的 JDBC 操作接口
 * 
 * @author SHEN.GANG
 */
public interface JdbcHandler extends Serializable
{
	/**
	 * 保存一个使用了  fastDB Annotation 的对象到数据库
	 * <p>
	 * fastDB Annotation Example: (注：为了能让@在javadoc中显示，故在将@改为_@，在实际使用时请还原为@)
	 * <p>
	 * <hr>
	 * <blockquote>
	 * 
	 * <pre>
	 * _@Table(name = &quot;fastdb_user&quot;)
	 * public class User
	 * {
	 *    _@Column(name=&quot;id&quot;, primaryKey=true, generatorType=GeneratorType.AUTO_INCREMENT)
	 *    private long id;
	 * 
	 *    _@Column(name=&quot;username&quot;)
	 *    private String username;
	 * 
	 *    _@Column(name=&quot;real_name&quot;)
	 *    private String realname;
	 * 
	 *    _@Column(name=&quot;age&quot;)
	 *    private int age;
	 * 
	 *    // 下面是构造方法以及属性的setter/getter方法
	 *    ......
	 * }
	 * 
	 * User user = new User();
	 * user.setUsername("test");
	 * user.setRealname("test_realname");
	 * user.setAge(20);
	 * 
	 * boolean res = jdbcHandler.save( user );
	 * </pre>
	 * 
	 * </blockquote>
	 * <hr>
	 * 
	 * @param bean 使用了 fastDB annotation 的对象
	 * @return 保存结果
	 */
	public boolean save(Object bean);

	/**
	 * 更新一个使用了 fastDB Annotation 的对象到数据库
	 * <hr>
	 * <blockquote>
	 * User Annotation 参见 {@link #save(Object)} 的User样例
	 * <pre>
	 * User user = ...;
	 * user.setUsername("test_007");
	 * user.setAge(25);
	 * 
	 * boolean res = jdbcHandler.update( user );
	 * </pre>
	 * </blockquote>
	 * <hr>
	 * 
	 * @param bean 使用了 fastDB annotation 的对象
	 * @return 更新结果
	 */
	public boolean update(Object bean);
	
	/**
	 * 更新一个使用了 fastDB Annotation 对象的部分属性到数据库
	 * <hr>
	 * <blockquote>
	 * User Annotation 参见 {@link #save(Object)} 的User样例
	 * <pre>
	 * User user = ...;
	 * user.setUsername("test_007");
	 * user.setRealname("test_2012");
	 * user.setAge(30);
	 * 
	 * ## 只更新User Bean的 realname 和 age 2个属性到数据库
	 * boolean res = jdbcHandler.update( user, new String[]{"realname", "age"} );
	 * </pre>
	 * </blockquote>
	 * <hr>
	 * 
	 * @param bean 使用了 fastDB annotation 的对象
	 * @param updatedFields 需要更新的字段(指Bean的属性，而不是@Column的name属性值)集合，如果为null或空，则更新Bean的全部属性。
	 * @return 更新成功返回 true，失败返回 false
	 */
	public boolean update(Object bean, String[] updatedFields);

	/**
	 * 执行一个INSERT SQL语句，不可以执行 UPDATE, DELETE 等语句，<br>
	 * 如果要执行 UPDATE, DELETE 等语句，使用 {@link #execute(String)} 替代。
	 * <p>
	 * <b>注意：</b>该方法仅适用于那些支持 <b>auto_increment</b> 主键生成方式的数据库！例如：MySQL 等
	 * </p>
	 * 
	 * @param sql 要执行的 INSERT SQL 语句
	 * @return 返回生成的主键值，如果失败，返回 -1
	 */
	public long saveForGeneratedKey(String sql);

	/**
	 * 执行一个INSERT SQL语句，不可以执行 UPDATE, DELETE 等语句，<br>
	 * 如果要执行 UPDATE, DELETE 等语句，使用 {@link #execute(String, Object...)} 替代。
	 * <p>
	 * <b>注意：</b>该方法仅适用于那些支持 <b>auto_increment</b> 主键生成方式的数据库！例如：MySQL 等
	 * </p>
	 * 
	 * @param sql 要执行的 INSERT SQL 语句
	 * @param paramValues INSERT 语句中要替换的参数值
	 * @return 返回生成的主键值，如果失败，返回 -1
	 */
	public long saveForGeneratedKey(String sql, Object... paramValues);
	
	/**
	 * 执行一个INSERT SQL语句，不可以执行 UPDATE, DELETE 等语句，<br>
	 * 如果要执行 UPDATE, DELETE 等语句，使用 {@link #execute(String, Map)} 替代。
	 * <p>
	 * <b>注意：</b>该方法仅适用于那些支持 <b>auto_increment</b> 主键生成方式的数据库！例如：MySQL 等
	 * </p>
	 * 
	 * @param namedSql 要执行的 INSERT 命名参数SQL 语句
	 * @param paramValues 命名参数值
	 * @return 返回生成的主键值，如果失败，返回 -1
	 */
	public long saveForGeneratedKey(String namedSql, Map<String, Object> paramValues);

	/**
	 * 执行 INSERT, UPDATE, DELETE, CREATE, DROP 等语句。
	 * 
	 * @param sql 要执行的DML和DDL类型SQL语句
	 * @return true -- 执行成功，false -- 执行失败
	 */
	public boolean execute(String sql);

	/**
	 * 执行 INSERT, UPDATE, DELETE, CREATE, DROP 等语句。
	 * 
	 * @param sql 要执行的DML和DDL类型SQL语句
	 * @param paramValues SQL语句中要替换的参数值
	 * @return true -- 执行成功，false -- 执行失败
	 */
	public boolean execute(String sql, Object... paramValues);
	
	/**
	 * 执行 INSERT, UPDATE, DELETE, CREATE, DROP 等语句。
	 * 
	 * @param namedSql 使用命名参数的SQL语句
	 * @param paramValues 命名参数值
	 * @return true -- 执行成功，false -- 执行失败
	 */
	public boolean execute(String namedSql, Map<String, Object> paramValues);

	/**
	 * 根据外部数据库连接对象Connection执行execute操作。
	 * <p>
	 * <font color="#ff0000"><b>注意：</b>如果使用外部Connection，在使用完毕后一定要<b>关闭</b>! 可以调用 {@link #closeConnection(Connection)}关闭。</font>
	 * </p>
	 * 
	 * @param conn 外部数据库连接对象
	 * @param sql 要执行的DML和DDL类型SQL语句
	 * @param paramValues SQL语句中要替换的参数值
	 * @return true -- 执行成功，false -- 执行失败
	 */
	public boolean execute(Connection conn, String sql, Object... paramValues);
	
	/**
	 * 根据外部数据库连接对象Connection执行execute操作。
	 * <p>
	 * <font color="#ff0000"><b>注意：</b>如果使用外部Connection，在使用完毕后一定要<b>关闭</b>! 可以调用 {@link #closeConnection(Connection)}关闭。</font>
	 * </p>
	 * 
	 * @param conn 外部数据库连接对象
	 * @param namedSql 使用命名参数的SQL语句
	 * @param paramValues 命名参数值
	 * @return true -- 执行成功，false -- 执行失败
	 */
	public boolean execute(Connection conn, String namedSql, Map<String, Object> paramValues);

	/**
	 * 执行批量的 INSERT, UPDATE, DELETE 等DDL语句。
	 * <p>
	 * 该批量操作默认不分批执行，不回滚操作。
	 * <p>
	 * 如果想要设置分批执行，则进行如下设置即可：
	 * <p>
	 * <hr>
	 * <blockquote>
	 * 
	 * <pre>
	 *  JdbcHandler jdbc = DBFactory.create(alias);
	 *  // 设置分批大小 setBatchSize( int batchSize )，例如：每100条SQL命令执行一次
	 *  jdbc.setBatchSize(100);
	 *  // 调用批量操作接口
	 *  jdbc.batchUpdate( ..... );
	 * </pre>
	 * 
	 * </blockquote>
	 * <hr>
	 * <p>
	 * 如果要设置回滚操作，请调用：batchUpdate( String[], boolean )方法
	 * 
	 * @param sqls 批量静态SQL语句数组
	 * @return BatchUpdateResult 批量操作结果信息
	 *         <p>
	 *         包含:success, errorBatchs, batchRows 信息
	 *         </p>
	 *         <ul>
	 *         <li>success - 如果批量操作<b>全部</b>执行成功，则为 true，否则 false。</li>
	 *         <li>errorBatchs - int[]数组，存储执行失败的分批批次信息，例如：[2,4]表示分批执行中第2,4批次执行失败；如果全部执行成功，errorBatchs 为[]。</li>
	 *         <li>batchRows - 批量处理最终返回的计数数组，即：Statement.executeBatch()返回的int[]信息内容</li>
	 *         </ul>
	 */
	public BatchUpdateResult batchUpdate(String[] sqls);

	/**
	 * 执行批量的 INSERT, UPDATE, DELETE 等DDL语句。
	 * <p>
	 * 该批量操作默认不分批执行，不回滚操作。
	 * <p>
	 * 如果想要设置分批执行，则进行如下设置即可：
	 * <p>
	 * <hr>
	 * <blockquote>
	 * 
	 * <pre>
	 * JdbcHandler jdbc = DBFactory.create(alias);
	 * // 设置分批大小 setBatchSize( int batchSize )，例如：每100条SQL命令执行一次
	 * jdbc.setBatchSize(100);
	 * // 调用批量操作接口
	 * jdbc.batchUpdate( ..... );
	 * </pre>
	 * 
	 * </blockquote>
	 * <hr>
	 * 
	 * @param sqls 批量SQL语句数组
	 * @param rollback 事务回滚， true -- 设置事务回滚，false -- 不设置事务回滚
	 *            <ul>
	 *            <li>未设置分批操作 -- 回滚所有执行的事务</li>
	 *            <li>设置了分批操作 -- 回滚执行失败的批次的所有事务</li>
	 *            </ul>
	 *            <b>需要注意的是：如果当前操作在事务管理器中，rollback将在内部被设为true，而忽略参数中该值的设置</b>
	 * @return BatchUpdateResult 批量操作结果信息
	 *         <p>
	 *         包含:success, errorBatchs, batchRows 信息
	 *         </p>
	 *         <ul>
	 *         <li>success - 如果批量操作<b>全部</b>执行成功，则为 true，否则 false。</li>
	 *         <li>errorBatchs - int[]数组，存储执行失败的分批批次信息，例如：[2,4]表示分批执行中第2,4批次执行失败；如果全部执行成功，errorBatchs 为[]。</li>
	 *         <li>batchRows - 批量处理最终返回的计数数组，即：Statement.executeBatch()返回的int[]信息内容。</li>
	 *         </ul>
	 */
	public BatchUpdateResult batchUpdate(String[] sqls, boolean rollback);

	/**
	 * 执行批量的 INSERT, UPDATE, DELETE 等DDL语句。
	 * <p>
	 * 该批量操作默认不分批执行，不回滚操作。
	 * <p>
	 * 如果想要设置分批执行，则进行如下设置即可：
	 * <p>
	 * <hr>
	 * <blockquote>
	 * 
	 * <pre>
	 * JdbcHandler jdbc = DBFactory.create(alias);
	 * // 设置分批大小 setBatchSize( int batchSize )，例如：每100条SQL命令执行一次
	 * jdbc.setBatchSize(100);
	 * // 调用批量操作接口
	 * jdbc.batchUpdate( ..... );
	 * </pre>
	 * 
	 * </blockquote>
	 * <hr>
	 * <p>
	 * 如果要设置回滚操作，请调用：batchUpdate( String, Object[][], boolean )方法
	 * 
	 * @param sql 带?占位符的预编译sql语句，例如：INSERT INTO tableName VALUES(?, ?, ?, ...)
	 * @param paramValues 预编译sql语句的参数
	 * @return BatchUpdateResult 批量操作结果信息
	 *         <p>
	 *         包含:success, errorBatchs, batchRows 信息
	 *         </p>
	 *         <ul>
	 *         <li>success - 如果批量操作<b>全部</b>执行成功，则为 true，否则 false。</li>
	 *         <li>errorBatchs - int[]数组，存储执行失败的分批批次信息，例如：[2,4]表示分批执行中第2,4批次执行失败；如果全部执行成功，errorBatchs 为[]。</li>
	 *         <li>batchRows - 批量处理最终返回的计数数组，即：Statement.executeBatch()返回的int[]信息内容</li>
	 *         </ul>
	 */
	public BatchUpdateResult batchUpdate(String sql, Object[][] paramValues);

	/**
	 * 执行批量的 INSERT, UPDATE, DELETE 等DDL语句。
	 * <p>
	 * 该批量操作默认不分批执行，不回滚操作。
	 * <p>
	 * 如果想要设置分批执行，则进行如下设置即可：
	 * <p>
	 * <hr>
	 * <blockquote>
	 * 
	 * <pre>
	 * JdbcHandler jdbc = DBFactory.create(alias);
	 * // 设置分批大小 setBatchSize( int batchSize )，例如：每100条SQL命令执行一次
	 * jdbc.setBatchSize(100);
	 * // 调用批量操作接口
	 * jdbc.batchUpdate( ..... );
	 * </pre>
	 * 
	 * </blockquote>
	 * <hr>
	 * 
	 * @param sql 带?占位符的预编译sql语句，例如：INSERT INTO tableName VALUES(?, ?, ?, ...)
	 * @param paramValues 预编译sql语句的参数
	 * @param rollback 事务回滚， true -- 设置事务回滚，false -- 不设置事务回滚
	 *            <ul>
	 *            <li>未设置分批操作 -- 回滚所有执行的事务</li>
	 *            <li>设置了分批操作 -- 回滚执行失败的批次的所有事务</li>
	 *            </ul>
	 *            <b>需要注意的是：如果当前操作在事务管理器中，rollback将在内部被设为true，而忽略参数中该值的设置</b>
	 * @return BatchUpdateResult 批量操作结果信息
	 *         <p>
	 *         包含:success, errorBatchs, batchRows 信息
	 *         <ul>
	 *         <li>success - 如果批量操作<b>全部</b>执行成功，则为 true，否则 false。</li>
	 *         <li>errorBatchs - int[]数组，存储执行失败的分批批次信息，例如：[2,4]表示分批执行中第2,4批次执行失败；如果全部执行成功，errorBatchs 为[]。</li>
	 *         <li>batchRows - 批量处理最终返回的计数数组，即：Statement.executeBatch()返回的int[]信息内容。</li>
	 *         </ul>
	 */
	public BatchUpdateResult batchUpdate(String sql, Object[][] paramValues, boolean rollback);

	/**
	 * 执行一个带参数的SQL语句，并通过自定义实现RowCallbackHandler接口返回对象
	 * <p>
	 * Example:
	 * </p>
	 * <p>
	 * <hr>
	 * <blockquote>
	 * 
	 * <pre>
	 * JdbcHandler jdbc = DBFactory.create(...);
	 * 
	 * // 查询单一对象，自定义返回HashMap
	 * String sql = &quot; SELECT * FROM tableName WHERE id = ? &quot;;
	 * Map&lt;String, String&gt; map = jdbc.query( sql, new RowCallbackHandler&lt;Map&lt;String,String&gt;&gt;(){
	 *    // 实现 handle 接口
	 *    public Object handle(ResultSet rs) throws SQLException {
	 *        Map&lt;String, String&gt; map = new HashMap&lt;String, String&gt;();
	 *        if (rs.next()) {
	 *             ResultSetMetaData rsmd = rs.getMetaData();
	 *             int columnCount = rsmd.getColumnCount();
	 *             for (int i = 1; i &lt;= columnCount; i++) {
	 *                 map.put(rsmd.getColumnLabel(i) ,rs.getString(i));
	 *             }
	 *        }
	 *        if (rs != null){
	 *            rs.close();
	 *            rs = null;
	 *        }
	 *        return map;
	 *    }
	 *  }, 1);
	 * </pre>
	 * 
	 * </blockquote>
	 * <p>
	 * <hr>
	 * <blockquote>
	 * 
	 * <pre>
	 * // 查询结果集，并自定义返回List&lt;Map&lt;String, String&gt;&gt;
	 * String	sql2	= &quot;SELECT * FROM table1&quot;;
	 * List&lt;Map&lt;String, String&gt;&gt; list = dbm.query(sql2, new RowCallbackHandler&lt;List&lt;Map&lt;String, String&gt;&gt;&gt;()
	 * {
	 * 		// 实现 handle 接口
	 * 		public Object handle(ResultSet rs) throws SQLException {
	 * 			List&lt;Map&lt;String, String&gt;&gt; _list = new ArrayList&lt;Map&lt;String, String&gt;&gt;();
	 * 			Map&lt;String, String&gt; map = null;
	 * 			while (rs.next()){
	 * 				map = new HashMap&lt;String, String&gt;();
	 * 				ResultSetMetaData rsmd = rs.getMetaData();
	 * 				int columnCount = rsmd.getColumnCount();
	 * 				for (int i = 1; i &lt;= columnCount; i++) {
	 * 					map.put(rsmd.getColumnLabel(i), rs.getString(i));
	 * 				}
	 * 				_list.add(map);
	 * 			}
	 * 
	 * 			if (rs != null) {
	 * 				rs.close();
	 * 				rs = null;
	 * 			}
	 * 
	 * 			return _list;
	 * 		}
	 * 	}, new Object[]{});
	 * </pre>
	 * 
	 * </blockquote>
	 * <hr>
	 * 
	 * @param <T> RowCallbackHandler转换返回的对象类型
	 * @param sql 要执行的SQL语句
	 * @param rch RowCallbackHandler要转换ResultSet结果集接口的实现类
	 * @param paramValues SQL语句中要替换的参数值
	 * @return RowCallbackHandler转换返回的对象类型
	 */
	public <T> T query(String sql, RowCallbackHandler<T> rch, Object... paramValues);
	
	/**
	 *  执行一个带参数的SQL语句，并通过自定义实现RowCallbackHandler接口返回对象
	 * 
	 * @param <T> RowCallbackHandler转换返回的对象类型
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT state FROM table WHERE id =:id)
	 * @param rch RowCallbackHandler要转换ResultSet结果集接口的实现类
	 * @param paramValues 命名参数值
	 * @return RowCallbackHandler转换返回的对象类型
	 */
	public <T> T query(String namedSql, RowCallbackHandler<T> rch, Map<String, Object> paramValues);

	/**
	 * 查询单一对象，并转换成自定义JavaBean对象
	 * 
	 * @param <T> 泛型，接受自定义JavaBean.class
	 * @param beanClass 自定义JavaBean.class
	 * @param sql 要执行的查询语句
	 * @return 封装数据后的单一JavaBean对象
	 */
	public <T> T queryForBean(Class<T> beanClass, String sql);

	/**
	 * 查询单一对象，并转换成自定义JavaBean对象
	 * 
	 * @param <T> 泛型，接受自定义JavaBean.class
	 * @param beanClass 自定义JavaBean.class
	 * @param sql 要执行的查询语句
	 * @param paramValues SQL语句中的变量值
	 * @return 封装数据后的单一JavaBean对象
	 */
	public <T> T queryForBean(Class<T> beanClass, String sql, Object... paramValues);
	
	/**
	 * 查询单一对象，并转换成自定义JavaBean对象
	 * 
	 * @param <T> 泛型，接受自定义JavaBean.class
	 * @param beanClass 自定义JavaBean.class
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT state FROM table WHERE id =:id)
	 * @param paramValues 命名参数值
	 * @return 封装数据后的单一JavaBean对象
	 */
	public <T> T queryForBean(Class<T> beanClass, String namedSql, Map<String, Object> paramValues);

	/**
	 * 查询单一数据对象，以超类Object返回
	 * 
	 * @param sql 要执行的查询语句
	 * @return 单一Object数据对象
	 */
	public Object queryForObject(String sql);

	/**
	 * 查询单一数据对象，以超类Object返回
	 * 
	 * @param sql 要执行的查询语句
	 * @param paramValues 查询语句中的变量值
	 * @return 单一Object数据对象
	 */
	public Object queryForObject(String sql, Object... paramValues);
	
	/**
	 * 查询单一数据对象，以超类Object返回
	 * 
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT state FROM table WHERE id =:id)
	 * @param paramValues 命名参数值
	 * @return 单一Object数据对象
	 */
	public Object queryForObject(String namedSql, Map<String, Object> paramValues);

	/**
	 * 查询单一数据对象，并封装成Map<String, Object> 返回
	 * 
	 * @param sql 要执行的查询语句
	 * @return 单一Map<String, Object> 数据对象
	 */
	public Map<String, Object> queryForMap(String sql);

	/**
	 * 查询单一数据对象，并封装成Map<String, Object> 返回
	 * 
	 * @param sql 要执行的查询语句
	 * @param paramValues 查询语句中的变量值
	 * @return 单一Map<String, Object> 数据对象
	 */
	public Map<String, Object> queryForMap(String sql, Object... paramValues);
	
	/**
	 * 查询单一数据对象，并封装成Map<String, Object> 返回
	 * 
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT state FROM table WHERE id =:id)
	 * @param paramValues 命名参数值
	 * @return 单一Map<String, Object> 数据对象
	 */
	public Map<String, Object> queryForMap(String namedSql, Map<String, Object> paramValues);


	/**
	 * 查询单一数据类型 Integer
	 * 
	 * @param sql 要执行的查询语句
	 * @return Integer数据类型，如果查询不到值，则返回 Null
	 */
	public Integer queryForInteger(String sql);

	/**
	 * 查询单一数据类型 Integer
	 * 
	 * @param sql 要执行的查询语句
	 * @param paramValues SQL语句中的变量值
	 * @return Integer数据类型，如果查询不到值，则返回 Null
	 */
	public Integer queryForInteger(String sql, Object... paramValues);
	
	/**
	 * 查询单一数据类型 Integer
	 * 
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT state FROM table WHERE id =:id)
	 * @param paramValues 命名参数值
	 * @return Integer数据类型，如果查询不到值，则返回 Null
	 */
	public Integer queryForInteger(String namedSql, Map<String, Object> paramValues);

	/**
	 * 查询单一数据类型 Long
	 * 
	 * @param sql 要执行的查询语句
	 * @return Long数据类型，如果查询不到值，则返回 0
	 */
	public Long queryForLong(String sql);

	/**
	 * 查询单一数据类型 Long
	 * 
	 * @param sql 要执行的查询语句
	 * @param paramValues SQL语句中的变量值
	 * @return Long数据类型，如果查询不到值，则返回 0
	 */
	public Long queryForLong(String sql, Object... paramValues);
	
	/**
	 * 查询单一数据类型 Long
	 * 
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT state FROM table WHERE id =:id)
	 * @param paramValues 命名参数值
	 * @return Long数据类型，如果查询不到值，则返回 0
	 */
	public Long queryForLong(String namedSql, Map<String, Object> paramValues);

	/**
	 * 查询数据结果集，以List集合存储自定义泛型类型返回
	 * 
	 * @param <T> 自定义泛型类型
	 * @param beanClass 自定义JavaBean.class
	 * @param sql 要执行的查询语句
	 * @return 存储自定义泛型类型的结果集List
	 */
	public <T> List<T> queryForList(Class<T> beanClass, String sql);

	/**
	 * 查询数据结果集，以List集合存储自定义泛型类型返回
	 * 
	 * @param <T> 自定义泛型类型
	 * @param beanClass 自定义JavaBean.class
	 * @param sql 要执行的查询语句
	 * @param paramValues 查询语句中的变量值
	 * @return 存储自定义泛型类型的结果集List
	 */
	public <T> List<T> queryForList(Class<T> beanClass, String sql, Object... paramValues);
	
	/**
	 * 查询数据结果集，以List集合存储自定义泛型类型返回
	 * 
	 * @param <T> 自定义泛型类型
	 * @param beanClass 自定义JavaBean.class
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT * FROM table WHERE id >:id)
	 * @param paramValues 命名参数值
	 * @return 存储自定义泛型类型的结果集List
	 */
	public <T> List<T> queryForList(Class<T> beanClass, String namedSql, Map<String, Object> paramValues);

	/**
	 * 查询支持分页数据结果集，以List集合存储自定义泛型类型返回
	 * 
	 * @param <T> 自定义泛型类型
	 * @param beanClass 自定义JavaBean.class
	 * @param sql 要执行的查询语句
	 * @param page 页码
	 * @param pagesize 分页结果集大小
	 * @param paramValues 查询语句中的变量值
	 * @return 分页后存储自定义泛型类型的结果集List
	 */
	public <T> List<T> queryForPageList(Class<T> beanClass, String sql, int page, int pagesize, Object... paramValues);
	
	/**
	 * 查询支持分页数据结果集，以List集合存储自定义泛型类型返回
	 * 
	 * @param <T> 自定义泛型类型
	 * @param beanClass 自定义JavaBean.class
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT * FROM table WHERE id >:id)
	 * @param page 页码
	 * @param pagesize 分页结果集大小
	 * @param paramValues 命名参数值
	 * @return 分页后存储自定义泛型类型的结果集List
	 */
	public <T> List<T> queryForPageList(Class<T> beanClass, String namedSql, int page, int pagesize, Map<String, Object> paramValues);

	/**
	 * 查询数据结果集，以List集合存储Map<String, Object>类型返回
	 * 
	 * @param sql 要执行的查询语句
	 * @return 存储Map<String, Object>类型的结果集List
	 */
	public List<Map<String, Object>> queryForList(String sql);

	/**
	 * 查询数据结果集，以List集合存储Map<String, Object>类型返回
	 * 
	 * @param sql 要执行的查询语句
	 * @param paramValues 查询语句中的变量值
	 * @return 存储Map<String, Object>类型的结果集List
	 */
	public List<Map<String, Object>> queryForList(String sql, Object... paramValues);
	
	/**
	 * 查询数据结果集，以List集合存储Map<String, Object>类型返回
	 * 
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT * FROM table WHERE id >:id)
	 * @param paramValues 命名参数值
	 * @return 存储Map<String, Object>类型的结果集List
	 */
	public List<Map<String, Object>> queryForList(String namedSql, Map<String, Object> paramValues);

	/**
	 * 查询支持分页数据结果集，以List集合存储Map<String, Object>类型返回
	 * 
	 * @param sql 要执行的查询语句
	 * @param page 页码
	 * @param pagesize 分页结果集大小
	 * @param paramValues 查询语句中的变量值
	 * @return 分页后存储Map<String, Object>类型的结果集List
	 */
	public List<Map<String, Object>> queryForPageList(String sql, int page, int pagesize, Object... paramValues);

	/**
	 * 查询支持分页数据结果集，以List集合存储Map<String, Object>类型返回
	 * 
	 * @param namedSql 使用了命名参数的SQL语句(Example: SELECT * FROM table WHERE id >:id)
	 * @param page 页码
	 * @param pagesize 分页结果集大小
	 * @param paramValues 命名参数值
	 * @return 分页后存储Map<String, Object>类型的结果集List
	 */
	public List<Map<String, Object>> queryForPageList(String namedSql, int page, int pagesize, Map<String, Object> paramValues);
	
	/**
	 * 获取数据表所有字段名称值
	 * 
	 * @param tableName 数据库表名
	 * @return 表字段值集合
	 */
	public List<String> queryTableFields(String tableName);

	/**
	 * 判断数据库中是否存在表名为<code>tableName</code>的表
	 * 
	 * @param tableName 表名
	 * @return true--表存在，false--表不存在
	 */
	public boolean isTableExist(String tableName);

	/**
	 * 调用无参数存储过程
	 * 
	 * @param procedureName 存储过程名称
	 * @return true -- 调用成功， false -- 调用失败
	 */
	public boolean callProcedure(String procedureName);

	/**
	 * 调用存储过程
	 * 
	 * @param paramValues 存储过程参数值
	 * @return true -- 调用成功， false -- 调用失败
	 */
	public boolean callProcedure(String procedureName, Object... paramValues);

	/**
	 * 调用无参数存储过程，返回结果集
	 * 
	 * @param procedureName 存储过程名称
	 * @return 执行存储返回结果字符串
	 */
	public String callProcedureForResult(String procedureName);

	/**
	 * 调用无参数存储过程，返回结果对象 <br>
	 * 存储过程中请将返回参数写在最前面
	 * 
	 * @param procedureName 存储过程名称
	 * @param sqlType java.sql.Types.xxx指定
	 * @return 执行存储返回结果对象
	 */
	public Object callProcedureForResult(String procedureName, int sqlType);

	/**
	 * 调用存储过程，返回结果字符串 <br>
	 * 存储过程中请将返回参数写在最前面
	 * 
	 * @param procedureName 存储过程名称
	 * @param paramValues 存储过程参数值
	 * @return 执行存储返回return结果字符串
	 */
	public String callProcedureForResult(String procedureName, Object... paramValues);

	/**
	 * 调用存储过程，返回结果对象 <br>
	 * 存储过程中请将返回参数写在最前面
	 * 
	 * @param procedureName 存储过程名称
	 * @param sqlType java.sql.Types.xxx指定
	 * @param paramValues 存储过程参数值
	 * @return 执行存储返回结果对象
	 */
	public Object callProcedureForResult(String procedureName, int sqlType, Object... paramValues);

	/**
	 * 调用存储过程返回结果集<br>
	 * <code>
	 * PROCEDURE TEST_P (param1 in varchar2,param2 out varchar2)<br>
	 * 
	 * callProcedureForResult("TEST_P", java.sql.Types.VARCHAR, 1, "param1");
	 * 
	 * </code>
	 * 
	 * @param procedureName 存储过程名称
	 * @param sqlType out参数的参数类型，使用 java.sql.Types.xxx 指定
	 * @param sqlTypeIndex out参数在所有参数集中的位置，位置计数从0开始
	 * @param paramValues in类型参数的参数值
	 * @return 执行存储过程返回的out参数结果集，顺序按照存储过程中out参数的先后顺序
	 */
	public Object callProcedureForResult(String procedureName, int sqlType, int sqlTypeIndex, Object... paramValues);

	/**
	 * 调用存储过程，返回结果对象集合List <br>
	 * 存储过程中请将返回参数写在最前面
	 * 
	 * @param procedureName 存储过程名称
	 * @param sqlTypes java.sql.Types.xxx指定
	 * @param paramValues 存储过程参数值
	 * @return 执行存储返回结果对象集合List
	 */
	public List<Object> callProcedureForResult(String procedureName, int[] sqlTypes, Object... paramValues);

	/**
	 * 调用存储过程返回结果集<br>
	 * <code>
	 * PROCEDURE TEST_P (param1 in varchar2,param2 out varchar2)<br>
	 * 
	 * callProcedureForResult("TEST_P", new int[]{java.sql.Types.VARCHAR}, new int[]{1},"param1");
	 * 
	 * </code>
	 * 
	 * @param procedureName 存储过程名称
	 * @param sqlTypes out参数的参数类型，使用 java.sql.Types.xxx 指定
	 * @param sqlTypesIndex out参数在所有参数集中的位置，位置计数从0开始，该参数的长度应该与sqlTypes相同，即sqlTypes与sqlTypesIndex应该一一对应
	 * @param paramValues in类型参数的参数值
	 * @return 执行存储过程返回的out参数结果集，顺序按照存储过程中out参数的先后顺序
	 */
	public List<Object> callProcedureForResult(String procedureName, int[] sqlTypes, int[] sqlTypesIndex, Object... paramValues);

	/**
	 * 设置批量操作提交的分段大小，默认不分批操作
	 * 
	 * @param batchSize 批量操作提交分段大小
	 */
	public void setBatchSize(int batchSize);

	/**
	 * 判断序列是否存在，仅用于Oracle数据库
	 * 
	 * @param sequenceName 序列名
	 * @return true -- 存在，false -- 不存在
	 */
	public boolean isSequenceExist(String sequenceName);

	/**
	 * 判断索引是否存在，该方法可用于MySQL和Oracle数据库
	 * 
	 * @param tableName 表名
	 * @param indexName 索引名
	 * @return true -- 存在，false -- 不存在
	 */
	public boolean isIndexExist(String tableName, String indexName);

	/**
	 * 判断函数是否存在，仅用于Oracle数据库
	 * 
	 * @param functionName 函数名
	 * @return true -- 存在，false -- 不存在
	 */
	public boolean isFunctionExist(String functionName);

	/**
	 * 判断存储过程是否存在，仅用于Oracle数据库
	 * 
	 * @param procedureName 存储过程名
	 * @return true -- 存在，false -- 不存在
	 */
	public boolean isProcedureExist(String procedureName);

	/**
	 * 判断是否可以获得当前数据源的连接
	 * 
	 * @return true -- 可获得，false -- 不可获得
	 */
	public boolean isConnectionReachable();

	/**
	 * 获得当前数据源的数据源对象<br>
	 * 注意：返回的对象是只读的，任何改变数据源属性的操作都将抛出UnsupportedOperationException异常
	 * 
	 * @return 数据源对象
	 */
	public DataSource getDataSource();

	/**
	 * 从连接池获得一个数据库连接
	 * 
	 * @return 数据库连接对象
	 */
	public Connection getConnection();

	/**
	 * 关系当前连接池中的连接
	 * 
	 * @param conn 要关闭的连接
	 */
	public void close(Connection conn);

	/**
	 * 关闭一个 <code>Statement</code>，这个实现避免了关闭一个空的 Statement， 并且在内部捕获了这个异常，同时在控制台 Throw。
	 * 
	 * @param st 要关闭的 Statement
	 */
	public void close(Statement st);

	/**
	 * 关闭一个 <code>ResultSet</code>，这个实现避免了关闭一个空的 ResultSet， 并且在内部捕获了这个异常，同时在控制台 Throw。
	 * 
	 * @param rs 要关闭的 ResultSet
	 */
	public void close(ResultSet rs);
}
