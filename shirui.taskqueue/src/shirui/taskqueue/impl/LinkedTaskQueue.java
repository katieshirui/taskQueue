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
 
        Node<E> next; 

        Node(E x) { item = x; }
    }
	/** the capacity of the queue */
	private final int capacity;
	
    /** the number of nodes */
	private final AtomicInteger len = new AtomicInteger(0);
	
	/** if the queue is open */
	private boolean isOpen=true;
	
	/** if the first task is in processing */
	boolean isProcessed=false;
	
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
    			System.out.println("when "+Thread.currentThread().getName()+" adding task,queue is shutdown");
    			return false;
    		}
        	while (len.get() == capacity) {			//when the queue is full block adding thread
                notFull.await();
            }
            if (len.get() < capacity) {				// when the queue is not full
            	last = last.next = new Node<E>(e);	//add a node to the end of the queue
            	System.out.println(Thread.currentThread().getName()+" has added task "+ e); 
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
            	System.out.println("when "+Thread.currentThread().getName()+"  getting task,queue is shutdown");
    			
            	return null;
            }
        	while (len.get() == 0 || isProcessed==true) {							//when the queue is empty block taking thread
        		System.out.println("when  "+Thread.currentThread().getName()+" getting task,queue is empty or the task is being processed :"+isProcessed);
                notEmpty.await();
                if (is_closed()) {
                	notEmpty.signal();
                	return null;
                }
            }
        	Node<E> x = head.next;								//get the fist task without deleting it
            if (x == null) {									//if the first task got is null
            	System.out.println("when "+Thread.currentThread().getName()+" getting task,the task taken from queue is null");
            	return null;
            }else { 											//the first task is not null
            	isProcessed=true;
            	System.out.println(Thread.currentThread().getName()+" got task "+ x.item); 
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
	 * @throws InterruptedException 
     */
	@Override
	public boolean done(E e) throws InterruptedException {
		if (is_closed()) {
			System.out.println("when "+Thread.currentThread().getName()+" deleting task,queue is shutdown");
			notEmpty.signal();
			return false;
		}
		if (e==null || len.get() == 0) {
			System.out.println("when "+Thread.currentThread().getName()+" deleting task,task is null or queue is empty");
            return false;
		}
		if(e.equals(head.next.item)) {				//if the task that just got processed is the same as the first task in the queue 
			takeLock.lock();							//get taking lock		
			int c = -1;
			try {
				Node<E> h = head;
		        Node<E> first = h.next;
		        h.next = h; // help GC
		        head = first;
		        first.item = null;
				isProcessed = false;
				c = len.getAndDecrement();
				System.out.println(Thread.currentThread().getName()+" deleted task : "+ e);         
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
	        	isOpen=false;
	        }
	        System.out.println("queue is tring to shutdown");
	        notEmpty.signalAll();
	        notFull.signalAll();
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
		System.out.println(Thread.currentThread().getName()+" getting queue status");
		final ReentrantLock shutdownLock = this.shutdownLock;
		shutdownLock.lock();
		try {
			return !isOpen;
		}finally {
			System.out.println(Thread.currentThread().getName()+" got queue status: "+ isOpen);
			
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
