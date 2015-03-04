package aurora.sqlje.core.database;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DatabaseDescriptor implements IDatabaseDescriptor {

	private String name = "";
	private boolean isOracle, isMysql, isSqlServer,isDB2;

	public void init(DatabaseMetaData dmd) throws SQLException {
		name = dmd.getDatabaseProductName();
		isOracle = name.equalsIgnoreCase("oracle");
		isMysql = name.equalsIgnoreCase("mysql");
		isSqlServer = name.equalsIgnoreCase("Microsoft SQL Server");
		isDB2 = name.startsWith("DB2");
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public boolean isOracle() {
		return isOracle;
	}

	@Override
	public boolean isMysql() {
		return isMysql;
	}

	public boolean isSqlServer() {
		return isSqlServer;
	}

	@Override
	public boolean isDB2() {
		return isDB2;
	}

}
