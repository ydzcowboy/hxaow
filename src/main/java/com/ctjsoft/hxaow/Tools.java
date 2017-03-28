package com.ctjsoft.hxaow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.internal.util.DateUtils;
import org.flywaydb.core.internal.util.StringUtils;
import org.flywaydb.core.internal.util.logging.Log;
import org.flywaydb.core.internal.util.logging.LogFactory;

import com.ctjsoft.hxaow.bo.Project;
import com.ctjsoft.hxaow.bo.VerInfo;
import com.thoughtworks.xstream.XStream;
/**
 * 工具类
 * @author ydzcowboy
 *
 */
public class Tools {

    private static Log LOG = LogFactory.getLog(Tools.class);
	/**
	 * 读取工程配置文件
	 * @param path
	 * @return
	 */
    public static Project readXmlConfig(String path){
    	Project pro = null;
        File file=new File(path);
        if(!file.isFile()){
        	throw new RuntimeException("未找到文件："+file.getAbsolutePath());
        }
        try {
            FileInputStream in=new FileInputStream(file);
	    	XStream xstream = new XStream(); 
	    	xstream.alias("Project", Project.class);
	    	xstream.alias("VerInfo", VerInfo.class);  	
	    	pro = (Project)xstream.fromXML(in);
	    	pro.setInstall_path(file.getParent());
        } catch (IOException e) {
        	e.printStackTrace(); 
        }
        return pro;
    }
    /**
     * 加载指定目录下的所有工程文件
     * @param dir
     * @return
     */
    public static List<Project> LoadProjectConfig(File file){
    	List<Project> projectList = new ArrayList<Project>();
    	if(file == null || !file.isDirectory())
    		return projectList;
    	File[] files = file.listFiles();
    	for(File f : files){
        	if(f.isDirectory()){
        		projectList.addAll(LoadProjectConfig(f));
        	}else{
        		if(f.getName().equals("project.xml")){
        			projectList.add(readXmlConfig(f.getAbsolutePath()));
        		}
        	}
    	}
    	return projectList;
    }
    
    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     * @param dir 将要删除的文件目录
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }
    
    
    /**
     * 生成配置XML
     * @param pro
     * @return
     */
    public static String configToXML(Project pro){
    	String xml;
    	XStream xstream = new XStream(); 
    	xstream.alias("Project", Project.class);
    	xstream.alias("VerInfo", VerInfo.class);  	
    	xml = xstream.toXML(pro);
    	return xml;
    }
    

    /**
     * 复制目录下的文件（不包括该目录）到指定目录，会连同子目录一起复制过去。  
     * @param toPath
     * @param fromPath
     */
    public static void copyFileFromDir(String toPath, String fromPath) {  
        File file = new File(fromPath);  
        createFile(toPath, false);// true:创建文件 false创建目录  
        if (file.isDirectory()) {// 如果是目录  
            copyFileToDir(toPath, listFile(file));  
        }else{
        	LOG.error("目录："+fromPath+"不存在，未能拷贝到指定目录："+toPath+"，请检查配置。");
        }  
    }  

    /**
     * 复制目录到指定目录,将目录以及目录下的文件和子目录全部复制到目标目录  
     * @param toPath
     * @param fromPath
     */
    public static void copyDir(String toPath, String fromPath) {  
        File targetFile = new File(toPath);// 创建文件  
        createFile(targetFile, false);// 创建目录  
        File file = new File(fromPath);// 创建文件  
        if (targetFile.isDirectory() && file.isDirectory()) {// 如果传入是目录  
            copyFileToDir(targetFile.getAbsolutePath() + "/" + file.getName(),  
                    listFile(file));// 复制文件到指定目录  
        }  
    }
  
    /**
     * 复制一组文件到指定目录。targetDir是目标目录，filePath是需要复制的文件路径  
     * @param toDir
     * @param filePath
     */
    public static void copyFileToDir(String toDir, String[] filePath) {  
        if (toDir == null || "".equals(toDir)) {// 目录路径为空  
        	LOG.error("参数错误，目标路径不能为空");  
            return;  
        }  
        File targetFile = new File(toDir);  
        if (!targetFile.exists()) {// 如果指定目录不存在  
            targetFile.mkdir();// 新建目录  
        } else {  
            if (!targetFile.isDirectory()) {// 如果不是目录  
            	LOG.error("参数错误，目标路径指向的不是一个目录！");  
                return;  
            }  
        }  
        for (int i = 0; i < filePath.length; i++) {// 遍历需要复制的文件路径  
            File file = new File(filePath[i]);// 创建文件  
            if (file.isDirectory()) {// 判断是否是目录  
                copyFileToDir(toDir + "/" + file.getName(), listFile(file));// 递归调用方法获得目录下的文件  
            } else {  
                copyFileToDir(toDir, file, "");// 复制文件到指定目录  
            }  
        }  
  }  
  
    public static void copyFileToDir(String toDir, File file, String newName) {// 复制文件到指定目录  
        String newFile = "";  
        if (newName != null && !"".equals(newName)) {  
            newFile = toDir + "/" + newName;  
        } else {  
            newFile = toDir + "/" + file.getName();  
        }  
        File tFile = new File(newFile);  
        copyFile(tFile, file);// 调用方法复制文件  
    }  
  
    public static void copyFile(File toFile, File fromFile) {// 复制文件  
        if (toFile.exists()) {// 判断目标目录中文件是否存在  
        	toFile.delete();
        	LOG.debug("文件:" + toFile.getAbsolutePath() + "被替换");  
        }
        createFile(toFile, true);// 创建文件  
        
        LOG.info("复制文件:" + fromFile.getName() + "到"  
                + toFile.getAbsolutePath());  
        try {  
            InputStream is = new FileInputStream(fromFile);// 创建文件输入流  
            FileOutputStream fos = new FileOutputStream(toFile);// 文件输出流  
            byte[] buffer = new byte[1024];// 字节数组  
            while (is.read(buffer) != -1) {// 将文件内容写到文件中  
                fos.write(buffer);  
            }  
            is.close();// 输入流关闭  
            fos.close();// 输出流关闭  
        } catch (FileNotFoundException e) {// 捕获文件不存在异常  
            e.printStackTrace();  
        } catch (IOException e) {// 捕获异常  
            e.printStackTrace();  
        }  
    }  
  
     public static String[] listFile(File dir) {// 获取文件绝对路径  
        String absolutPath = dir.getAbsolutePath();// 声获字符串赋值为路传入文件的路径  
        String[] paths = dir.list();// 文件名数组  
        String[] files = new String[paths.length];// 声明字符串数组，长度为传入文件的个数  
        for (int i = 0; i < paths.length; i++) {// 遍历显示文件绝对路径  
            files[i] = absolutPath + "/" + paths[i];  
        }  
        return files;  
    }  
  
    public static void createFile(String path, boolean isFile) {// 创建文件或目录  
        createFile(new File(path), isFile);// 调用方法创建新文件或目录  
    }  
  
    public static void createFile(File file, boolean isFile) {// 创建文件  
        if (!file.exists()) {// 如果文件不存在  
            if (!file.getParentFile().exists()) {// 如果文件父目录不存在  
                createFile(file.getParentFile(), false);  
                createFile(file,isFile);
            } else {// 存在文件父目录  
                if (isFile) {// 创建文件  
                    try {  
                        file.createNewFile();// 创建新文件  
                    } catch (IOException e) {  
                        e.printStackTrace();  
                    }  
                } else {  
                    file.mkdir();// 创建目录  
                }  
            }  
        }  
    } 
    /**
     * 获取数据库连接
     * @param url
     * @param user
     * @param passwork
     * @return
     */
    public static Connection getJdbcConnection(String url,String user,String password){
    	Connection con = null;
    	try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			con = DriverManager.getConnection(url, user, password);
		} catch (Exception e) {
			throw new RuntimeException("获取数据库连接失败："+e.getMessage());
		}
    	return con;
    }
    /**
     * 获取版本信息
     * @param proList
     * @return
     */
	public static String versionInfo(List<Map> proList) {
		int sysCodeWidth = "SYS_CODE".length();
		int versionNoWidth = "VERSION_NO".length();
		int i = proList.size();
		for (int j = 0; j < i; j++) {
			Map migrationInfo = proList.get(j);
			sysCodeWidth = Math.max(sysCodeWidth,
					migrationInfo.get("sys_code") != null ? migrationInfo.get("sys_code").toString().length() : 0);
			versionNoWidth = Math.max(versionNoWidth, migrationInfo.get("version_no").toString().length());
		}

		String ruler = (new StringBuilder()).append("+-")
				.append(StringUtils.trimOrPad("", sysCodeWidth, '-'))
				.append("-+-")
				.append(StringUtils.trimOrPad("", versionNoWidth, '-'))
				.append("-+---------------------+---------+\n").toString();
		StringBuilder table = new StringBuilder();
		table.append(ruler);
		table.append("| ")
				.append(StringUtils.trimOrPad("SYS_CODE", sysCodeWidth, ' '))
				.append(" | ")
				.append(StringUtils.trimOrPad("VERSION_NO", versionNoWidth))
				.append(" | Installed on        | State   |\n");
		table.append(ruler);
		if (proList.size() == 0) {
			table.append(
					StringUtils.trimOrPad("| No migrations found",
							ruler.length() - 2, ' ')).append("|\n");
		} else {
			int k = proList.size();
			for (int l = 0; l < k; l++) {
				Map migrationInfo = proList.get(l);
				String versionStr = migrationInfo.get("sys_code")!= null ? migrationInfo.get("sys_code").toString() : "";
				table.append("| ").append(
						StringUtils.trimOrPad(versionStr, sysCodeWidth));
				table.append(" | ").append(
						StringUtils.trimOrPad(migrationInfo.get("version_no").toString(),
								versionNoWidth));
				table.append(" | ").append(
						StringUtils.trimOrPad(DateUtils
								.formatDateAsIsoString((Date)migrationInfo.get("update_date")), 19));
				table.append(" | ").append(
						StringUtils.trimOrPad("OK", 7));
				table.append(" |\n");
			}
		}
		table.append(ruler);
		return table.toString();
	}
      
}
