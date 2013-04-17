async4j
=======
asynchronous programming library that provides a set of callback based constructs (chain, try/catch, etc...) to ease composition of asynchronous task.


Callback based asynchronous tasks
=================================
The core concepts of asynk library are callback and asynchronous operations

Callback
--------

```java
public interface Callback<R>{
  public void completed(R result);
  public void error(Throwable t);
}
```

Callback is a very simple interface that define two methods:
* the `completed()` method to be called on normal completion of the operation with the result. In Java language the result may be type as Void to indicate no value.
* the `error()` which will be called to report an exception during the execution of the operation.

Asynchrnous operation
---------------------

```java
public void operation(Callback<R> k, P p /* more parameter if needed */)
```
Asynchrounous operation accept a callback object as parameter with following agreement:
* The callback MUST be called exactly one time using either `completed()` or `error()` method.
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

In some situation a asynchronous operation may call another asynchronous operation:

```java
public void operation(Callback<R> k, P p){
  try{
    // Do some processing with your parameter p 
    anotherOperation(k, p, ...)
  } catch(Throwable t){ k.error(t) }
}
```
In this template the callback object is delegated to the `anotherOperation()` which will take the responsability to call methods on the callback object. The try/catch is still needed to reporte errors that mey occured before the call of `anotherOperation()`.




