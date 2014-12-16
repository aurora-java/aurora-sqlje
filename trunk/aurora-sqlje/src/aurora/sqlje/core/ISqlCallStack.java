/*
 * Created on 2014-7-30 下午3:51:25
 * $Id$
 */

package aurora.sqlje.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import aurora.sqlje.core.database.IDatabaseDescriptor;
import uncertain.composite.CompositeMap;

public interface ISqlCallStack {

	public Connection getCurrentConnection() throws SQLException;

	public Connection createConnection() throws SQLException;

	public void push(ResultSet rs);

	public void push(Statement stmt);

	public void free(Connection conn) throws SQLException;

	/*
	 * public DataSource getDataSource();
	 * 
	 * public void setDataSource(DataSource ds);
	 */
	public CompositeMap getContextData();

	public void setContextData(CompositeMap data);

	public void cleanUp() throws SQLException;

	public void commit() throws SQLException;

	public void rollback() throws SQLException;
	
	public IDatabaseDescriptor getDatabaseDescriptor() ;

}
