package aurora.sqlje.core.reflect;

import java.lang.reflect.Field;

import aurora.sqlje.core.database.StdField;

public class BeanInfo {
	private Class<?> beanClass;
	private Field pkField;
	private Field[] columnFields;
	private StdField[] stdFields;
	private String tableName;
	private String pkName;

	public Class<?> getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	public Field getPkField() {
		return pkField;
	}

	public void setPkField(Field pkField) {
		this.pkField = pkField;
	}

	/**
	 * no pkField and stdFields included.
	 * @return
	 */
	public Field[] getColumnFields() {
		return columnFields;
	}

	public void setColumnFields(Field[] columnFields) {
		this.columnFields = columnFields;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getPkName() {
		return pkName;
	}

	public void setPkName(String pkName) {
		this.pkName = pkName;
	}

	public boolean isStdWhoEnabled() {
		return isStdWhoEnabled;
	}

	public void setStdWhoEnabled(boolean isStdWhoEnabled) {
		this.isStdWhoEnabled = isStdWhoEnabled;
	}

	public StdField[] getStdFields() {
		return stdFields;
	}

	public void setStdFields(StdField[] stdFields) {
		this.stdFields = stdFields;
	}

	private boolean isStdWhoEnabled = false;

}
