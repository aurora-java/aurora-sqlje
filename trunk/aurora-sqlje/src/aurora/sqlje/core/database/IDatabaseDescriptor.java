package aurora.sqlje.core.database;

public interface IDatabaseDescriptor {
	String name();
	boolean isOracle();
	boolean isMysql();
	boolean isSqlServer();
}
