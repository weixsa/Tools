package top.weixsa.code.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;



/** 
* @Title: CodeGeneratorUtil.java 
* @Package top.weixsa.code.utils 
* @Description: 根据表生成sql语句
* @author weixsa@gmail.com 
* @date 2018年12月4日 下午8:39:40 
* @version V1.0 
*/
public class CodeGeneratorUtil {

	private static String ROOT_PATH =  "E:/code"; //生成代码基本路径
	private static String XML_TEMP_FILE = "xml-mysql"; //支持多种数据库模板
	private static String PACKAGE_PATH = "top/weixsa/code/utils/"; //支持多种数据库模板
//	private static String DOMAIN_PATH = "top/weixsa/code/utils/"; //domain路径
//	private static String ENTITY_PATH = "top/weixsa/code/utils/"; //entity路径
//	private static String MAPPER_PATH = "top/weixsa/code/utils/"; //SQL文件地址
//	private static String DAO_PATH = "top/weixsa/code/utils/"; //dao路径
//	private static String DAOIMPL_PATH = "top/weixsa/code/utils/"; //dao实现类路径
//	private static String SERVICE_PATH = "top/weixsa/code/utils/"; //service接口路径
//	private static String SERVICEIMPL_PATH = "top/weixsa/code/utils/"; //service实现类路径
//	private static String RESOURCE_PATH = "top/weixsa/code/utils/"; //resource路径
	//方法入口
	public static void main(String[] args) throws Exception {
		String tablename = "point_exch_cond"; // 数据库表名
		String classname = "cond"; // 类前置（即：子模块名）
		String moduleName = "start"; // 模块名
		String tableDesc = "测试用户表"; // 当前业务模块的概要想描述（表描述）
		
		generator(tablename,classname,moduleName,tableDesc);
	}
	
	
	private static void generator(String tablename, String classname, String moduleName, String tableDesc)
			throws Exception {
		
		if (classname != null) {
			classname = classname.substring(0, 1).toUpperCase() + classname.substring(1);
		}
		// 生成 dao层，service层，controll层
		// 初始化spring
		BeanFactory beanFactory = new ClassPathXmlApplicationContext("classpath:/top/weixsa/code/utils/spring-application-ds.xml");
		// 获取数据库链接
		SqlSession sqlsession = (SqlSession) beanFactory.getBean("information_schemaSqlsession");

		// 获取表结构
		List<Map> data = sqlsession.selectList("com.linkdsp.codehelper.getColumnInfo", tablename);
		Context context = new VelocityContext();
		
		context.put("moduleName", moduleName.replace("/", "."));
		context.put("collist", data);
		context.put("tablename", tablename);
		context.put("classname", classname);
		context.put("tableDesc", tableDesc);
		context.put("newclassname", classname.substring(0, 1).toLowerCase() + classname.substring(1));
		// 生成实体bean
		for (int i = 0; i < data.size(); i++) {
			data.get(i).put("newcolumn_name", underlineProccess(data.get(i).get("column_name").toString(), true));//驼峰首字母大写
			data.get(i).put("newfield_name", underlineProccess(data.get(i).get("column_name").toString()));//驼峰
//			if (data.get(i).get("column_name").toString().toLowerCase().equals("created_time")
//					|| data.get(i).get("column_name").toString().toLowerCase().equals("updated_time")) {
//				data.get(i).put("coltype", "Long");
//				continue;
//			}
			String dataType = data.get(i).get("data_type").toString();
			if (dataType.equalsIgnoreCase("varchar") || dataType.equalsIgnoreCase("varchar2")|| dataType.equalsIgnoreCase("char") || dataType.equalsIgnoreCase("text")) {
				data.get(i).put("coltype", "String");
				continue;
			}
			if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("tinyint")) {
				data.get(i).put("coltype", "Integer");
				continue;
			}
			if (dataType.equalsIgnoreCase("bigint")) {
				data.get(i).put("coltype", "Long");
				continue;
			}
			if (dataType.equalsIgnoreCase("double")|| dataType.equalsIgnoreCase("number") || dataType.equalsIgnoreCase("decimal") || dataType.equalsIgnoreCase("float")) {
				//取数据精度
				int dataScale = Integer.valueOf(data.get(i).get("scale").toString());
				if(dataScale>0){
					data.get(i).put("coltype", "BigDecimal");
					addImport(context, "java.math.BigDecimal");
				}else{
					data.get(i).put("coltype", "Long");
				}
				continue;
			}
			if (dataType.equalsIgnoreCase("date") || dataType.equalsIgnoreCase("datetime") || dataType.equalsIgnoreCase("timestamp")) {
				data.get(i).put("coltype", "Date");
				addImport(context, "java.util.Date");
				continue;
			}
		}
		System.out.println(data);
		if (context.get("import") == null) {
			context.put("import", "");
		}

