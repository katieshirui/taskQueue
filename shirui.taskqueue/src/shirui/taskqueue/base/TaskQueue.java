package shirui.taskqueue.base;

import java.util.Map;

public interface TaskQueue<E> {
	boolean add(E e);
	int len();
	E get();
	boolean done(E e);
	boolean shutdown();
	boolean is_closed();

}
