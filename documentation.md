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


#### Asynchronous tail call optimization
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
The `FutureCallback` holds the completion status of asynchronous function call
and provide the result or exception through the method `get()` which 
blocks until the asynchronous function completes. 
It is the sole callback object that do not have parent callback.
 
The `FutureCallback` is similar to the standard `java.util.concurrent.Future` as both
provide synchronously the output of the asynchronous call. The difference 
that, it is up to the asynchronous function caller to provide the future object and not the 
invoked asynchronous function to return it. The possibility of cancellation that is exposed 
on the `java.util.concurrent.Future` interface is not supported by `FutureCallback`.
 
The `FutureCallback` is useful when you want to keep a thread until the
asynchronous function end, like in the main method when you all other threads 
in the JVM are daemon, or in the context on unit test where the thread calling the 
test should be blocked until the completion of the asynchronous test scenario.
The `Async.call()` is a helper method to call synchronously an asynchronous function:
 
{% highlight java %}
public static <P , R> R call(P p, Task<P , R> task) {
	FutureCallback<R> k = new FutureCallback<R>();
	task.run(k, p);
	return k.getResult();
}
{% endhighlight %}


#### Asynchronous pipe
The `PipeAsync` is a composite function made of two asynchronous functions to be
executed sequentially. On `PipeAsync` invocation, the first function is called 
with `PipeCallback` as callback parameter which is constructed with constructed
with a reference to the second asynchronous function. When the first function complete
successfully, it call the 'PipeCallback.completed(R r)`. Then 'PipeCallback` call the 
second function passing it the parent callback and the output of the first function.
On error the first function invokes the method `PipeCallback.error(Throwable e)` which 
bubble up the exception to the parent callback, the second function is not invoked.
The type parameter of two functions must match i.e. the output
type of the first function must be compatible with the input parameter of the second function.

Hereunder an example:

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



It is possible to chanin more than two asynchronous function as shown in the following example:

	// four chaining example here

The pipe flow will be generally used to define a sequence of asynchronous operations similar 
to usual a sequential invocation synchronous functions. The example above translated to synchronous
operation will looks like this:
  
	// synchronous equivalent code.

The difficulties come when a function in the sequence requires parameters that are result of more that one preceding function. 
Consider following synchronous example where the last function require the output of the two preceding ones:

	// synchronous example with multiple input

Possible solution to handle these case are following:

##### Create context object
The context object will gather all required input/output values of asynchronous functions:

	// synchronous example with multiple input

The drawback of this approach is that the function signature is bound to the call context. 
It will be hard to make these asynchronous function reusable.

##### Nesting asynchronous functions
This approach is based on closure capture functionality provided in Java language
to make preceding outputs available to the function being called. The calculation flow
would looks like this:

// nested example here
 
Nesting callback or asynchronous functions is something async4j do not advise, 
as it could make the code unreadable and hard to maintains.

##### Delimited continuation
The delimited continuation would be a clean solution the asynchronous function chaining
but is not supported in Java when this document is written.
The idea is to wrap the rest of the processing in a callback
object that will be passed to the asynchronous call. At the end, that is equivalent to
to the asynchronous function discussed above but here it is transparent and code remain linear
readable and maintainable. 

// Java example with continuation
// Or sacala example with continutation



#### Asynchronous condition

The `IfAsync` implement an asynchronous for of the if-then-else bloc.
It is a composite function that combines two asynchronous functions,
the first one is an asynchronous predicate that returns a boolean value 
as result and an asynchronous body function that is called when the predicate
returns `true`.
This control is implemented using `IfCallback` which is constructed with
a reference to the body function. When the control is invoked, it call
the predicate with the `IfCallback` as parameter. On successful completion 
of the predicate, it will call method `completed(Boolean b)` which will
invoke the body if the boolean value is `true`, otherwise the flow is passed to 
the parent. Any reported exception is bubbled up to the parent callback.

// Condition sample here (test page on google and load first)


#### Asynchronous try / catch / finally

The asynchronous exception handling using callback based controls is very 
similar to the try catch finally block natively provided in existing programming languages. 
It consist in an asynchronous function to be tried, the catch asynchronous function
which is called on exception and the finally function which 
is called whatever status of previous blocks is. 

That implemented through `CatchCallback` and `FinallyCallback` which listen to
normal completion or exception to take appropriate action.

// Example here
 
The asynchronous catch block accept a `Throwable` object as parameter and may `rethrow` 
an exception or return normally a value. The finally block has no parameter other 
than the callback object and return no value. The intention is to simplify 
the prototype of final blocks. Asynchronous exceptions thrown from the finally 
block are bubbled up to the parent callback and hides original exception if any.

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
pull and push based generator respectively represented by `IteratorAsync` (or its compacted form `EnumeratorAsync`) 
and `ProducerAsync` interfaces.

#### IteratorAsync or EnumeratorAsync
`IteratorAsync` is pull based generator as it provides each element on demand by calling 
asynchronous method `next()`. It is equivalent to the synchronous `java.util.Iterator` 
where methods `boolean hasNext()` and `E next()` are replaced by their asynchronous 
equivalent `hasNext(Callback<Boolean> k)` and `next(Callback<E> k)` respectively. 

// interfaces here

`EnumeratorAsync` is the compacted form of `IteratorAsync` where methods `hasNext()` and `next()` are combined 
into a single method `next()` that take the `Callback2<Boolean, E>` which is used to returns simultaneously a boolean
indicating whether the next element exists and the element itself.

Interfaces `IteratorAsync` and `EnumeratorAsync` can be used to represent a wide range of asynchronous stream of data
like asynchronous input channel in Java NIO.2., asynchronous server socket etc...


#### ProducerAsync
`ProducerAsync` interface is a push data source model where elements are published to a listener 
defined by the `ConsumerAsync` interface:

// Producer Async interface here
// Consumer Async interface here

The method `produce()` starts generation of elements which are submitted to the consumer through the 
method `consume()`. Depending on implementation, the producer can generates simultaneously more than one elements 
which means that the consumer may be called concurrently.

The producer interface can be used as abstraction for event/message sources like JMS queue or topics. It can be also 
used as a wrapper for multiple asynchronous iterator when you want to process all their items using the same foreach loop.

 
#### Foreach
The foreach control iterates over elements of an `IteratorAsync` and sequentially call an asynchronous iteration body function for each. 
The sequential call means the loop proceed to the next iterator element after the completion of the body execution with the previous element.

// For each sample here

Internally, the foreach control make use only `EnumeratorAsync` interface. If an `IteratorAsync` is passed as parameter, it is wrapped 
in an adapter that implements  `EnumeratorAsync` interface. 

The foreach control initiates an asynchronous invocation cycle that involve successevely the enumerator `next()`, `NextCallback`, iteration body 
and  `IterationCallback`. At the beginning of the loop, the enumerator `next()` is called with the `NextCallback` which method 
`completed(Boolean b, E e)` is invoked on completion. If an element is found, that is the boolean value `b` is `true`, the body function
is called with an `IterationCallback` and element `e` as parameters. At the end of the body function it invokes the iteration callback 
which call the enumerator `next()` method to restart the cycle. If an exception occures during the cycle, it is bubbled up to the parent callback
and the loop ends.
 
The foreach control is use to process asynchronous stream of data especially when element must be processed sequentially.  

// exemple here


#### Asynchronous parallel foreach
Like the foreach flow discussed in the previous section, the parallel foreach iterate over elements and call an iteration body function for each. 
The difference is that elements generations and thier processing with the body function are not sequenced asynchronous processes. In the most general
case, the producer and body function can generate or process several elements concurrently at the same time. This control requires
a loop controller defined by the interface `LoopController` which handle parallelism concerns such as multi-threading or flow control. 
In fact, the parallel foreach do not implement parallelism by itself but defines an asynchronous execution structure as a framework 
for parallel execution of the body function.

To be noticed, the built-in loop controllers currently provided in async4j address flow controls only, multi-threading concerns
will be covered in coming releases. For now, the multi-threading can be introduced
explicitly in the loop structure by submitting body function to a thread pool for execution, see 'Async.withPool()` helper method.


The following example show a simple use case of a parallel loop: 

// Example 


The loop is initiated by a call to the producer method `produce()` to generate elements to be processed by the loop. 
Each element generated is passed to loop controller which implement the strategy to use for the 
body function invocation. For instance it may call the body function in separate thread for parallelism
or delay the call if the limit of maximum number of parallel execution is reached. The loop completes 
when the `produce()` method and all body function calls completes.

// picture of parallel branches


// exemple without flow control

It use and iterator producer that generates elements from synchronous iterator implemented by a range generating number. 
The application logic implemented in the iteration task is quitte simple, it just cumulate numbers values for consistancyr check. 
The iteration task is wrapped with an ExecutorTask by DSL function withPool() to have parallel actual parallelism as this construct has no internal thread. In fact the foreach loop can work without thread pool as following:

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

