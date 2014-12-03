package aurora.sqlje.core.database;

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aurora.database.service.SqlServiceContext;
import aurora.sqlje.core.DataTransfer;
import aurora.sqlje.core.ISqlCallStack;

public abstract class AbstractInsert {

	static String createdByField = "CREATED_BY";
	static String creationDateField = "CREATION_DATE";
	static String lastUpdatedByField = "LAST_UPDATED_BY";
	static String lastUpdatedDateField = "LAST_UPDATE_DATE";
	static String[] stdwho_fields = { createdByField, creationDateField,
			lastUpdatedByField, lastUpdatedDateField };
	static List<String> stdwho_fields_list = Arrays.asList(stdwho_fields);

	protected ISqlCallStack context;
	private boolean standardWhoEnabled;

	protected String[] columns;
	protected HashMap<String, InsertField> insertFieldOptions = new HashMap<String, InsertField>();
	private Object recordBean;
	private Map recordMap;

	private String tableName;
	private String pkName;

	public AbstractInsert(ISqlCallStack context, Object bean,String tableName,String pkName) {
		super();
		this.context = context;
		this.recordBean = bean;
		this.tableName=tableName;
		this.pkName = pkName;
	}

	public AbstractInsert(ISqlCallStack context, Map map, String tableName,
			String pkName) {
		super();
		this.context = context;
		this.recordMap = map;
		this.tableName = tableName;
		this.pkName = pkName;
	}

	/**
	 * do insert operation
	 * 
	 * @return primary key
	 */
	public Object insert() throws SQLException, Exception {
		Connection conn = context.getCurrentConnection();
		getInsertFieldOptions();
		String _sql_ = createInsertSql();
		String sql = getPrefix() + _sql_ + getSuffix();

		// System.out.println(sql);
		PreparedStatement ps = createStatement(conn, sql);
		performParameterBinding(ps);
		Object pk = execute(ps);
		// System.out.println("pk:" + pk);
		setPrimaryKeyBack(pk);
		return pk;
	}

