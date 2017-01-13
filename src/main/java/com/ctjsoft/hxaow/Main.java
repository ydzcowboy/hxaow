package com.ctjsoft.hxaow;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.internal.info.MigrationInfoDumper;
import org.flywaydb.core.internal.util.ClassUtils;
import org.flywaydb.core.internal.util.FileCopyUtils;
import org.flywaydb.core.internal.util.StringUtils;
import org.flywaydb.core.internal.util.VersionPrinter;
import org.flywaydb.core.internal.util.logging.Log;
import org.flywaydb.core.internal.util.logging.LogFactory;
import org.flywaydb.core.internal.util.logging.console.ConsoleLog.Level;
import org.flywaydb.core.internal.util.logging.console.ConsoleLogCreator;

import com.ctjsoft.hxaow.bo.Project;
import com.ctjsoft.hxaow.bo.VerInfo;


/**
 * Main class and central entry point of the hxaow command-line tool.
 * 韩信 command 基于 flyway 4.0.3
 * @author ydzcowboy
 *
 */
public class Main {
    private static Log LOG;

    /**
     * The property name for the directory containing a list of jars to load on the classpath.
     */
    private static final String PROPERTY_JAR_DIRS = "flyway.jarDirs";

    /**
     * Initializes the logging.
     *
     * @param level The minimum level to log at.
     */
    static void initLogging(Level level) {
        LogFactory.setFallbackLogCreator(new ConsoleLogCreator(level));
        LOG = LogFactory.getLog(Main.class);
    }

