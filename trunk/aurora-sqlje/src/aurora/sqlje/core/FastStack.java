package aurora.sqlje.core;

import java.util.ArrayList;

/**
 * a simple <b>none-thread-safe</b> stack implement ,based on {@link ArrayList}<br>
 * 
 * 
 * @author jessen
 *
 * @param <E>
 *            element type
 */
public class FastStack<E> {
	private ArrayList<E> list = new ArrayList<E>();

	public E peek() {
		return list.get(list.size() - 1);
	}

	public E pop() {
		E e = list.get(list.size() - 1);
		list.remove(list.size() - 1);
		return e;
	}

	public E push(E e) {
		list.add(e);
		return e;
	}

	public boolean isEmpty() {
		return list.size() == 0;
	}

	public int size() {
		return list.size();
	}
}
