package shirui.taskqueue.base;

import java.util.Map;

public interface TaskQueue<E> {
	boolean add(E e) throws InterruptedException;
	int len();
	E get() throws InterruptedException;
	boolean done(E e) throws InterruptedException;
	boolean shutdown();
	boolean is_closed();

}