    /**
     * Main method.
     *
     * @param args The command-line arguments.
     */
    @SuppressWarnings("unused")
	public static void main(String[] args) {
        Level logLevel = getLogLevel(args);
        initLogging(logLevel);

        try {
            printVersion();
            if (isPrintVersionAndExit(args)) {
                System.exit(0);
            }

            List<String> operations = determineOperations(args);
            if (operations.isEmpty()) {
                printUsage();
                return;
            }

            Properties properties = new Properties();
            initializeDefaults(properties);
            loadConfiguration(properties, args);
            overrideConfiguration(properties, args);        
            promptForCredentialsIfMissing(properties);
            dumpConfiguration(properties);
            loadJdbcDrivers();
            loadJavaMigrationsFromJarDirs(properties);
            //获取工程XML 配置信息
            List<Project> projectList = loadProjectConfig(properties);
            //套件工程
            List<Project> updateList = new ArrayList<Project>();
            Project suitPro = getSuitProject(projectList,updateList);
            //需要升级的版本
            List<Project> verList = getUpdateVerList(suitPro,updateList);
            Console console = System.console();
            //版本依赖检查
            setLastVersion(verList,properties);
            checkVersionNo(console,verList);
            //个性化脚本检查
            specialSqlCheck(console,verList);
            //是否进行自动发布jar 包等文件
            boolean autoDeploy = properties.getProperty("flyway.autoDeploy") == null ? true : Boolean.parseBoolean(properties.getProperty("flyway.autoDeploy").toString());
            String[] domainDirs = getDomains(autoDeploy,properties);
            //执行升级
            for(Project p : verList){
            	LOG.info("========================版本"+p.getName()+p.getVersion()+"升级开始============================");
            	String verTable = p.getVersionTable();
            	if(verTable == null || verTable.length() ==0){
            		throw new RuntimeException("版本["+p.getName()+p.getVersion()+"]project.xml 中未指定versionTable,请检查。");
            	}
            	String databasePath = p.getDatabasePath();
            	if(databasePath == null ||databasePath.length() ==0){
            		throw new RuntimeException("版本["+p.getName()+p.getVersion()+"]project.xml 中未指定databasePath,请检查。");
            	}   	
            	properties.put("flyway.table", verTable);
            	StringBuffer filesystem = new StringBuffer("classpath:db.migration");
            	
            	if(p.isFull()){//全量脚本
            		filesystem.append(",filesystem:").append(p.getInstall_path()).append("/").append(databasePath).append("/full");
            	}
            	//产品化增量脚本
            	filesystem.append(",filesystem:").append(p.getInstall_path()).append("/").append(databasePath).append("/update");
            	//个性化脚本
            	if(p.getCurRegion() != null && !p.getCurRegion().isEmpty()){
            		filesystem.append(",filesystem:").append(p.getInstall_path()).append("/").append(databasePath).append("/").append(p.getCurRegion());
            	}
            	
            	properties.put("flyway.locations",filesystem.toString());
                Flyway flyway = new Flyway();
                filterProperties(properties);
                flyway.configure(properties);
                //执行数据库操作
                for (String operation : operations) {
                    executeOperation(flyway, operation);
                }
                LOG.info("版本["+p.getName()+p.getVersion()+"]===数据库升级完成.");
                //客户端、服务端包升级
                if(autoDeploy){
                	deployServer(p,domainDirs,properties);
                }
                //插入版本日志信息
                insertGapVersion(p,properties);
                LOG.info("========================版本"+p.getName()+p.getVersion()+"升级成功============================");
            }
            if(suitPro != null){
            	insertGapVersion(suitPro,properties);
            	LOG.info("========================套件"+suitPro.getName()+suitPro.getVersion()+"升级成功============================");
            }
        } catch (Exception e) {
            if (logLevel == Level.DEBUG) {
                LOG.error("Unexpected error", e);
            } else {
                if (e instanceof FlywayException) {
                    LOG.error(e.getMessage());
                } else {
                    LOG.error(e.toString());
                }
            }
            System.exit(1);
        }
    }
    /**
     * 发布服务包
     * @param p
     * @param domainDirs
     * @param properties
     */
    private static void deployServer(Project p,String[] domainDirs,Properties properties){
        //升级版本
        String jdkVer = properties.getProperty("server.jdk") == null ? "jdk1.6" : properties.getProperty("server.jdk");
    	for(String d : domainDirs){
    		//执行清理文件
    		if(p.getCleanup() != null && p.getCleanup().size() > 0){
    			cleanUpFile(p,d);
    		}
    		//后台服务包
    		if(p.getServerPath() != null && !p.getServerPath().equals("")){
    			Tools.copyFileFromDir(d, p.getInstall_path()+"/"+p.getServerPath()+"/"+jdkVer);
    		}else{
    			LOG.debug("发布工程服务端路径未指定，不进行服务端包发布");
    		}
    		//客户端DLL包
    		if(p.getClientPath() != null && !p.getClientPath().equals("")){
    			Tools.copyFileFromDir(d+"/"+p.getContextName()+"/update", p.getInstall_path()+"/"+p.getClientPath());
    		}else{
    			LOG.debug("发布工程客户端路径未指定，不进行客户端包发布");
    		}
    	}
    }
    /**
     * 获取发布服务路径
     * @param autoDeploy
     * @param properties
     * @return
     */
    private static String[] getDomains(boolean autoDeploy,Properties properties){
    	String[] domainDirs = null;
        if(autoDeploy){
            //获取domain 目录，即工程发布根目录
            String domainPath = properties.getProperty("domain.path");
            if(domainPath != null && domainPath.length()>0){
            	domainDirs = StringUtils.tokenizeToStringArray(domainPath, ";");
            	for(String p : domainDirs){
            		File f = new File(p);
            		if(f == null || !f.isDirectory()){
            			throw new RuntimeException("参数[domain.path]指定目录："+p+"不存在，请检查配置。");
            		}
            	}
            }else{
            	throw new RuntimeException("未指定 参数[domain.path],请检查配置，或改为手动发布。");
            }               
        }
        return domainDirs;
    }
    
