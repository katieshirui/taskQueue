package shirui.taskqueue.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import shirui.taskqueue.impl.LinkedTaskQueue;

public class TaskQueueTest {

	public class Queue {

		LinkedTaskQueue<String> queue = new LinkedTaskQueue<String>(5);			// add task to the queue
        public void put(String s) throws InterruptedException {
        	System.out.println(Thread.currentThread().getName()+" is trying to add task: "+s);
        	queue.add(s);
        	System.out.println("task "+s+" is added"); 
        }
        
        																		// take task from the queue and process the task, delete the task after process; 
        public void take() throws InterruptedException {
            String task = queue.get();
            System.out.println(Thread.currentThread().getName()+" is processing task: "+task);
            queue.done(task);
            System.out.println(Thread.currentThread().getName()+" finished processing task: "+task);
            if(task=="3") queue.shutdown(); 				 					//test the shutdown function,after finishing task3,the queue should be shutdown;		
        }
	}
	
	class Taker implements Runnable {
        private Queue queue;

        public Taker(String instance, Queue queue) {
            this.queue = queue;
        }

        public void run() {
            try {
                while (!queue.queue.is_closed()) {
                    queue.take();
                    Thread.sleep(300);
                }
            } catch (InterruptedException ex) {
                System.out.println("Producer Interrupted");
            }
        }
    }

    
    class Adder implements Runnable {
        private Queue queue;

        public Adder(String instance,Queue queue) {
            this.queue = queue;
        }

        public void run() {
            try {
                while (!queue.queue.is_closed()) {
                    queue.put("1");
                    queue.put("2");
                    queue.put("3");
                    queue.put("4");
                    Thread.sleep(5000);
                }
            } catch (InterruptedException ex) {
                System.out.println("Consumer Interrupted");
            }
        }
    }

    public static void main(String[] args) {
    	TaskQueueTest test = new TaskQueueTest();

    	Queue queue = test.new Queue();

        ExecutorService service = Executors.newCachedThreadPool();
        Adder adder1 = test.new Adder("adder1", queue);
        Taker taker1 = test.new Taker("taker1", queue);
        Taker taker2 = test.new Taker("taker2", queue);
        service.submit(adder1);
        service.submit(taker1);
        service.submit(taker2);
        service.shutdown();
    }

}
