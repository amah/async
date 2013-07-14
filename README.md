async4j
-------
asynchronous programming library that provides a set of callback based constructs (chain, try/catch, etc...) to ease composition of asynchronous operations.


Callback based asynchronous operation
-------------------------------------
As core concepts of async4j library, callback and asynchronous operations are defined as following

Callback
========
Callback is asynchronous operation listener and is defined a very simple interface :

```java
public interface Callback<R>{
  public void completed(R result);
  public void error(Throwable t);
}
```

* The generic parameter R is the type of the result od the asynchronous operations.
* the `completed()` method is called on normal completion of the asynchrnous operation with the result as parameter.
* the `error()` which will be called to report an exception during the execution of the operation.

Asynchronous operation
---------------------

```java
public void operation(Callback<R> k, P p /* more parameter if needed */)
```
Asynchrounous operation accept a callback object as parameter with following agreement:
* The callback passed as parameter MUST be called exactly one time using either `completed()` or `error()` method.
* The operation MUST NOT throw any exception. Errors must be reported through the `error()` method.

A template to implements asynchronous operation as define above is:
```java
public void operation(Callback<R> k, P p){
  try{
    R result = // Do some processing with your parameter p 
    k.completed(result)
  } catch(Throwable t){ k.error(t) }
}
```

Catching a Throwable is not usualy recommended but here any exception must be captured and reported through the callback to match the agreement.
In fact, the asynchrnous operation defined above is analogous to the usual synchronous call paradigm.  The `completed(R r)` method is the asynchronous 
form of `return r` instruction as both mark the end of the operation and are used to report the result to the caller. Regarding the exception management,
the `error(Throwable e)` method is the asynchronous form of exception bubble up mecanism provided by synchronous operations. The main difference between 
these two paradigm is the synchronous invocation require to keep the call stack until operation ends whereas asynchronous invocation do not. 