    /**
     * 检查版本号，判断是全量还是增量升级
     * @param console
     * @param verList
     */
    private static void checkVersionNo(Console console,List<Project> verList){
        List<String> errMsgLs = new ArrayList<String>();
        for(Project p : verList){
        	String errMsg = p.checkVersion();
        	if((p.getCurrentVerNo() == null || p.getCurrentVerNo().isEmpty()) && console != null){//当前数据库未找到版本记录，判断是否全量安装
        		String inputChar = "";
        		LOG.warn("数据库中，未找到项目["+p.getName()+"]的任何版本信息,是否进行全量安装？（Y/N）");
        		while(!inputChar.equalsIgnoreCase("Y") && !inputChar.equalsIgnoreCase("N") ){
        			inputChar = console.readLine("Y 进行全量安装 ，N 取消本次安装：");
        		}
        		if(inputChar.equalsIgnoreCase("Y")){
        			LOG.debug("项目["+p.getName()+"]被要求执行全量升级");
        			p.setFull(true);
        		}else{
        			LOG.error("本次安装被被取消");
        			System.exit(1);
        		}    		
        	}else if(errMsg.length() > 0){
        		errMsgLs.add(errMsg);
        	}
        }
        if(errMsgLs.size() > 0){
        	for(String s : errMsgLs){
        		LOG.error(s);
        	}
        	System.exit(1);
        }
    }
    
    private static Project getSuitProject(List<Project> projectList,List<Project> updateList){
    	Project suitPro = null;
        for(Project p : projectList){
        	if(p.isSuit()){
        		if(suitPro == null){
        			suitPro = p;
        		}else{
        			throw new RuntimeException("本次升级发现两个套件：["+suitPro.getName()+suitPro.getDescription()+","+p.getName()+p.getDescription()+"]");
        		}
        	}else{
        		updateList.add(p);
        	}
        }
        return suitPro;
    }
    
