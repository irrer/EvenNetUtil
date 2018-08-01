
The EventNetClientJVM library is a Scala API to facilitate connections with EventNet.

In general, the caller should begin by instantiating an EventNetClient object.
That instance will register with the EventNet monitoring service and start
a thread that responds to ping events with pong events.  It also automatically
sends a stop event to EventNet monitoring when the program exits.

The following methods may also be useful:

    EventNetClient.sendErrorEvent methods to report errors.

    EventNetClient.consumeDurable : consume messages in a loop on a durable queue

    EventNetClient.consumeNonDurable : consume messages in a loop on a non-durable queue
    
    EventNetClient.sendEvent : send an event
 
Messages received are automatically acknowledged.  If an alternate acknowledgement scheme
is needed then this API would have to be modified.   
    
To build the code, use the command: 

    mvn install