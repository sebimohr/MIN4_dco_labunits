# Actors Lab Unit "Toll" (Persistence With Event Sourcing)

> **N.B.** This repository is both: a solution to the previous *Supervision lab* (see [remarks](REMARKS.md)) and a new lab to implement a persistent actor with snapshot functionality.
> 
> If you don't want to re-use your supervision and discovery project, just check-out this one an take it from here. 

## Your Tasks

### Truck Actor with Persistent Behavior

You have to implement the `TruckActor` with an **EventSourcedBehavior** based on the given details below.

The `TruckActor`'s request/command messages are already defined:

```scala
sealed trait TruckActorRequest
case class RouteUsage(position: String, km: Double) extends TruckActorRequest
case class RouteStart(position: String) extends TruckActorRequest
case class RouteEnd(position: String, km: Double) extends TruckActorRequest
````

All messages are sent from the `ChargingManagerActor`, which itself receives it from the `SensorActor` via the already
implemented behaviors from the last lab unit:

````scala
system ! "hello-A3-1"
system ! "hello-A93-1"
// uncomment "fail" message if you want to test "Actor Supervision"
//  system ! "fail-A3"
system ! "hello-A3-1"
system ! "hello-A93-1"
// uncomment "sensor" messages if you want to test "Actor Discovery" (and Stashing)
system ! "sensor-A93-1-R_AB_123-enter"; Thread.sleep(100)  // Ugh! Just simulation time needed for driving, otherwise truck gets messages unordered
system ! "sensor-A93-2-R_AB_123-usage"; Thread.sleep(100)  // Ugh! Just simulation time needed for driving, otherwise truck gets messages unordered
system ! "sensor-A3-1-R_AB_123-usage";  Thread.sleep(100)  // Ugh! Just simulation time needed for driving, otherwise truck gets messages unordered
system ! "sensor-A3-2-R_AB_123-exit";   Thread.sleep(100)  // Ugh! Just simulation time needed for driving, otherwise truck gets messages unordered
````

The normal order of `TruckActorRequest`s is (for example): 
- 1 `RouteStart("sensor-A93-1")` message
- 0..n `RouteUsage("sensor-A93-2", 5.60)` messages with a double value that represents the distance in km from the last position/event
- 1 `RouteEnd("sensor-A3-2", 9.82)` message with the last distance

After the `RouteEnd` message is received, the actor can create an `Invoice` and store it internally in a `List[Invoice]` 
(where the last invoices are kept; we do not send them anywhere here). 
Check the `case class Invoice(truckId: String, startedAt: String, exitedAt: String, km: Double, timestamp: Date)`, 
which is defined in [`TruckActor`](src/main/scala/de/othr/dco/actor/truck/TruckActor.scala).

> **N.B.** Message delivery is not guaranteed in Akka ("at least once"). The order of messages as seen from the `TruckActor` is not guaranteed either, since it receives it from different actors.
> We ignore this fact here and simulate the right order with `Thread.sleep`. In a real-life project the actor would have to deal with message ordering or missing messages!
> If you are interested in further details, see [Message Delivery Reliability](https://doc.akka.io/docs/akka/current/general/message-delivery-reliability.html).

### Taking Snapshots

Are there any appropriate events here that would make a good use case for snapshots?
If yes, implement it accordingly. If not, make snapshots every 200 messages.