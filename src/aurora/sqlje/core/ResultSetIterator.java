package aurora.sqlje.core;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ResultSetIterator<T> implements Iterable<T>, Iterator<T> {
	final ResultSet rs;
	Class<T> typeClass;
	T current;
	List<String> column_names;
	boolean isBasicType = false;

	private ResultSetIterator(ResultSet rs) {
		super();
		this.rs = rs;
		if (rs == null)
			throw new NullPointerException("ResultSet can not be null.");
		getColumnsInfo();
	}

	public ResultSetIterator(ResultSet rs, Class<T> clazz) {
		this(rs);
		this.typeClass = clazz;
		isBasicType = DataTransfer.supported_type_list.contains(clazz);
	}

	private void getColumnsInfo() {
		try {
			column_names = DataTransfer.getColumnNames(rs);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	void createCurrent() {
		try {
			if (isBasicType) {
				current = (T) DataTransfer.verboseGet(rs, column_names.get(0),
						typeClass);
			} else if (Map.class.isAssignableFrom(typeClass)) {
				if (!typeClass.isInterface()) {
					current = typeClass.newInstance();
				} else
					current = (T) new HashMap();
				DataTransfer.fillMap((Map) current, rs, column_names);
			} else {
				current = typeClass.newInstance();
				DataTransfer.fillBean(current, rs, column_names);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() {
		try {
			boolean next = rs.next();
			if (!next) {
				rs.close();
			}
			return next;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public T next() {
		createCurrent();
		return current;
	}

	@Override
	public void remove() {
		throw new RuntimeException("remove is not supported.");
	}

	@Override
	public Iterator<T> iterator() {
		return this;
	}
}
