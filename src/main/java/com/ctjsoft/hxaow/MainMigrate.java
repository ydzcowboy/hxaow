package com.ctjsoft.hxaow;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

public class MainMigrate {
	public static void main(String[] args){
		
        // 创建Flyway实例
        Flyway flyway = new Flyway();
        
//        flyway.setLocations("flyway");//设置flyway扫描sql升级脚本、java升级脚本的目录路径或包路径（表示是src/main/resources/flyway下面，前缀默认为src/main/resources，因为这个路径默认在classpath下面）
        flyway.setEncoding("UTF-8");//设置sql脚本文件的编码
//        flyway.setOutOfOrder(true);
//        flyway.setTable("schema_version");//设置存放flywaymetadata数据的表名
        
//        flyway.setBaselineVersionAsString("2016.12.24");
//        flyway.info();
        
        //flyway.setSchemas("flywaydemo");//设置接受flyway进行版本管理的多个数据库

        //flyway.setValidationMode(ValidationMode.ALL);//设置执行migrate操作之前的validation行为
        //flyway.setValidationErrorMode(ValidationErrorMode.FAIL);//设置当validation失败时的系统行为
       

        // 设置数据库 
        flyway.setDataSource("jdbc:oracle:thin:@localhost:1521:orcl", "tx0315", "a",null);
        //设置当validation失败时的系统行为
        try{
            flyway.setBaselineOnMigrate(true);
            // 开始迁移
            flyway.setBaselineVersionAsString("2016.12.32");
            flyway.migrate();
//           
//            flyway.baseline();
        }catch(FlywayException e){
            flyway.repair();
            e.printStackTrace();
        }
        System.out.println(flyway.getBaselineVersion());
        System.out.println(flyway.getTable());
        
	}

}