	private void setPrimaryKeyBack(Object pk) {
		if (recordBean != null) {
			try {
				recordBean.getClass().getField(pkName).set(recordBean, pk);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (recordMap != null)
			recordMap.put(pkName, pk);
	}

	/**
	 * execute the statement, retrieve and return primary key
	 * 
	 * @param ps
	 * @return
	 * @throws SQLException
	 */
	protected abstract Object execute(PreparedStatement ps) throws SQLException;

	protected abstract PreparedStatement createStatement(Connection conn,
			String sql) throws SQLException;

	protected int performParameterBinding(PreparedStatement ps)
			throws SQLException {
		int i = 1;
		for (String c : columns) {
			InsertField insf = insertFieldOptions.get(c);
			if (insf.isParamBinding()) {
				ps.setObject(i, insf.getValue());
				// System.out.printf("%d%30s%s\n", i, c,
				// String.valueOf(insf.getValue()));
				i++;
			}
		}
		if (standardWhoEnabled) {
			InsertField insf = insertFieldOptions.get(createdByField);
			ps.setObject(i++, insf.getValue());
			ps.setObject(i++, insf.getValue());
		}
		return i;
	}

	protected abstract String getDateExpression();

	protected abstract String getTimeExpression();

	protected String getPrefix() {
		return "";
	}

	protected String createInsertSql() {
		StringBuilder strb = new StringBuilder();
		strb.append("insert into ").append(getTableName()).append("\n(");
		for (int i = 0; i < columns.length; i++) {
			if (i != 0)
				strb.append(",");
			strb.append(columns[i]);

		}
		if (standardWhoEnabled) {
			for (String s : stdwho_fields)
				strb.append(",").append(s);
		}
		strb.append(")\nvalues\n(");
		for (int i = 0; i < columns.length; i++) {
			if (i != 0)
				strb.append(",");
			InsertField insf = insertFieldOptions.get(columns[i]);
			if (insf == null) {
				// System.err.println("InsertField for " + columns[i]
				// + " is null.");
				continue;
			}
			if (insf.isParamBinding()) {
				strb.append("?");
			} else {
				strb.append(insf.getExpression());
			}

		}
		if (standardWhoEnabled) {
			for (String s : stdwho_fields) {
				InsertField insf = insertFieldOptions.get(s);
				if (insf.isParamBinding())
					strb.append(",").append("?");
				else
					strb.append(",").append(insf.getExpression());
			}
		}
		strb.append(")");
		return strb.toString();
	}

	protected Map<String, InsertField> getInsertFieldOptions() throws Exception {
		insertFieldOptions.clear();
		if (recordBean != null)
			getInsertFieldOptionsOfBean(recordBean);
		else if (recordMap != null)
			getInsertFieldOptionsOfMap(recordMap);
		if (standardWhoEnabled) {
			SqlServiceContext sqlsvc = SqlServiceContext
					.createSqlServiceContext(context.getContextData());
			Long user_id = sqlsvc.getSession().getLong("user_id");
			InsertField insf = new InsertField();
			insf.setType(Long.class);
			insf.setValue(user_id);
			insertFieldOptions.put(createdByField, insf);
			insertFieldOptions.put(lastUpdatedByField, insf);

			insf = new InsertField();
			insf.setParaBinding(false);
			insf.setExpression(getTimeExpression());
			insertFieldOptions.put(creationDateField, insf);
			insertFieldOptions.put(lastUpdatedDateField, insf);
		}
		return insertFieldOptions;
	}

	private void getInsertFieldOptionsOfBean(Object bean) throws Exception {
		getTableName0(bean);
		ArrayList<String> columnList = new ArrayList<String>();
		java.lang.reflect.Field[] fields = bean.getClass().getFields();
		for (java.lang.reflect.Field f : fields) {
			if (f.getModifiers() != Modifier.PUBLIC)
				continue;
			Class<?> type = f.getType();
			if (!DataTransfer.supported_type_list.contains(type))
				continue;
			String name = f.getName();
			if (stdwho_fields_list.contains(name.toUpperCase()))
				continue;
			columnList.add(name);
			DBField af = f.getAnnotation(DBField.class);
			PK pkaf = f.getAnnotation(PK.class);
			InsertField insf = new InsertField();
			if (af != null && af.name().length() > 0)
				insf.setName(af.name());
			if (name.equals(pkName)) {
				//pkName = name;
				// System.out.println("pk:" + pkName);
				insf.setParaBinding(false);
				insf.setExpression(getInsertExpressionForPk());
			} else {
				insf.setType(type);
				insf.setValue(f.get(bean));
			}
			insertFieldOptions.put(name, insf);
		}
		columns = columnList.toArray(new String[columnList.size()]);

	}

	private void getInsertFieldOptionsOfMap(Map<String, Object> map) {
		if(!map.keySet().contains(pkName))
			map.put(pkName,null);
		ArrayList<String> columnList = new ArrayList<String>();
		for (String s : map.keySet()) {
			if (s != null && s.length() > 0 && s.charAt(0) != '$') {
				Object v = map.get(s);
				if (v != null
						&& !DataTransfer.supported_type_list.contains(v
								.getClass()))
					continue;
				if (stdwho_fields_list.contains(s.toUpperCase()))
					continue;
				columnList.add(s);
			}
		}
		columns = columnList.toArray(new String[columnList.size()]);
		for (String c : columns) {
			InsertField insf = new InsertField();
			if (c.equals(pkName)) {
				insf.setName(c);
				insf.setParaBinding(false);
				insf.setExpression(getInsertExpressionForPk());
			} else {
				Object v = map.get(c);
				insf.setType(v == null ? Object.class : v.getClass());
				insf.setValue(v);
			}

			insertFieldOptions.put(c, insf);
		}
	}

	private void getTableName0(Object bean) {
		DBTable dbt = bean.getClass().getAnnotation(DBTable.class);
		if (dbt != null) {
			tableName = dbt.name();
			standardWhoEnabled = dbt.stdwho();
			System.out.println("tableName:" + tableName);
		} 
		/*
		 * else { tableName = bean.getClass().getSimpleName().toUpperCase();
		 * System.out.println("className:" + tableName); }
		 */
	}

	protected abstract String getInsertExpressionForPk();

	public boolean isStandardWhoEnabled() {
		return standardWhoEnabled;
	}

	public void setStandardWhoEnabled(boolean enabled) {
		standardWhoEnabled = enabled;
	}

	protected String getTableName() {
		return tableName;
	}

	protected String getPkField() {
		return pkName;
	}

	/**
	 * not include standard-who fields
	 * 
	 * @return
	 */
	protected String[] getColumns() {
		return new String[0];
	}

	protected String getSuffix() {
		return "";
	}

}
