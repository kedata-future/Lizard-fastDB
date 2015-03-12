Lizard fastDB是一个轻量级、全功能的数据持久层框架，目的是为了减轻开发人员开发数据库访问层的压力。

Lizard fastDB v1.0 GA

统一、简单、可扩展的数据源配置文件 (只有一个配置文件 datasource.xml)
丰富的JDBC操作接口
ORM注解支持
多数据源支持：druid、bonecp、proxool、c3p0
V1版支持mysql、Oracle数据库
事务管理器支持(支持单数据源、多数据源、事务嵌套)
丰富的工具类支持
Lizard fastDB v2.0（开发中）

支持datasource.properties数据源配置
支持PostgreSQL、mongoDB 数据库
增加缓存功能
集成Cobar
Lizard fastDB V1 JAR Requirements

依赖Jars	说明
commons-lang-2.4.jar	必须
commons-io-1.4.jar	必须
commons-logging-1.1.1.jar	必须
dom4j-1.6.1.jar	必须
jaxen-1.1.1.jar	必须， 用于schema验证xml文件内容和xpath选择
jotm-2.0.8.jar	如果需要使用多数据事务管理器，则必须
druid-1.2.jar	默认数据库连接池，必须
proxool-0.9.1.jar	如果使用Proxool连接池，则必须
proxool-cglib-0.9.1.jar	Proxool依赖包
c3p0-0.9.1.2.jar	如果使用C3P0连接池，则必须
c3p0-oracle-thin-extras-0.9.1.2.jar	如果使用C3P0连接池，且是Oracle数据库并thin连接方式，则必须
bonecp-0.7.1.jar	如果使用BoneCP连接池，则必须
jdom-1.0.jar	用于辅助jaxen解析xml文件
xalan-2.6.0.jar	Apache 内置 - 根据实际情况增加
xercesImpl-2.6.2.jar	Apache 内置 - 根据实际情况增加
xml-apis-1.0.b2.jar	Apache 内置 - 根据实际情况增加
xmlParserAPIs-2.6.2.jar	Apache 内置 - 根据实际情况增加
xom-1.0.jar	Apache 内置 - 根据实际情况增加
How to use

Step 1：配置

将 fastdb-1.0.0.jar 及相关依赖 jar 包拷贝到项目lib 下；
将 datasource.xml 拷贝到项目的 classpath 下；
在 web.xml 文件中配置监听器（该监听器的作用是预解析数据源相关配置信息、销毁数据库连接池）：说明：该监听器是可选配置，但是强烈建议webapp程序进行配置，可以提高数据库访问性能。
<listener>
    <listener-class>com.lizard.fastdb.datasource.DataSourceListener</listener-class>
</listener>
配置 datasource.xml 中的数据源连接信息。
<datasources
    xmlns="http://fastdb.com/db"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://fastdb.com/db/fastdb-1.0.xsd">
        
    <datasource>
        <name>test</name>
        <driver-class>oracle.jdbc.driver.OracleDriver</driver-class>
        <driver-url>jdbc:oracle:thin:@10.0.42.90:1521:ora10g</driver-url>
        <user>root</user>
        <password>123456</password>
   </datasource>
</datasources>
对于Java应用程序(J2SE)，只需要将 fastdb-xxx.jar、相关依赖包和 datasource.xml 文件放到应用程序的classpath下，然后在 datasource.xml 文件中配置数据源即可。
Step 2：使用

查询数据
// 导入包
import com.lizard.fastdb.jdbc.JdbcHandler;
import com.lizard.fastdb.DBFactory;

// 创建数据源对象：DBFactory.create( "数据源name" );
JdbcHandler jdbc = DBFactory.create("test");

String sql = "SELECT * FROM tableName";

// 调用JdbcHandler的queryForList接口
List<Map<String, Object>> listMap = jdbc.queryForList( sql );

for( Map<String, Object> map : listMap ){
  String value = map.get( key ); // key 是小写字母
  // ......
}
详细使用指南

数据源配置文件 datasource.xml 详解
JdbcHanlder使用
事务管理器(单数据源事务，多数据源事务，事务嵌套)使用