    /**
     * 获取待升级项目
     * @param suitPro
     * @param updateList
     * @return
     */
    private static List<Project> getUpdateVerList(Project suitPro,List<Project> updateList){
        List<Project> verList = new ArrayList<Project>();
        if(suitPro == null && updateList.size() > 1){
        	throw new RuntimeException("本次升级，非套件版本中发现多个升级版本。");
        }
        //检查套件合法性
        if(suitPro != null){
        	List<VerInfo> verInfoList = suitPro.getSuit();
        	if(verInfoList == null || verInfoList.isEmpty()){
        		throw new RuntimeException("套件["+suitPro.getName()+"]工程文件，未配置升级版本信息。");
        	}
        	for(VerInfo v : verInfoList){
        		boolean isExist = false;
        		for(Project p : updateList){
        			if(v.getName().equals(p.getName()) && v.getVersion().equals(p.getVersion())){
        				verList.add(p);
        				isExist = true;
        				break;
        			}
        		}
        		if(!isExist){
        			throw new RuntimeException("套件["+suitPro.getName()+"]指定升级版本["+v.getName()+v.getVersion()+"],在套件工程目录下未找到。");
        		}
        	}
        	LOG.info("========================套件"+suitPro.getName()+suitPro.getVersion()+"升级开始============================");
        }else{
        	verList.add(updateList.get(0));
        }
        return verList;
    }
    /**
     * 个性化脚本检查
     * @param console
     * @param verList 待升级工程
     */
    private static void specialSqlCheck(Console console,List<Project> verList){
        for(Project p : verList){
        	List<String> rgList = p.getSpecialRegion();
        	Map<String,String> rgMap = new HashMap<String,String>();
        	if(rgList != null && rgList.size() > 0){
        		LOG.info("工程：["+p.getName()+"]存在个性化脚本，请选择：");
        		for(String rg : rgList){
        			LOG.info("===>"+rg);
        			String[] rgArr = rg.split("-");
        			rgMap.put(rgArr[0], rgArr[1]);
        		}
        		String inputChar = "0";
        		if(console != null){    		
            		while(!rgMap.containsKey(inputChar)){
            			inputChar = console.readLine("请选择您对应地区序号：");
            		}
        		}
        		if(!inputChar.equals("0")){
        			p.setCurRegion(rgMap.get(inputChar));  
        		}	  		
        	}
        }
    }
    /**
     * 设置项目最新一个版本的版本号
     * @param p
     * @param properties
     * @return
     */
    private static void setLastVersion(List<Project> projectList,Properties properties)
    {
    	Connection con = Tools.getJdbcConnection(properties.getProperty("flyway.url"), properties.getProperty("flyway.user"), properties.getProperty("flyway.password"));
    	if(con != null){
   		 StringBuffer querySql = new StringBuffer("select sys_id,sys_code,version_no from  gap_version where is_now = 1 and (");
   		 for(Project p : projectList){
   			 querySql.append("(sys_id="+p.getSysId()+" and sys_code='"+p.getName()+"') or ");
   		 }
   		 querySql.delete(querySql.length()-3, querySql.length());
   		 querySql.append(")  order by version_no desc");
   		 
   		 Statement st = null;
   		 ResultSet rs = null;
   		 try {
				st = con.createStatement();
				rs = st.executeQuery(querySql.toString());
				while(rs.next()){
					long sysId = rs.getLong("sys_id");
					String sysCode = rs.getString("sys_code");
					String versionNo = rs.getString("version_no");
					for(Project p : projectList){
						if(p.getSysId() == sysId && p.getName().equals(sysCode)){
							p.setCurrentVerNo(versionNo);
						}
					}
				}	
			} catch (SQLException e) {
				throw new RuntimeException("查询版本信息出错："+e.getMessage());
			}finally{
				try {
					rs.close();
					st.close();
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
    	}
    }
    
    /**
     * 保存版本信息
     * @param p
     * @param properties
     */
    private static void insertGapVersion(Project p,Properties properties){
    	Connection con = Tools.getJdbcConnection(properties.getProperty("flyway.url"), properties.getProperty("flyway.user"), properties.getProperty("flyway.password"));
    	if(con != null){
    		 String updateSql = "update gap_version set is_now=0 where sys_id="+p.getSysId()+" and sys_code='"+p.getName()+"'";
    		 String insertSql = "insert into gap_version(sys_id,sys_code,version_no,update_date,is_now) values ("+p.getSysId()+",'"+p.getName()+"','"+p.getName()+p.getVersion()+"',sysdate,1)";
    		 Statement st = null;
    		 try {
				st = con.createStatement();
				st.addBatch(updateSql);
				st.addBatch(insertSql);
				st.executeBatch();
			} catch (SQLException e) {
				LOG.error("执行版本插入异常："+e.getMessage());
				e.printStackTrace();
			}finally{
				try {
					st.close();
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
    		LOG.debug("更新版本："+p.getName()+p.getVersion());
    	}
    }
    /**
     * 執行文件清理
     * @param p  工程配置
     * @param d  发布路径 
     */
    private static void cleanUpFile(Project p,String d){
		for(String cl : p.getCleanup()){
			String abPath = d+"/"+p.getContextName()+"/"+cl;
			File f = new File(abPath);
			if(f.isFile() || f.isDirectory()){
				f.delete();
				LOG.info("清理文件："+f.getAbsolutePath());
			}else{
				if(cl.indexOf("*")>-1){//判断是否含统配符
					String[] clArr = abPath.split("/");
					String parentPath = "";
					String fileName = "";
					for(int i = 0 ;i<clArr.length;i++){
						if(i == clArr.length -1){
							fileName = clArr[i];
						}else{
							parentPath += clArr[i]+"/";
						}
					}
					File parentFile = new File(parentPath);
					if(!parentFile.isDirectory()){
						LOG.debug("未找到需要清理的文件："+parentPath);
						continue;
					}
					String[] temp = fileName.split("\\*");
					if(temp.length == 0){
						parentFile.delete();
						LOG.info("清理文件："+parentFile.getAbsolutePath());
					}else if(temp.length == 2){
						File[] childFile = parentFile.listFiles();
						if(childFile.length > 0){
							for(File cf : childFile){
								if(cf.getName().startsWith(temp[0]) && cf.getName().endsWith(temp[1])){
									cf.delete();
									LOG.info("清理文件："+cf.getAbsolutePath());
								}
							}
						}
					}else{
						LOG.error("通配符使用錯誤："+cl);
					}
				}else{
					LOG.debug("未找到需要清理的文件："+d+"/"+p.getContextName()+"/"+cl);
				}
			}
			
		}
    }

    private static boolean isPrintVersionAndExit(String[] args) {
        for (String arg : args) {
            if ("-v".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes this operation on this Flyway instance.
     *
     * @param flyway    The Flyway instance.
     * @param operation The operation to execute.
     */
    private static void executeOperation(Flyway flyway, String operation) {
        if ("clean".equals(operation)) {
        	 LOG.error("错误操作: 清空数据库，风险较大，暂停使用" + operation);
             printUsage();
             System.exit(1);
//            flyway.clean();
        } else if ("baseline".equals(operation)) {
            flyway.baseline();
        } else if ("migrate".equals(operation)) {
            flyway.migrate();
        } else if ("validate".equals(operation)) {
            flyway.validate();
        } else if ("info".equals(operation)) {
            LOG.info("\n" + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));
        } else if ("repair".equals(operation)) {
            flyway.repair();
        } else if("init".equals(operation)){
        	//TODO 进行年度初始化工作，初始化年度，清理业务数据
        	
        }else {
            LOG.error("Invalid operation: " + operation);
            printUsage();
            System.exit(1);
        }
    }

    /**
     * Checks the desired log level.
     *
     * @param args The command-line arguments.
     * @return The desired log level.
     */
    private static Level getLogLevel(String[] args) {
        for (String arg : args) {
            if ("-x".equals(arg)) {
                return Level.DEBUG;
            }
            if ("-q".equals(arg)) {
                return Level.WARN;
            }
        }
        return Level.INFO;
    }

    /**
     * Initializes the properties with the default configuration for the command-line tool.
     *
     * @param properties The properties object to initialize.
     */
    private static void initializeDefaults(Properties properties) {
        properties.put("flyway.locations", "filesystem:" + new File(getInstallationDir(), "sql").getAbsolutePath());
        properties.put(PROPERTY_JAR_DIRS, new File(getInstallationDir(), "jars").getAbsolutePath());
        //由于存在全量和增量升级脚本的不同，所以不进行脚本版本依赖校验
    	properties.put("flyway.validateOnMigrate", false);
    }

    /**
     * Filters there properties to remove the Flyway Commandline-specific ones.
     *
     * @param properties The properties to filter.
     */
    private static void filterProperties(Properties properties) {
        properties.remove(PROPERTY_JAR_DIRS);
        properties.remove("flyway.configFile");
        properties.remove("flyway.configFileEncoding");
    }

    /**
     * Prints the version number on the console.
     *
     * @throws IOException when the version could not be read.
     */
    private static void printVersion() throws IOException {
        VersionPrinter.printVersion();
        LOG.info("");

        LOG.debug("Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        LOG.debug(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + "\n");
    }

    /**
     * Prints the usage instructions on the console.
     */
    private static void printUsage() {
        LOG.info("Usage");
        LOG.info("=====");
        LOG.info("");
        LOG.info("hxaow [options] command");
        LOG.info("");
        LOG.info("By default, the configuration will be read from conf/hxaow.conf.");
        LOG.info("Options passed from the command-line override the configuration.");
        LOG.info("");
        LOG.info("Commands");
        LOG.info("--------");
        LOG.info("init  : 进行数据库年度初始化");
        LOG.info("migrate  : 进行数据库升级");
        LOG.info("clean    : 清空数据库，风险较大，暂停使用");
        LOG.info("info     : 查看升级信息");
        LOG.info("validate : 校验升级版本依赖");
        LOG.info("baseline : 基于基线版本形成基线迁移");
        LOG.info("repair   : 修复升级脚本日志信息");
        LOG.info("");
        LOG.info("Options (Format: -key=value)");
        LOG.info("-------");
        LOG.info("driver                       : Fully qualified classname of the jdbc driver");
        LOG.info("url                          : Jdbc url to use to connect to the database");
        LOG.info("user                         : User to use to connect to the database");
        LOG.info("password                     : Password to use to connect to the database");
        LOG.info("schemas                      : Comma-separated list of the schemas managed by Flyway");
        LOG.info("table                        : Name of Flyway's metadata table");
        LOG.info("locations                    : Classpath locations to scan recursively for migrations");
        LOG.info("resolvers                    : Comma-separated list of custom MigrationResolvers");
        LOG.info("skipDefaultResolvers         : Skips default resolvers (jdbc, sql and Spring-jdbc)");
        LOG.info("sqlMigrationPrefix           : File name prefix for sql migrations");
        LOG.info("repeatableSqlMigrationPrefix : File name prefix for repeatable sql migrations");
        LOG.info("sqlMigrationSeparator        : File name separator for sql migrations");
        LOG.info("sqlMigrationSuffix           : File name suffix for sql migrations");
        LOG.info("encoding                     : Encoding of sql migrations");
        LOG.info("placeholderReplacement       : Whether placeholders should be replaced");
        LOG.info("placeholders                 : Placeholders to replace in sql migrations");
        LOG.info("placeholderPrefix            : Prefix of every placeholder");
        LOG.info("placeholderSuffix            : Suffix of every placeholder");
        LOG.info("target                       : Target version up to which Flyway should use migrations");
        LOG.info("outOfOrder                   : Allows migrations to be run \"out of order\"");
        LOG.info("callbacks                    : Comma-separated list of FlywayCallback classes");
        LOG.info("skipDefaultCallbacks         : Skips default callbacks (sql)");
        LOG.info("validateOnMigrate            : Validate when running migrate");
        LOG.info("ignoreFutureMigrations       : Allow future migrations when validating");
        LOG.info("cleanOnValidationError       : Automatically clean on a validation error");
        LOG.info("cleanDisabled                : Whether to disable clean");
        LOG.info("baselineVersion              : Version to tag schema with when executing baseline");
        LOG.info("baselineDescription          : Description to tag schema with when executing baseline");
        LOG.info("baselineOnMigrate            : Baseline on migrate against uninitialized non-empty schema");
        LOG.info("configFile                   : Config file to use (default: conf/flyway.properties)");
        LOG.info("configFileEncoding           : Encoding of the config file (default: UTF-8)");
        LOG.info("jarDirs                      : Dirs for Jdbc drivers & Java migrations (default: jars)");
        LOG.info("");
        LOG.info("Add -x to print debug output");
        LOG.info("Add -q to suppress all output, except for errors and warnings");
        LOG.info("Add -v to print the hxoawh version and exit");
        LOG.info("");
        LOG.info("Example");
        LOG.info("-------");
        LOG.info("flyway -user=myuser -password=s3cr3t -url=jdbc:h2:mem -placeholders.abc=def migrate");
        LOG.info("");
        LOG.info("More info at https://flywaydb.org/documentation/commandline");
    }
    /**
     * 加载升级工程文件
     * @throws IOException
     */
    private static List<Project> loadProjectConfig(Properties properties) throws IOException {
    	String updateDir = (String)properties.getProperty("update_dir");
    	File path = null;
    	if(updateDir!= null && !updateDir.isEmpty()){
    		path = new File(getInstallationDir(), "update/"+updateDir);
    	}else{
    		path = new File(getInstallationDir(), "update");
    	}
        List<Project> projectList = Tools.LoadProjectConfig(path);
        if(projectList.isEmpty()){
            throw new RuntimeException("在升级目录：["+path.getAbsolutePath() +"]及子目录下，未找到工程文件project.xml,请检查。");
        }
        return projectList;
    }


    /**
     * Loads all the driver jars contained in the drivers folder. (For Jdbc drivers)
     *
     * @throws IOException When the jars could not be loaded.
     */
    private static void loadJdbcDrivers() throws IOException {
        File driversDir = new File(getInstallationDir(), "drivers");
        File[] files = driversDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        // see javadoc of listFiles(): null if given path is not a real directory
        if (files == null) {
            LOG.error("Directory for Jdbc Drivers not found: " + driversDir.getAbsolutePath());
            System.exit(1);
        }

        for (File file : files) {
            addJarOrDirectoryToClasspath(file.getPath());
        }
    }

    /**
     * Loads all the jars contained in the jars folder. (For Java Migrations)
     *
     * @param properties The configured properties.
     * @throws IOException When the jars could not be loaded.
     */
    private static void loadJavaMigrationsFromJarDirs(Properties properties) throws IOException {
        String jarDirs = properties.getProperty(PROPERTY_JAR_DIRS);
        if (!StringUtils.hasLength(jarDirs)) {
            return;
        }

        jarDirs = jarDirs.replace(File.pathSeparator, ",");
        String[] dirs = StringUtils.tokenizeToStringArray(jarDirs, ",");

        for (String dirName : dirs) {
            File dir = new File(dirName);
            File[] files = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });

            // see javadoc of listFiles(): null if given path is not a real directory
            if (files == null) {
                LOG.error("Directory for Java Migrations not found: " + dirName);
                System.exit(1);
            }

            for (File file : files) {
                addJarOrDirectoryToClasspath(file.getPath());
            }
        }
    }

    /**
     * Adds a jar or a directory with this name to the classpath.
     *
     * @param name The name of the jar or directory to add.
     * @throws IOException when the jar or directory could not be found.
     */
    /* private -> for testing */
    static void addJarOrDirectoryToClasspath(String name) throws IOException {
        LOG.debug("Adding location to classpath: " + name);

        try {
            URL url = new File(name).toURI().toURL();
            URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysloader, url);
        } catch (Exception e) {
            throw new FlywayException("Unable to load " + name, e);
        }
    }

    /**
     * 加载配置文件
     * @param properties
     * @param args
     */
    static void loadConfiguration(Properties properties, String[] args) {
        String encoding = determineConfigurationFileEncoding(args);

        loadConfigurationFile(properties, getInstallationDir() + "/conf/hxaow.conf", encoding, false);
        loadConfigurationFile(properties, System.getProperty("user.home") + "/hxaow.conf", encoding, false);
        loadConfigurationFile(properties, "hxaow.conf", encoding, false);

        String configFile = determineConfigurationFileArgument(args);
        if (configFile != null) {
            loadConfigurationFile(properties, configFile, encoding, true);
        }
    }

    /**
     * Loads the configuration from the configuration file. If a configuration file is specified using the -configfile
     * argument it will be used, otherwise the default config file (conf/flyway.properties) will be loaded.
     *
     * @param properties    The properties object to load to configuration into.
     * @param file          The configuration file to load.
     * @param encoding      The encoding of the configuration file.
     * @param failIfMissing Whether to fail if the file is missing.
     * @return Whether the file was loaded successfully.
     * @throws FlywayException when the configuration file could not be loaded.
     */
    private static boolean loadConfigurationFile(Properties properties, String file, String encoding, boolean failIfMissing) throws FlywayException {
        File configFile = new File(file);
        String errorMessage = "Unable to load config file: " + configFile.getAbsolutePath();

        if (!configFile.isFile() || !configFile.canRead()) {
            if (!failIfMissing) {
                LOG.debug(errorMessage);
                return false;
            }
            throw new FlywayException(errorMessage);
        }

        LOG.debug("Loading config file: " + configFile.getAbsolutePath());
        try {
            String contents = FileCopyUtils.copyToString(new InputStreamReader(new FileInputStream(configFile), encoding));
            properties.load(new StringReader(contents.replace("\\", "\\\\")));
            return true;
        } catch (IOException e) {
            throw new FlywayException(errorMessage, e);
        }
    }

    /**
     * If no user or password has been provided, prompt for it. If you want to avoid the prompt,
     * pass in an empty user or password.
     *
     * @param properties The properties object to load to configuration into.
     */
    private static void promptForCredentialsIfMissing(Properties properties) {
        Console console = System.console();
        if (console == null) {
            // We are running in an automated build. Prompting is not possible.
            return;
        }

        if (!properties.containsKey("flyway.url")) {
            // URL is not set. We are doomed for failure anyway.
            return;
        }
        
//        if(!properties.containsKey("update_dir")){
//        	properties.put("update_dir", console.readLine("请输入update文件加下的升级版本："));
//        }

        if (!properties.containsKey("flyway.user")) {
            properties.put("flyway.user", console.readLine("Database user: "));
        }

        if (!properties.containsKey("flyway.password")) {
            char[] password = console.readPassword("Database password: ");
            properties.put("flyway.password", password == null ? "" : String.valueOf(password));
        }
        
        if (!properties.containsKey("domain.path")) {
            properties.put("domain.path", console.readLine("升级服务domain路径,如：D:\\bea103\\user_projects\\domains\\domain_8001\\servers:"));
        }
    }

    /**
     * Dumps the configuration to the console when debug output is activated.
     *
     * @param properties The configured properties.
     */
    private static void dumpConfiguration(Properties properties) {
        LOG.debug("Using configuration:");
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String value = entry.getValue().toString();
            value = "flyway.password".equals(entry.getKey()) ? StringUtils.trimOrPad("", value.length(), '*') : value;
            LOG.debug(entry.getKey() + " -> " + value);
        }
    }

    /**
     * Determines the file to use for loading the configuration.
     *
     * @param args The command-line arguments passed in.
     * @return The path of the configuration file on disk.
     */
    private static String determineConfigurationFileArgument(String[] args) {
        for (String arg : args) {
            if (isPropertyArgument(arg) && "configFile".equals(getArgumentProperty(arg))) {
                return getArgumentValue(arg);
            }
        }

        return null;
    }

    /**
     * @return The installation directory of the Flyway Command-line tool.
     */
    @SuppressWarnings("ConstantConditions")
    private static String getInstallationDir() {
        String path = ClassUtils.getLocationOnDisk(Main.class);
        return new File(path).getParentFile().getParentFile().getAbsolutePath();
    }

    /**
     * Determines the encoding to use for loading the configuration.
     *
     * @param args The command-line arguments passed in.
     * @return The encoding. (default: UTF-8)
     */
    private static String determineConfigurationFileEncoding(String[] args) {
        for (String arg : args) {
            if (isPropertyArgument(arg) && "configFileEncoding".equals(getArgumentProperty(arg))) {
                return getArgumentValue(arg);
            }
        }

        return "UTF-8";
    }

    /**
     * 使用命令参数重置参数
     * @param properties
     * @param args
     */
    static void overrideConfiguration(Properties properties, String[] args) {
        for (String arg : args) {
            if (isPropertyArgument(arg)) {
                properties.put("flyway." + getArgumentProperty(arg), getArgumentValue(arg));
            }
        }
    }
    /**
     * 恢复项目XML中信息
     * @param properties
     */
    static void overrideConfigByXMl(Properties properties){
    	
    }

    /**
     * Checks whether this command-line argument tries to set a property.
     *
     * @param arg The command-line argument to check.
     * @return {@code true} if it does, {@code false} if not.
     */
    /* private -> for testing*/
    static boolean isPropertyArgument(String arg) {
        return arg.startsWith("-") && arg.contains("=");
    }

    /**
     * Retrieves the property this command-line argument tries to assign.
     *
     * @param arg The command-line argument to check, typically in the form -key=value.
     * @return The property.
     */
    /* private -> for testing*/
    static String getArgumentProperty(String arg) {
        int index = arg.indexOf("=");

        return arg.substring(1, index);
    }

    /**
     * Retrieves the value this command-line argument tries to assign.
     *
     * @param arg The command-line argument to check, typically in the form -key=value.
     * @return The value or an empty string if no value is assigned.
     */
    /* private -> for testing*/
    static String getArgumentValue(String arg) {
        int index = arg.indexOf("=");

        if ((index < 0) || (index == arg.length())) {
            return "";
        }

        return arg.substring(index + 1);
    }

    /**
     * Determine the operations Flyway should execute.
     *
     * @param args The command-line arguments passed in.
     * @return The operations. An empty list if none.
     */
    private static List<String> determineOperations(String[] args) {
        List<String> operations = new ArrayList<String>();

        for (String arg : args) {
            if (!arg.startsWith("-")) {
                operations.add(arg);
            }
        }

        return operations;
    }
   
}
