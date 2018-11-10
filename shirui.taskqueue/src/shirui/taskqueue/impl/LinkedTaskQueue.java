package shirui.taskqueue.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import shirui.taskqueue.base.TaskQueue;

/**
 * @author shirui0825
 *
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
    
	@Override
	public boolean add(E e) {
		if (e == null)
            throw new NullPointerException();
		if (is_closed()) {
			System.out.println("queue is shutdown");
			return false;
		}/*
        if (len.get() == capacity) {
        	System.out.println("queue is full");
            return false;
        }*/
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        putLock.lock();// get the lock for adding task
        try {
        	while (len.get() == capacity) {//when the queue is full block adding thread
                notFull.await();
            }
            if (len.get() < capacity) {// when the queue is not full
            	last = last.next = new Node<E>(e);//add a node to the end of th queue
                c = len.getAndIncrement();// the size goes down by one
                if (c + 1 < capacity)// if the size of the queue after adding a task is still smaller than the capacity
                    notFull.signal();//wake up one thread in adding thread
            }
        }catch(InterruptedException e1){
        	
        } finally {
            putLock.unlock();// release the lock for adding 
        }
        if (c == 0)// if the queue was empty before adding this task
            signalNotEmpty();
        return c >= 0;
	}

	@Override
	public int len() {
		return len.get();
	}

	@Override
	public E get() {
		/*
        if (len.get() == 0) {
        	System.out.println("queue is empty");
            return null;
        }*/
        if (is_closed()) {
        	System.out.println("queue is shutdown");
			return null;
        }
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();// get the lock for taking task
        try {
        	while (len.get() == 0) {//when the queue is empty block taking thread
        		System.out.println("queue is empty");
                notEmpty.await();
            }
        	Node<E> x = head.next;//get the fist task without deleting it
            if (x == null) {//if the first task got is null
            	System.out.println("the task taken from queue is null");
            	return null;
            }else {//the first task is not null
            	if(x.isProcessed) {//if it's get by another thread already,then get the null node
            		System.out.println("the task taken from queue is in processing");
            		return null;
            	}
            	x.isProcessed=true;
                return x.item;
            }
        }catch(InterruptedException e1) {
        	
        } finally {
            takeLock.unlock();
        }
		return null;
	}

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
		if(e.equals(head.next.item)) {//if the task that just got processed is the same as the first task in the queue 
	    takeLock.lock();//get taking lock
		int c = -1;
		try {
		Node<E> h = head;//get the head node 
        Node<E> first = h.next;//get the first task
        h.next = h; 
        head = first;//set the head node as null
        first.item = null;//set the task node as null
        c = len.get();
        if (c > 1)// if there is node left in the queue
            notEmpty.signal();// wake taking thread
			} finally {
	            takeLock.unlock();//release taking lock
	        }
	        if (c == capacity)//if the queue before taking this node out was full
	            signalNotFull();// wake adding thread
		}
		return false;
	}

	@Override
	public boolean shutdown() {
		putLock.lock();
        takeLock.lock();
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
    	}
	}

	@Override
	public boolean is_closed() {
		return !isOpen;
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
