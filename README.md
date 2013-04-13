asynk
=======
asynchronous programing library that provides a set of callback based constructs to ease composition os asynchronous operations.


Callback based asynchronous operation
=====================================
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
* the `completed()` method to be called on normal completion on the asynchronous call. It accepts the operation result as parameter.
* the `error()` which will be called to report an exception during the execution of the operation.

Asynchrnous operation
---------------------
```java
public void operation(Callback<R> k, P p /* more parameter if needed */)
```
Asynchrounous operation accept a callback object as parameter with following contract:
The callback must be called exactly one time using either `completed()` or `error()` method.








