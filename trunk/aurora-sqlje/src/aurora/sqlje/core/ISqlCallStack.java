/*
 * Created on 2014-7-30 下午3:51:25
 * $Id$
 */

package aurora.sqlje.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import uncertain.composite.CompositeMap;
import aurora.sqlje.core.database.IDatabaseDescriptor;

public interface ISqlCallStack {

	public Connection getCurrentConnection() throws SQLException;

	/**
	 * {@link #createConnection(boolean)} default true
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Connection createConnection() throws SQLException;

	/**
	 * create a connection
	 * 
	 * @param man
	 *            <ul>
	 *            <li>true:this connection will push into stack,all resource
	 *            created by this connection will close automatically</li>
	 *            <li>false:just create a connection,nothing more will be
	 *            done,all resource created by this connection MUST close
	 *            manually</li>
	 *            </ul>
	 * @return
	 * @throws SQLException
	 */
	public Connection createConnection(boolean man) throws SQLException;

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

	public IDatabaseDescriptor getDatabaseDescriptor();

}
