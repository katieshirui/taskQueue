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
        boolean isProcessed=false;
        
        Node<E> next; 

        Node(E x) { item = x; }
    }
	
	private final int capacity;

    /** the number of nodes */
	private final AtomicInteger len = new AtomicInteger(0);
	
	private boolean isOpen=true;

    /** 链表头节点 */
    private Node<E> head;

    /** 链表尾节点 */
    private Node<E> last;

    /** 出队锁 */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** 出队等待条件 */
    private final Condition notEmpty = takeLock.newCondition();

    /** 入队锁 */
    private final ReentrantLock putLock = new ReentrantLock();

    /** 入队等待条件 */
    private final Condition notFull = putLock.newCondition();
    
    public LinkedTaskQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<E>(null);//初始化头节点和尾节点，均为封装了null数据的节点
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
		}
        if (len.get() == capacity) {
        	System.out.println("queue is full");
            return false;
        }
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        putLock.lock();// 获取入队锁
        try {
        	while (len.get() == capacity) {//当队列已满，添加线程阻塞
                notFull.await();
            }
            if (len.get() < capacity) {// 容量没满
            	last = last.next = new Node<E>(e);
                c = len.getAndIncrement();// 容量+1，返回旧值（注意）
                if (c + 1 < capacity)// 如果添加元素后的容量，还小于指定容量（说明在插入当前元素后，至少还可以再插一个元素）
                    notFull.signal();// 唤醒等待notFull条件的其中一个线程
            }
        }catch(InterruptedException e1){
        	
        } finally {
            putLock.unlock();// 释放入队锁
        }
        if (c == 0)// 如果c==0，这是什么情况？一开始如果是个空队列，就会是这样的值，要注意的是，上边的c返回的是旧值
            signalNotEmpty();
        return c >= 0;
	}

	@Override
	public int len() {
		return len.get();
	}

	@Override
	public E get() {
        if (len.get() == 0) {
        	System.out.println("queue is empty");
            return null;
        }
        if (is_closed()) {
        	System.out.println("queue is shutdown");
			return null;
        }
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();// 获取出队锁
        try {
        	while (len.get() == 0) {//当队列为空，取出线程阻塞
                notEmpty.await();
            }
        	Node<E> x = head.next;
            if (x == null) {//如果队列为空，则直接返回null
            	System.out.println("the task taken from queue is null");
            	return null;
            }else {
            	if(x.isProcessed) {
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
		if(e.equals(head.next.item)) {
	    takeLock.lock();
		int c = -1;
		try {
		Node<E> h = head;//获取头节点：x==null
        Node<E> first = h.next;//将头节点的下一个节点赋值给first
        h.next = h; // 将当前将要出队的节点置null（为了使其做head节点做准备）
        head = first;//将当前将要出队的节点作为了头节点
        first.item = null;//将出队节点的值置空
        c = len.get();
        if (c > 1)// 还有元素（如果旧值c==1的话，那么通过上边的操作之后，队列就空了）
            notEmpty.signal();// 唤醒等待在notEmpty队列中的其中一条线程
			} finally {
	            takeLock.unlock();
	        }
	        if (c == capacity)// c == capacity是怎么发生的？如果队列是一个满队列，注意：上边的c返回的是旧值
	            signalNotFull();
		}
		return false;
	}

	@Override
	public boolean shutdown() {
		putLock.lock();
        takeLock.lock();
        try {
        	System.out.println("trying to shutdown");
        if(isOpen != false) {
        	isOpen=false;
        }
        return isOpen;
	}finally {
		putLock.unlock();// 释放出队锁
        takeLock.unlock();
    	}
	}

	@Override
	public boolean is_closed() {
		return !isOpen;
	}
		
	private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();//获取出队锁
        try {
            notEmpty.signal();//唤醒等待notEmpty条件的线程中的一个
        } finally {
            takeLock.unlock();//释放出队锁
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
