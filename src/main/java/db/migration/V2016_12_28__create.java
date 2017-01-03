package db.migration;

import java.sql.Connection;
import java.sql.Statement;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

public class V2016_12_28__create implements JdbcMigration{

	public void migrate(Connection connection) throws Exception {
		// TODO Auto-generated method stub
		
		Statement st = connection.createStatement();
		try{
			st.execute("insert into ydz_flyway_test(name,code)values('ydz','java')");
		}catch(Exception ex){
			ex.printStackTrace();
			connection.rollback();
		}finally{
			if(st != null){
				st.close();
			}
		}
	}
	
	

}
