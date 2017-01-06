package com.ctjsoft.hxaow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ctjsoft.hxaow.bo.Project;
import com.ctjsoft.hxaow.bo.VerInfo;
import com.thoughtworks.xstream.XStream;
/**
 * 工具类
 * @author ydzcowboy
 *
 */
public class Tools {

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
}