		genCode(context, moduleName, classname);
		System.out.println("实体表【"+ tablename+"】对应代码生成成功！");
	}
	
	private static void genCode(Context context,String moduleName,String classname) throws ResourceNotFoundException, ParseErrorException, Exception{
		String path="";
		
		// 生成实体类domain
		path=ROOT_PATH + "/crm-domain/src/main/java/com/hsbank/domain/" + moduleName + "/"
					+ classname + "Domain.java";
		genertorByType(context,moduleName,classname,"domain",path);
		// 生成Entity
		path=ROOT_PATH + "/crm-core/src/main/java/com/hsbank/core/entities/" + moduleName
				+ "/" + classname + "Entity.java";
		genertorByType(context,moduleName,classname,"entity",path);
		// 生成实体类
		path=ROOT_PATH + "/crm-data-service/src/main/java/com/hsbank/dao/" + moduleName
							+ "/impl/" + classname + ".map.xml";
		genertorByType(context,moduleName,classname,XML_TEMP_FILE,path);
		// 生成DAO
		path=ROOT_PATH + "/crm-data-service/src/main/java/com/hsbank/dao/" + moduleName + "/I"
				+ classname + "Dao.java";
        genertorByType(context,moduleName,classname,"dao",path);
        path=ROOT_PATH + "/crm-data-service/src/main/java/com/hsbank/dao/" + moduleName + "/impl/"
				+ classname + "DaoImpl.java";
        genertorByType(context,moduleName,classname,"dao.impl",path);
		// 生成service层
		path=ROOT_PATH + "/crm-business-service/src/main/java/com/hsbank/service/"
				+ moduleName + "/I" + classname + "Service.java";
		genertorByType(context,moduleName,classname,"service",path);
		path=ROOT_PATH + "/crm-business-service/src/main/java/com/hsbank/service/" + moduleName
				+ "/impl/" + classname + "ServiceImpl.java";
		genertorByType(context,moduleName,classname,"service.impl",path);
		// 生成RESTFulAPI层
		//context.put("moduleName", moduleName.replace(".", "/"));
		path=ROOT_PATH + "/crm-resource/src/main/java/com/hsbank/rs/resources/" + moduleName
				+ "/" + classname + "Resource.java";
		genertorByType(context,moduleName,classname,"controller",path);
	}
	
	/**
	 * 
	 * @param context  模板引擎容器
	 * @param moduleName  模块前缀
	 * @param classname   类名前缀
	 * @param type  生成实体类
	 * @param path  文件生成路径
	 * @throws ResourceNotFoundException
	 * @throws ParseErrorException
	 * @throws Exception
	 */
	public static void genertorByType(Context context, String moduleName, String classname,String type,String path)
			throws ResourceNotFoundException, ParseErrorException, Exception {
		Velocity.setProperty("input.encoding", "utf-8");
		Velocity.setProperty("output.encoding", "utf-8");
		Velocity.setProperty("file.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init();
		//根据类型获取模板
		Template template = Velocity.getTemplate(PACKAGE_PATH + type + ".vm");
		//创建java类
		File domainFile = new File(path);
		if (!domainFile.getParentFile().exists()) {
			domainFile.getParentFile().mkdirs();
		}
		PrintWriter writer = new PrintWriter(domainFile, "utf-8");
		template.merge(context, writer);
		writer.flush();
		writer.close();
	}
	/**
	 * 去掉下划线， 下划线后面第一个字符大写， 第一个字符大写
	 */
	public static String underlineProccess(String str, boolean firstToUpperCase) {
		if (str != null) {
			str = str.toLowerCase();
			str = strProccess("_", str);
			if (firstToUpperCase)
				str = str.substring(0, 1).toUpperCase() + str.substring(1);
			return str;
		}
		return null;
	}
	
	/**
	 * 去掉下划线， 下划线后面第一个字符大写， 第一个字符大写
	 */
	public static String underlineProccess(String str) {
		return underlineProccess(str, false);
	}
	
	/**
	 * 转驼峰
	 * @param replaceStr
	 * @param str
	 * @return
	 */
	private static String strProccess(String replaceStr, String str) {
		if (str != null) {
			int index = str.indexOf(replaceStr);
			if (index > -1) {
				String header = str.substring(0, index);
				String tail = str.substring(index + replaceStr.length());
				tail = tail.substring(0, 1).toUpperCase() + tail.substring(1);
				str = strProccess(replaceStr, header + tail);
			}
		}
		return str;
	}

	/**
	 * 
	 * @param context
	 * @param importConent
	 */
	private static void addImport(Context context, String importConent) {
		if (context.get("import") != null) {
			if (context.get("import").toString().indexOf(importConent) < 0)
				context.put("import", context.get("import") + "\nimport " + importConent + ";");
		} else {
			context.put("import", "import " + importConent + ";");
		}
	}
}
