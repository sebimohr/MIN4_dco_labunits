# Questions in lab units

## Lab Unit 1 - fork join pool

- What is the difference between `ForkJoinPool::invoke(ForkJoinTask)`, `ForkJoinPool::execute(ForkJoinTask)`, and `ForkJoinPool::submit(ForkJoinTask)`?
- What is meant by the `ForkJoinPool::commonPool`?
- Can you define a task using native `ForkJoinTask[V]`? If yes, how (what is different)? If no, give a short explanation why not.

## Lab Unit 3 - actors lab

- Does this setup has an advantage over a single-threaded solution? If so, explain what it is, if not, explain why not and what would need to change in order to get an advantage.

## Lab Unit 4 - actors webshop

- In a later stage the WebshopActor could be implemented as the interface to the webshop actor system:
    - What API technologies do you know and/or recommend to use here (and why)?
    - Are there any specific problems to be solved here, given the nature of actor systems or the actor model?
- Let's say the BasketActor or PaymentActor would use a blocking database API to store data to an external database.
Is this possible? If no, why not? If yes, are there any problems involved and how would you solve them?

## Lab Unit 5 - actors clustered

- How can you add or remove nodes to the cluster? How would you roughly implement it? (Check the documentation!)
- For the cluster-aware group routers: how do they learn about routees that join or leave the cluster? What do you need to implement (if at all)?
- In what way is "cluster-aware routing" different form "fork-join pools"? For what kind of "workers" or "tasks" is this appropriate? (Or not?)

## Lab Unit 6 - project whatsup

## Lab Unit 7 - actors toll persistence

## Lab Unit 8 - actors toll

