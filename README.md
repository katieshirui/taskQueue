# task-queue
this project consists one interface and one class and one test case;</br>
##
interface TaskQueue </br>
including add(E e); len(); get(); done(E e); shutdown(); is_closed();</br>
##
class LinkedTaskQueue </br>
implements TaskQueue  </br>
##
test case TaskQueueTest </br>
there are three threads in this test,including one producer and two Consumer; </br>
##
run TaskQueueTest to see the result;</br>
##
result of one test is as follows:</br>
```
pool-1-thread-3 got task 3
pool-1-thread-3 finished processing task: 3
pool-1-thread-3 getting queue status
pool-1-thread-2 getting queue status
pool-1-thread-3 got queue status: true
pool-1-thread-2 got queue status: true
when  pool-1-thread-2 getting task,queue is empty or the task is being processed :true
pool-1-thread-3 deleted task : 3
queue is tring to shutdown
pool-1-thread-2 getting queue status
pool-1-thread-2 got queue status: false
pool-1-thread-2 finished processing task: null
pool-1-thread-2 getting queue status
pool-1-thread-2 got queue status: false
when pool-1-thread-2 deleting task,queue is shutdown
pool-1-thread-1 is trying to add task: 4
pool-1-thread-1 getting queue status
pool-1-thread-1 got queue status: false
when pool-1-thread-1 adding task,queue is shutdown
pool-1-thread-3 getting queue status
pool-1-thread-3 got queue status: false
consumer: pool-1-thread-3 thread run finished
pool-1-thread-1 getting queue status
pool-1-thread-1 got queue status: false
producer thread run finished

```


