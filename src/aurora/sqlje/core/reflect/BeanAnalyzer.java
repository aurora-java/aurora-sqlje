package aurora.sqlje.core.reflect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import aurora.sqlje.core.DataTransfer;
import aurora.sqlje.core.annotation.PK;
import aurora.sqlje.core.annotation.Table;
import aurora.sqlje.core.database.AbstractInsertOperation;
import aurora.sqlje.core.database.StdField;

public class BeanAnalyzer {

	private HashMap<Class<?>, BeanInfo> infoCache = new HashMap<Class<?>, BeanInfo>();
	private static BeanAnalyzer instance = new BeanAnalyzer();
	private StdField[] stdfields;

	private BeanAnalyzer() {

	}

	public static BeanAnalyzer instance() {
		return instance;
	}

	public void setStdfields(StdField[] sf) {
		this.stdfields = sf;
	}

	public StdField[] getStdfields() {
		return stdfields;
	}

	public BeanInfo getBeanInfo(Class<?> clazz) {
		BeanInfo bi = infoCache.get(clazz);
		if (bi == null) {
			bi = analyze(clazz);
			infoCache.put(clazz, bi);
		}
		return bi;
	}

	public void removeCache(Class<?> clazz) {
		infoCache.remove(clazz);
	}

	public void clearCache() {
		infoCache.clear();
	}

	public BeanInfo analyze(Class<?> clazz) {
		BeanInfo info = new BeanInfo();
		info.setBeanClass(clazz);
		Table t = clazz.getAnnotation(Table.class);
		if (t != null) {
			info.setTableName(t.name());
			info.setStdWhoEnabled(t.stdwho());
		} else {
			info.setTableName(clazz.getSimpleName().toUpperCase());
		}
		Field[] flds = clazz.getFields();
		List<Field> fieldList = new ArrayList<Field>();
		List<StdField> stdfieldList = new ArrayList<StdField>();
		StdField stdField = null;
		for (Field f : flds) {
			Class<?> type = f.getType();
			if (!DataTransfer.supported_type_list.contains(type))
				continue;
			PK p = f.getAnnotation(PK.class);
			if (p != null) {
				if (info.getPkField() != null)
					throw new IllegalStateException("duplicate PK Field in "
							+ clazz.getName());
				info.setPkField(f);
				info.setPkName(f.getName());
			} else if ((stdField = isStdWhoField(f.getName())) != null) {
				stdfieldList.add(stdField);
			} else
				fieldList.add(f);
		}
		if (info.getPkField() == null)
			System.out.println("no PK field declared in " + clazz.getName());
		info.setColumnFields(fieldList.toArray(new Field[fieldList.size()]));
		info.setStdFields(stdfieldList.toArray(new StdField[stdfieldList.size()]));
		return info;
	}

	private StdField isStdWhoField(String name) {
		for (StdField sf : stdfields) {
			if (sf.getName().equalsIgnoreCase(name))
				return sf;
		}
		return null;
	}
}
