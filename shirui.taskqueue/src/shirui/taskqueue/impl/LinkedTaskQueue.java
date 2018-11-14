package shirui.taskqueue.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import shirui.taskqueue.base.TaskQueue;


/**
 * @author shirui0825
 * Blocked task queue
 * @param <E>
 */
public class LinkedTaskQueue<E> implements TaskQueue<E>{

	static class Node<E> {
        E item;
        /** if the task is already taken by another thread */
        boolean isProcessed=false;
        
        Node<E> next; 

        Node(E x) { item = x; }
    }
	/** the capacity of the queue */
	private final int capacity;

    /** the number of nodes */
	private final AtomicInteger len = new AtomicInteger(0);
	
	/** if the queue is open */
	private boolean isOpen=true;

    /** the head node of the queue */
    private Node<E> head;

    /** node of the queue */
    private Node<E> last;
    
    /** lock for shutdown task */
    private final ReentrantLock shutdownLock = new ReentrantLock();

    /** lock for taking task */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** the condition for taking task */
    private final Condition notEmpty = takeLock.newCondition();

    /** lock for adding task */
    private final ReentrantLock putLock = new ReentrantLock();

    /** the condition for adding task  */
    private final Condition notFull = putLock.newCondition();
    
    public LinkedTaskQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }
    
    public LinkedTaskQueue() {
        this(Integer.MAX_VALUE);
    }
    
	/**
	 * add an object to the queue,if the queue is shutdown,return false;
	 * if the queue is full,set the status of the producer threads as wait;
	 * else add the object to the end of the queue;
	 * if the queue is not full,wake up the producer threads;
	 * @param E:Object
     * @throws InterruptedException
     * @return boolean
	 */
	@Override
	public boolean add(E e) throws InterruptedException {
		if (e == null) throw new NullPointerException();
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        putLock.lockInterruptibly();				// get the lock for adding task
        try {
        	if (is_closed()) {
    			System.out.println("queue is shutdown");
    			return false;
    		}
        	while (len.get() == capacity) {			//when the queue is full block adding thread
                notFull.await();
            }
            if (len.get() < capacity) {				// when the queue is not full
            	last = last.next = new Node<E>(e);	//add a node to the end of the queue
                c = len.getAndIncrement();			// the size goes up by one
                if (c + 1 < capacity)				// if the size of the queue after adding a task is still smaller than the capacity
                    notFull.signal();				//wake up one thread in adding thread
            }
        }finally {
            putLock.unlock();						// release the lock for adding 
        }
        if (c == 0)									// if the queue was empty before adding this task
            signalNotEmpty();
        return c >= 0;
	}

	/**
	 * get the length of the queue;
     * @return Int
	 */
	@Override
	public int len() {
		return len.get();
	}
	
	/**
     * get the first object in the queue;
     * if the queue is shutdown,return null;
     * if the queue is empty,set the status of consumer thread as wait;
     * when the first object of the queue is not null, if the object is already in processing,return null;
     * else set the isProcessed status of the object as true,and return the object; 
     * @throws InterruptedException
     * @return E:Object that stored in the queue
     */
	@Override
	public E get() throws InterruptedException {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();							// get the lock for taking task
        try {
        	if (is_closed()) {
            	System.out.println("queue is shutdown");
    			return null;
            }
        	while (len.get() == 0) {							//when the queue is empty block taking thread
        		System.out.println("queue is empty");
                notEmpty.await();
            }
        	Node<E> x = head.next;								//get the fist task without deleting it
            if (x == null) {									//if the first task got is null
            	System.out.println("the task taken from queue is null");
            	return null;
            }else { 											//the first task is not null
            	if(x.isProcessed) {             				//if it's get by another thread already,then get the null node
            		System.out.println("the task taken from queue is in processing");
            		return null;
            	}
            	x.isProcessed=true;
                return x.item;
            }
        }finally {
            takeLock.unlock();
        }
	}

	/**delete the object that has been successfully processed;
     * if the queue is empty,return false;
     * if the queue is shutdown,return false;
     * else if the first object in the queue is processed, then delete it;
     * if the queue is not empty, wake up the consumer thread;
     * @return boolean;
     */
	@Override
	public boolean done(E e) {
		if (len.get() == 0) {
			System.out.println("queue is empty");
            return false;
		}
		if (is_closed()) {
			System.out.println("queue is shutdown");
			return false;
		}
		if(e.equals(head.next.item)) {				//if the task that just got processed is the same as the first task in the queue 
			takeLock.lock();							//get taking lock		
			int c = -1;
			try {
				Node<E> h = head.next.next;	
				head.next.item=null;
				head.next = h; 						
				c = len.get();
				if (c > 1) notEmpty.signal();					// wake taking thread
			} finally {
				takeLock.unlock();					//release taking lock
			}
	        if (c == capacity) signalNotFull();					// wake adding thread
		}
		return false;
	}

	/**shutdown the queue;
     * @return boolean;
     */
	@Override
	public boolean shutdown() {
		putLock.lock();
        takeLock.lock();
        shutdownLock.lock();
        try {
	        if(isOpen != false) {
	        	System.out.println("queue shutdown");
	        	isOpen=false;
	        }
	        System.out.println("queue shutdown");
	        return isOpen;
        }finally {
        	putLock.unlock();
        	takeLock.unlock();
        	shutdownLock.unlock();
    	}
	}

	/**return the isOpen status of the queue;
     * @return boolean;
     */
	@Override
	public boolean is_closed() {
		final ReentrantLock shutdownLock = this.shutdownLock;
		shutdownLock.lock();
		try {
			return !isOpen;
		}finally {
			shutdownLock.unlock();
		}
	}
		
	private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }
	
	private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }
}
