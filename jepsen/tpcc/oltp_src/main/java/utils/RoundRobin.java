package utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RoundRobin<T> implements Iterable<T> {
	private List<T> coll;

	public RoundRobin() {
		this.coll = new ArrayList<T>();
	}

	public void add(T newElement) {
		coll.add(newElement);
	}
	
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int index = 0;
			
			public boolean hasNext() {
				return true;
			}

			public T next() {
				System.out.println("~~>"+index);
				T res = coll.get(index);
				index = (index + 1) % coll.size();
				return res;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

}