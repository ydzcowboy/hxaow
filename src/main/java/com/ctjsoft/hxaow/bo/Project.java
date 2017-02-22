package com.ctjsoft.hxaow.bo;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 升级工程信息
 * @author ydzcowboy
 *
 */
@XmlRootElement
public class Project implements Cloneable{
	//工程名称
	String name;
	//子系统ID
	long sysId;
	//描述
	String description;
	//当前版本
	String version;
	//脚本升级版本日志表,记录脚本依赖关系
	String versionTable;
	//基线版本
	String baseversion;
	//需要清除的文件
	List<String> cleanup;
	//需要替换的服务器包路径
	String serverPath;
	//客户端包路径
	String clientPath;
	//脚本路径
	String databasePath;
	//套件工程信息 key : name,version
	List<VerInfo> suit;
	// 含个性化脚本的地区，个性化脚本，需按此创建文件夹
	List<String> specialRegion;
	// 当前选择地区
	String curRegion = "";
	
	//工程安装目录
	String install_path = "";
	//工程部署名
	String contextName = "";
	//当前版本号
	String currentVerNo = "";
	//是否全量升级
	boolean isFull = false;
	//是否涉及常态库，默认为false,如果涉及，脚本结果需要分[常态库]、[年度库] 进行存放
	boolean isPmDb = false;
	

	public boolean getIsPmDb() {
		return isPmDb;
	}

	public void setIsPmDb(boolean isPmDb) {
		this.isPmDb = isPmDb;
	}

	public String getCurRegion() {
		return curRegion;
	}

	public void setCurRegion(String curRegion) {
		this.curRegion = curRegion;
	}

	public boolean isFull() {
		return isFull;
	}

	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}

	public String getCurrentVerNo() {
		return currentVerNo;
	}

	public void setCurrentVerNo(String currentVerNo) {
		this.currentVerNo = currentVerNo;
	}

	public String getContextName() {
		return contextName;
	}

	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	public String getInstall_path() {
		return install_path;
	}

	public void setInstall_path(String install_path) {
		this.install_path = install_path;
	}

	//判断是否为套件工程
	public boolean isSuit()
	{
		if(suit != null && suit.size() > 0)
			return true;
		return false;
	}
	
	public List<String> getSpecialRegion() {
		return specialRegion;
	}
	public void setSpecialRegion(List<String> special_region) {
		this.specialRegion = special_region;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getSysId() {
		return sysId;
	}
	public void setSysId(long sys_id) {
		this.sysId = sys_id;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getVersionTable() {
		return versionTable;
	}
	public void setVersionTable(String version_table) {
		this.versionTable = version_table;
	}
	public String getBaseversion() {
		return baseversion;
	}
	public void setBaseversion(String baseversion) {
		this.baseversion = baseversion;
	}
	public List<String> getCleanup() {
		return cleanup;
	}
	public void setCleanup(List<String> cleanup) {
		this.cleanup = cleanup;
	}
	public String getServerPath() {
		return serverPath;
	}
	public void setServerPath(String server_path) {
		this.serverPath = server_path;
	}
	public String getClientPath() {
		return clientPath;
	}
	public void setClientPath(String client_path) {
		this.clientPath = client_path;
	}
	public String getDatabasePath() {
		return databasePath;
	}
	public void setDatabasePath(String database_path) {
		this.databasePath = database_path;
	}
	public List<VerInfo> getSuit() {
		return suit;
	}
	public void setSuit(List<VerInfo> suit) {
		this.suit = suit;
	}
	
	public String checkVersion(){
		String errMsg = "";
		if(baseversion != null && !baseversion.isEmpty()){
    		String baseVer = this.getName()+baseversion;
    		if(this.getCurrentVerNo() == null || baseVer.compareTo(this.getCurrentVerNo())>0){
    			errMsg = "工程："+this.getName()+"版本依赖异常，当前版本【"+this.getCurrentVerNo()+"】小于基线版本【"+baseVer+"】，请先升级到此基线版本。";
    		}
    	}
    	return errMsg;
	}
	@Override
    public Project clone(){  
        Project sc = null;  
        try  
        {  
            sc = (Project) super.clone();  
        } catch (CloneNotSupportedException e){  
            e.printStackTrace();  
        }  
        return sc;  
    }  
	
}
