---
layout: default
title: async4j documentation
---

**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [title: async4j documentation](#title-async4j-documentation)
	- [Introduction](#introduction)
	- [Concepts](#concepts)
		- [Callbacks](#callbacks)
		- [Asynchronous function](#asynchronous-function)
		- [Asynchronous function template](#asynchronous-function-template)
		- [Synchronous vs Asynchronous call](#synchronous-vs-asynchronous-call)
	- [Controls](#controls)
		- [Future callback](#future-callback)
		- [Pipe](#pipe)
		- [Nesting asynchronous calls](#nesting-asynchronous-calls)
		- [Asynchronous condition](#asynchronous-condition)
		- [Asynchronous try / catch / finally](#asynchronous-try--catch--finally)
		- [Asynchronous while](#asynchronous-while)
		- [Asynchronous do while](#asynchronous-do-while)
		- [Asynchronous foreach](#asynchronous-foreach)
		- [IteratorAsync and EnumeratorAsync](#iteratorasync-and-enumeratorasync)
		- [ProducerAsync](#producerasync)
		- [Asynchronous Foreach](#asynchronous-foreach-1)
		- [Asynchronous parallel foreach](#asynchronous-parallel-foreach)
			- [Asynchronous Flow Controller](#asynchronous-flow-controller)

### Introduction
When it comes to address application responsiveness, one of possible 
optimization consists in moving to a background thread, long lasting 
operations which completion is not required immediately. The objective 
is to keep the critical execution path as short as possible. 
A well known example is the use of worker thread in GUI implementations 
to run long commands so the single UI thread (or Event dispatcher thread) 
is let focused on UI specific tasks. Looking at server side, 
the challenge is to limit the number of threads created in the system. 
Traditional  servers implementation are based on thread per request 
model and in context where processes are likely to perform blocking 
I/O operations or long running computation, the required number of threads 
are tightly correlated to the number of simultaneous requests. 
Increasing the maximum number of allocated threads on these servers can 
be the cause of a bottleneck as the underlying system may end in time 
consuming scheduling operations like context switch ( issue known as C10K problem). 
Even though some operating systems have been optimized to handle 
hundreds or thousands of threads, it is still possible to handle far higher 
load with frugal use of threads but with the adoption of asynchronous 
programming model as did in node.js or vert.x servers.

However developing application using asynchronous programming model 
is very challenging as it not fit well in the omnipresent imperative languages
where flow are generally synchronous. To make that easier, async4j makes
a clean separation between asynchronous flow and application logics and provides 
facilities to compose asynchronous operations easily using concepts 
borrowed from synchronous programming model.

### Concepts
The main concepts you have to understand before use async4j are callback and asynchronous function
as defined below. These concepts have nothing very new, but async4j introduce specific usage patterns 
you should understand to use async4j properly. 
 
#### Callbacks
The Callback interface is the core concepts of the async4j library and is defined as following:

{% highlight java %}
public interface Callback<R>{
  public void completed(R result);
  public void error(Throwable t);
}
{% endhighlight %}

It is passed to asynchronous functions where it used as an indirection of the processing completion
where the method `completed()` is used to notify a normal completion with a result of type 'R', and 
the method `error()` is used to notify exception if any. 

The callback is intended to implement asynchronous flow logics like if-then-else, try-catch-finally
and not application logic which should be coded in asynchronous functions as defined below.

#### Asynchronous function
An asynchronous function is a simple function which accept a callback object as parameter plus zero, 
one or more arguments and produce a result by calling the callback object with following contract:

* the callback object passed as parameter must be called one and only one time.
* on successful completion, the callback method `completed(R r)` must be called with the result object.
* on failure, the callback method `error(Throwable e)` must to report occurred exception. As a consequence
  the asynchronous function must not throw any exception at runtime.

In addition to the contract, it is highly recommended to perform these call at tail position as the callback 
notification marks the end of the asynchronous function execution.
 
In Java language, asynchronous functions are defined through Function[n]Async interfaces 
where [n] corresponds to the number of parameters passed in addition 
to the callback object. The general template is:  

{% highlight java %}
public interface Function[n]Async<T1,T2,...,Tn, R>{
	public void apply(Callback<R> k, T1 t1, T2 t2, ... , Tn tn);
}
{% endhighlight %}

The generic types `T1` to `Tn` are the n parameters respective types and `R` 
the type of the result that will be provided to the callback object.
async4j flow implementations use essentially asynchronous functions with zero or one parameter
which are deifned as following:

{% highlight java %}
public interface Function0Async<R>{
	public void apply(Callback<R> k);
}
{% endhighlight %}

{% highlight java %}
public interface FunctionAsync<T, R>{
	public void apply(Callback<R> k, T t);
}
{% endhighlight %}

The `apply()` asynchronous method has no return value and do not declare any Exception as both are supposed
to be routed to the callback object. Passing the callback as first parameter is a design choice to use a distinctive
place to let opportunity to have variable length arguments. The intention is also to discourage the use of inner
class implementation of callback interface with the temptation to include in application logic.

The asynchronous code template give some guidelines to code asynchronous functions

{% highlight java %}
public void operation(Callback<R> k, P p){
  try{
    // Application logic here
    R result = // some result value
    k.completed(result)
  } catch(Throwable t){ k.error(t) }
}
{% endhighlight %}

It is not advisable to catch `Throwable` but here the asynchronous call
contract do not allow exception to be thrown the the calling thread.

As you may mention in previous examples, the same name 'k' is used for all callback 
parameters to take advantage of variable shadowing effect to avoid to have more than
one callback object visible from asynchronous functions scope. Most of time, the asynchronous
function completion have to be notified to the closest callback object visible from the execution context. 

{% highlight java %}
// sample here
{% endhighlight %}


#### Asynchronous control of flow
Asynchronous control of flow refers to combination patterns of asynchronous functions which 
are building blocs for asynchronous programming model. They are implemented through 
callback objects where execution path decision is made when `completed()` or `error()` 
methods are invoked. async4j provides implementation of basics controls that are asynchronous 
equivalent of synchronous controls such as pipe, if-then-else, try-catch-finally, loops. 
These built-in controls are generally sufficient to build complex asynchronous programs, but it 
is still possible to implement specific controls that match your requirement if needed.


#### Callback hierarchy 
Callbacks structure implementing an asynchronous control are constructed with a reference 
to a parent callback which is called at the end of the control execution. That is, the 
asynchronous program runs with a hierarchy of callback object each node implementing 
specific control logic. That hierarchy is very similar to execution stack used behind 
the scene for synchronous function call which is transparently managed by the code generated by the compiler.
The callback object passed to the asynchronous function is the equivalent of the return address
of stack frame and which point to the instruction to execute when the current function 
completes. 

Regarding exceptions, they are bubbled up in callback hierarchy until they
are handled by a callback implementing a catch logic (see try-catch-finally construct).
That is analogous to the exception management controls natively found in programming languages.

Considering all these similarities, callback methods `k.completed(R r)` and `k.error(Throwable e)` will be refered
as asynchronous return and asynchronous throw respectively.


#### Asynchronism and and multi-threading
Generally asynchronous functions will delegate their body execution to a separate thread 
to free the calling thread as soon as possible. THat said, the use of separate thread is 
not a requirement, a function executing synchronously its body including invocation of callback 
using the calling thread can be considered asynchronous as long as it match asynchrony contract.
For instance, following functions samples are both considered as asynchronous function: 

{% highlight java %}
// Sample with and without thread
{% endhighlight %}

That make application logics and multi-threading almost separate concerns when you adopt 
asynchronous programming model especially with async4j. You can write your program focusing 
initially on your application logic, and during tests identify CPU intensive operations 
to assign them appropriate thread pool without changing fundamentally the code structure. 
That said, running synchronously an asynchronous function can in some cases cause stackoverflow error 
due to the accumulation of call to `completed()` or `error()` the execution stack. 
The tail call optimization would have help on this as callback will be invoked at tail position
most of time. Unfortunately there is no plan for its support in java. In some cases a like `EnumeratorAsync` data 
generator used in loops, a workaround is possible to avoid stack overflow error but that are not simple to implements.


### Built-in asynchronous controls
This section describe core asynchronous flow built in async4j library.

#### Future callback
It is the equivalent of the Future based asynchronous call where the
result is provided on the calling thread stack. The getResult()
method blocks until the end of asynchronous operation the
futurecallback is passed to, then it return a value or throws
exception depending on the completion status. The following helper
method used to call synchronously an asynchronous is implemented used
the FutureCallback as following

{% highlight java %}
public static <P , R> R call(P p, Task<P , R> task) {
	FutureCallback<R> k = new FutureCallback<R>();
	task.run(k, p);
	return k.getResult();
}
{% endhighlight %}

This call back is useful when a thread to be kept until the
asynchronous task end, the main thread if it is the single non daemon
thread in the jvm, unit test thread or threads bound to transactions
for instance. It is the sole callback object that do not have parent.

#### Pipe
The Pipe is a construct that combines two asynchronous operations by
calling them sequentially using the pipe callback.

{% highlight java %}
String s = Async.call(10, new PipeTask<>(new Task<Integer , Long>() {
		public void run(Callback<? super Long> k, Integer p) {
			k.completed(10 * 2L);
		}
	}, new Task<Long, String>() {
		public void run(Callback<? super String> k, Long p) {
			k.completed(p.toString());
		}
	}));
{% endhighlight %}

On invocation, the pipe construct delegate the call to the first task using a PipeCallback created with initial callback as parent and 
a reference to the second operation. On successful completion of the first operation, the PipeCallback use the output value along with
the parent callback to call the second operation. When the first operation ends with error, the PipeCallback forward the exception to the parent
callback, the second operation is not invoked.

In the example above two tasks have been chained that is quite simple. More than two tasks can been chained like exemple below:

	// four chaining example here

It is the asynchronous equivalent of

	// synchronous equivalent code.

Here again simple control. The difficulties come when a task need more than one value whitch come not only from preceding task. For exemple consider folowing synchronous exemple:

	// synchronous example with multiple input

To the asynchronous version of the example we will need to convey the d value through the second task to send it to the last task. To do that in Java language a structure class need to be created to bold theses two values but as may guess doing that will end with combersome parameter classes. That is a sample use case where Tuple in Java would help like in Scala. Actually, Scala bring more than than Tuple, the delimited continuation which simplify considerably asynchronous operation chaining.

	// example scala

#### Nesting asynchronous calls
Asynchronous operation may call another asynchronous operation in a way that match rules stated above.

{% highlight java %}
public void operation(Callback<R> k, P p){
  try{
    // Do some processing with your parameter p 
    anotherOperation(k, p, ...)
  } catch(Throwable t){ k.error(t) }
}
{% endhighlight %}

In The callback object is delegated to the `anotherOperation()` which will take the 
responsability to call methods on the callback object which means the value returned 
asynchronously to the initial caller is one provided by ̀anotherOperation()̀. 
The try/catch is needed to report any exception that occurs in the operation proper code.
The call to the nested asynchronous operation is the last instruction (tail call actualy)
to make sure only one error is reported to the callback object. In fact the nested
asynchronous operation is at tail call is a tail call that mey occured before the
call of `anotherOperation()`

#### Asynchronous condition

It is the asynchronous form of the if else blocs found in programming languages. 
It combine two asynchronous operations where the first as the condition that returns 
a boolean and the second the operation to run when the boolean value is true.
the asynchronous condition flow logic is implemented using ConditionCallback. 
It holds reference to the body operation that is invoked when a boolean value 
true is passed to the `completed()`  method. out pipe described above as that is 
it chain two asynchronous operations, the first one returns a boolean value. 
The second operation is invoked only when the first one return true, otherwise 
the flow is passed to the parent callback on the stack.


#### Asynchronous try / catch / finally

The asynchronous exception handling using callback based controls is very 
similar to the try catch finally block natively provided in existing programing languges. 
It consist in asynchronous try block that represents the application logic subject 
to a failure, the catch block which is called on exception and the finally which 
is called whatever status of previous blocks is. 

The asynchronous catch block accept a Throwable object as paramater and may rethrow 
an exception or return normally a value. The finally block has no parameter other 
than the callback object and returns only void value. The intent is to simplify 
the prototype of final blocks. Asynchronous exceptions thrown from the finally 
block are bubbled up to the parent callback.

#### Asynchronous while
The asynchronous while is composed of condition and body that are asynchronous task both. 
The condition take an input value of a type that match boby output type. Basically, 
The body is called while the condition return the boolean value true and the loop ends 
when condition retirns value false or an exception occured, very similar to language while loop.
// to complete side effect context

#### Asynchronous do while
The asynchronous repeat is similar to the asynchronous while but with one difference, 
the body is called first. It is the same logic as those implemented in programming language.

#### Asynchronous foreach
The general principle of the foreach loop is iterate over as set of item and call an operation for each of them. 
In the async4j library, the set of elements are represented by two models of asynchronous data generators, 
pull and push based generator respectively represented by Enumerator and Producer interfaces.

#### IteratorAsync and EnumeratorAsync
IteratorAsync and EnumeratorAsync are pull based generators as they provide each element on demand by calling 
appropriate asynchronous method next() methods. IteratorAsync is the asynchrone equivalent of Iterator from 
collection java collection API where synchronous methods hasNext() and next() methods and repaced 
by thier asynchronous equivalent `hasNext(Callback<Boolean> k)` and `next(Callback<E> k)`. 
EnumeratorAsync is the campacted form of IteratorAsync where methods hasNext and next are combined 
into a single method next() that take Callback2 callback interface which accept two values on completion. 
The first returned value is of type boolean and indicates whether an item is returned or not:


* if true the second value of generic type T can be used as an element even it is null.
* a false value means no more element is available from the source and The second value must be ignored whatever it is null or not.</li>

#### ProducerAsync
The push data source model is specified by ProducerAsync interface that define the asynchronous method ´produce()´ which take 
the ConsumerAsync interface as element handler or consumer:

// element handler code here

When the method produce is called, the producer submit each element to to the consumer through the asynchronous 
`consume()` method of the consumer which returns asynchronously at the end on element processing using the 
callback provided in the parameter. Depending on implementation, the Producer may generate more than one elements 
simultaneously and means Element handler can be called concurrently. 

The notifcation of the callback passed to the Producer.produce() method marks the end of element generation.

#### Asynchronous Foreach
This construct iterate  over elements  and sequentially call an asynchronous  iteration function for each. 
That is, the iteration funtion complete before proceed to the next element.


The asynchronous foreach flow logic is implemented using combination of two callbacks the nextcallback and iterationcallback.
The nextcabk is passed as completion handler to the method Enumerator `next()` to request the the next element from the asynchronous iterator. 
Element returned if any is passed asynchronously to iteration task using the iteration callback as completion handler. 
On the completion of the iteration task, iterationcall ack  call the `next()` using the nextcallback to continue the loop. 
The cycle continue until the enimerator returns no element or and  exception occured, the flow is then back to the parent 
callback through `completed()` or `error()` methods respectivelly.

Here is an exemple of Socket reading stream

// exemple here

#### Asynchronous parallel foreach
Like the foreach flow discussed previously, the parallel foreach iterate over elements and call an iteration for each. 
The main difference lie in the of Producer as source of elements which are processed as they are submitted to the 
consumer this construct implements.

But the difference is elements generations and iteration task executions are decoupled in two separate asynchronous flows of executions, one for element generation from producer and the other for elements processing. That is, element generation are not directly coupled to completion of the iteration task and more than one iteration task can being executed at the same moment. 
When called, the parallel foreach construct initiate elements generation by calling produce() method passing an implemention of consumer that call iteration task on a separate asynchrone flow using a callback that make a separate branch of execution flow.

// picture of parallel branches

The parallel foreach is completed when the `Producer.produce()` and all iteration tasks call are completed. 
Hereunder an example of parallel foreach code

// exemple without flow control

It use and iterator producer that generates elements from synchronous iterator implemented by a range generating number. The application logic implemented in the iteration task is quitte simple, it just cumulate numbers values for consistancyr check. The iteration task is wrapped with an ExecutorTask by DSL function withPool() to have parallel actual parallelism as this construct has no internal thread. In fact the foreach loop can work without thread pool as following:

// exemple without thread pool

In this specific case where only the calling thread will execute the code, instructions are executed sequentially and there is no parallelism. 
In some use cases, the producer can be the source of parallelism like the JmsListenerProducer. When the underlying Jms system is configured to deliver more than one message at the same moment, iterations can be executed in pararel way:

// jms producer example

Obviously, Producer and Consumer can be both source of parallelism:

// jms producer with withpool

These exemples show how flexible is the parallel foreach cinstruct in regard to the thread pooling by decoupling the element generation flow from processing's one. However that open the flow control question discussed in the next section.
In some situation, producer can generate elements faster than Consumer can process and male system overloaded.
The chalenge with the parallel foreach constuct the differeitem generation controle as the consumer may process elements at a rate less than what Producer generate. Following approaches help on this.
Producer auto regulation
A producer is auto regulated when the number of pending calls to the Cosumer consume() method is limited. One exemple of auto regulated In that case, the producer has a finit limit of pending consume() call. For instance a Producer composed of a finite set of Asynchronous foreachs loops that iterate sequentially over Enumerator to submit elements to the consumer.
Stop the producer
The idea is to send a stop signal to the Producer when the consumer get overload and sends restart signal when lowest threadhold is reached. 

##### Asynchronous Flow Controller
The flow controller is an asynchronous task interceptor that manage the number of concurrent invocations of a task given the limitation of system resources like like available memory, number of cpu / core. The limitations may also depends on external factors like database maximum number of connections or the maximum number of concurrent requests allowed by a web server. 
The flow control is implemented by observing the load indicator before and after the controlled task. On invocation, the flow controller check the load indicator, than loareach the pre defined limit it buffering when the load of resource reach the maximum limit, asynchronous parametrs including the callbak object Are staged in memory which has effect asynchronous call blocking As the callback object is not notified. Staged call are resumed when the load dicrease below the predefined limit, the flow controller then recall the subsequent task with the same parameters.
The async4j provides some implementztion of flow controller described below.
* NoFlowController juste delegates calls to the wrapped task without any staging
* MaxjobFlowController limits the number of concurrent t calls to the wrapped task. When the limit is reached calls are staged in Concurrent non blocing queue, and resumed once some call terminates. Note that there is no priority defined between the new calls and staged ones.
* EnumeratorController: it is an optimized flow controller optimezed for Producer that generate element sequencially. In this case A single reference is used as staging instead of queue as there is no concurrent.

// data and task parallelism

