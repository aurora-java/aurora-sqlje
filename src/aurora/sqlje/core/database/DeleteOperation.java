package aurora.sqlje.core.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;

import aurora.sqlje.core.ISqlCallStack;
import aurora.sqlje.core.annotation.PK;
import aurora.sqlje.core.annotation.Table;

/**
 * Standard delete operation<br>
 * 
 * @author jessen
 *
 */
public class DeleteOperation {
	private ISqlCallStack callStack;
	private String tableName;
	private String pkName;
	private Object pkValue;

	public DeleteOperation(ISqlCallStack callStack, String tableName,
			String pkName, Object value) {
		super();
		this.callStack = callStack;
		this.tableName = tableName;
		this.pkName = pkName;
		this.pkValue = value;
	}

	public DeleteOperation(ISqlCallStack callStack, Object bean) {
		super();
		this.callStack = callStack;

		Class<?> clazz = bean.getClass();
		Table table = clazz.getAnnotation(Table.class);
		if (table != null) {
			tableName = table.name();
		} else
			tableName = clazz.getSimpleName();
		for (Field f : clazz.getFields()) {
			PK pka = f.getAnnotation(PK.class);
			if (pka != null) {
				pkName = f.getName();
				try {
					pkValue = f.get(bean);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				break;
			}
		}
	}

	/**
	 * delete by pk
	 * 
	 * @throws Exception
	 */
	public void doDelete() throws Exception {
		if (tableName == null || pkName == null) {
			throw new IllegalArgumentException(
					"can not do delete without tableName or pkName");
		}
		StringBuilder sb = new StringBuilder(100);
		sb.append("DELETE FROM ").append(tableName).append(" WHERE ")
				.append(pkName).append("=?");
		//System.out.println("[DELETE]" + sb.toString());
		//System.out.println("[DELETE]" + pkName + "=" + pkValue);
		Connection conn = callStack.getCurrentConnection();
		PreparedStatement ps = conn.prepareStatement(sb.toString());
		try {
			ps.setObject(1, pkValue);
			ps.execute();
		} finally {
			ps.close();
		}
	}
}
