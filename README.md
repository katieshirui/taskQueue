# task-queue
## 
this project consists one interface and one class and one test case;</br>
interface TaskQueue </br>
including add(E e); len(); get(); done(E e); shutdown(); is_closed();</br>
class LinkedTaskQueue </br>
implements TaskQueue  </br>
test case TaskQueueTest </br>
there are three threads in this test,including one producer and two Consumer; </br>
##
run TaskQueueTest to see the result;</br>
##
result of one test is as follows:</br>
```
queue is empty
pool-1-thread-1 is trying to add task: 1
pool-1-thread-3 is processing task: 1
pool-1-thread-3 finished processing task: 1
task 1 is added
the task taken from queue is null
pool-1-thread-1 is trying to add task: 2
pool-1-thread-2 is processing task: null
task 2 is added
pool-1-thread-1 is trying to add task: 3
task 3 is added
pool-1-thread-1 is trying to add task: 4
task 4 is added
pool-1-thread-3 is processing task: 2
pool-1-thread-3 finished processing task: 2
pool-1-thread-3 is processing task: 3
pool-1-thread-3 finished processing task: 3
queue shutdown
queue shutdown
```


