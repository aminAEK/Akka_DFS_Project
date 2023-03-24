# Simple distributed store system (aka Distributed File System)
---
* Imane AKDIM
* Amina AIT ELKADI
---- 
** In this project, we choose to write our code with scala **

## Day 1
--- 

### Part 1:
* In this part we implement a store system with 3 operations; Store(key, value), Lookup(key), and Delete(key), assuming that keys and values are strings.
* We provide a very simple implementation of the system based on an Key-Value HashTable, the user can issue the operations through the terminal, and this service is entirely maintained in-memory.
* The implementation is present on the file named "Day1Part1.scala", to run it do:

		sbt "runMain com.example.Day1Part1"

* You will be prompted to give a command (store, lookup, delete).
Example for store command: 

		[info] Enter a command (store, lookup, delete):
		>> store
		[info] Enter a key:
		>> 8
		[info] Enter a value:
		>> 7
		[info] Stored key 8 with value 7.


-  when the user accidentally types an incorrect command an error message is displayed saying: 

		[info] Unknown command.
		
-  If the user accidentally types Ctrl-c: The data in the store is lost.
		

### Part 2:
* In this part, the server and the user-terminal runs in different process, we implement the Key-value store on an independent actor called , and the console reader on an independent actor.
* The implementation is present on the the file named "Day1Part2.scala", to run it do:

		sbt "runMain com.example.Day1Part2"

* You will be prompted to give a command (store, lookup, delete).

## Day 2
---

### Part 1:
-  In this part, we implement a durable-actor which implements the key-value abstraction with an append-only file that we called "storeD2P1.txt". 
	- When the user stores an element, we append the (key, value) pair at the end of the file,
	- When the user deletes an element, we add a (key, deleted) marker at the end of the file,
	-  When the user looks up a key, we traverse the file back to front to find the first non-deleted value, if any.
- The implementation is present on the the file named "Day2Part1.scala", to run it do:

		sbt "runMain com.example.Day2Part1"

* You will be prompted to give a command (store, lookup, delete).
* To test it, start with a store command followed by a delete command of the same key, then a lookup of the key deleted:

		[info] Enter a command (store, lookup, delete):
		>> store
		[info] Enter a key:
		>> 5
		[info] Enter a value:
		>> 9
		[info] Stored key
		[info] Enter a command (store, lookup, delete):
		>> delete
		[info] Enter a key:
		>> 5
		[info] Deleted key
		[info] Enter a command (store, lookup, delete):
		>> lookup
		[info] Enter a key:
		>> 5
		[info] The key is not in the file
* The file where the (key,value) pairs will be stored is: "storeD2P1.txt".

- If our server crashes now, we still have the data stored in the file.

### Part 2:
* Consider the complexity of a lookup operation, the best case complexity is O(1) if the user finds the key in the last position of the file , and the worst case complexitie is O(n) if the user had to search all the file.
* As the file grows, the performance of traversing the whole log becomes a bottleneck, to improve the performance, we, alongside the file, keep an in-memory key-value store implemented in a different actor.
* The implementation is present on the the file named "Day2Part2.scala", to run it do:

		sbt "runMain com.example.Day2Part2"

* You will be prompted to give a command (store, lookup, delete).
* The file where the (key,value) pairs will be stored is: "storeD2P2.txt"

## Day 3
---

### Part 1:
* If we have a lot of keys with large values, one of the bottlenecks that we will have is that large values may consume too much memory, which can slow down the system and cause memory issues. We can solve this bottleneck by introducing an in-memory cache that sits between the actors and the data store. The caching actor can store frequently accessed data in memory, reducing the need to access the data store and minimizing the amount of data stored in memory.
* When the user will launch a lookup request, the system will look for it in the cache first, if it doesn't exist there, it'll look in the in-memory key-value store, if it doesn't exist either, it searches in the stored file.
* If we have super-fast key-value stores that can store at most 1000 keys, we can use these actors to improve the overall performance of our system by dividing the data across multiple actors, each responsible for a subset of the keys. This will reduce the amount of data each actor needs to manage and improve performance.
* Implementing multiple caching actors can significantly improve the performance of the system by distributing the load, reducing contention, improving fault tolerance, and increasing scalability.
* The implementation is present on the the file named "Day3Part1.scala", to run it do:

		sbt "runMain com.example.Day3Part1"

* You will be prompted to give a command (store, lookup, delete).
* To test the speed of the cache, give a store command followed by a lookup command:

		[info] Enter a command (store, lookup, delete):
		>> store
		[info] Enter a key:
		>> 8
		[info] Enter a value:
		>> 3
		[info] Stored key 8
		[info] Enter a command (store, lookup, delete):
		>> lookup
		[info] Enter a key:
		>> 8
		[info] Found key 8 with value 3 (from cache)

* The file where the (key,value) pairs will be stored is: "storeD3P1.txt"

### Part 2:
* In this part, we implement a client program, that makes a large number of random requests to the storage service, the keys are from 1 to 1000. 
	* We start by populating the storage with values for keys from 1-500
	- Then a random operation from store, delete, and lookup will be picked. 
		- If the operation is a store or delete, the key picked will be greater than 500
		- If the operation is a lookup, the last key looked-up will be picked 50% of the time. And if we have to look up a new key, the sampling  bias towards the first 1-500 with a percentage of 70%.
- To prove the impact of the caches in the performance of the system, we implement 3 layers of caches. When the user will launch a lookup request, the system will look for it in one of the caches (randomly) first, if it doesn't exist there, it'll look in the in-memory key-value store, if it doesn't exist either, it searches in the stored file.  
- The implementation of the system **with the cache** is present on the the file named "Day3Part2a.scala", to run it do:

		sbt "runMain com.example.Day3Part2a"

* The commands will be automatically sent.
* At the end of the execution, the total time of execution will be printed.
* The file where the (key,value) pairs will be stored is: "storeD3P2a.txt"

- The implementation of the system **without the cache** is present on the the file named "Day3Part2b.scala", to run it do:

		sbt "runMain com.example.Day3Part2b"

* The commands will be automatically sent.
* At the end of the execution, the total time of execution will be printed.
* This file does not implement the cache so the execution is significantly slower.
* The file where the (key,value) pairs will be stored is: "storeD3P2b.txt"

- The implementation of the system **with 3 caches** is present on the the file named "Day3Part2c.scala", to run it do:

		sbt "runMain com.example.Day3Part2c"

* The commands will be automatically sent.
* The file where the (key,value) pairs will be stored is: "storeD3P2c.txt"

- We noticed that the version with the caches (3 caches is the best) gives better results compared to the one without the cache, because it reduces the need to access the data store simultaneously.

## Day 4
--- 

### Part 1:
- To avoid the problem that with a single actor system, an error on the part of the client can crash the whole system, we make each of the actors run on a separate actor system, and process. This involves:
        1.  The storage logging actor is a single process
        2.  The centralized key-value store is a single process
        3.  The cache actors run in their own process
        4.  The client (or console) runs on an individual process.
-  The implementation  is present on the the file named "Day4.scala", to run it do:

* Open 4 terminals and type the following commands in the following order.
In terminal 1: 

	sbt "runMain com.example.Main1"
In terminal 2: 

	sbt "runMain com.example.Main2"
In terminal 3: 

	sbt "runMain com.example.Main3"
In terminal 4: 

	sbt "runMain com.example.Main4"
* Check the logs of the 4 terminals.
* The file where the (key,value) pairs will be stored is: "storeD4.txt"

- We set the port configuration of each process in the "application.conf" file  

### Part 2:
* If the storage logging actor crashes, the system will continue to operate normally, but any data that was not logged before the crash may be lost. To bring the system back to operation, we will need to restart the storage logging actor and restore any lost data.
* If the centralized key-value store crashes, the entire system will be down. To bring the system back to operation, we will need to restart the centralized key-value store and restore any lost data. We can also implement replication to ensure that if one instance of the centralized key-value store fails, another instance can take over.
* If a cache actor crashes, the system will continue to operate, but the cache will be empty until the cache actor is restarted. To bring the system back to operation, we will need to restart the cache actor and restore any lost data.
* If the client crashes, it will not affect the operation of the system.
* To ensure the system remains operational, we will need to monitor each component closely and have plans in place to restore lost data and restart any failed components.

---


## Note:
A Jar containing all the dependencies created using "sbt assembly" can be found in 

	|----src
	|----target
		  |----scala-2.13
		  	|----akka-quickstart-scala-assembly-1.0.jar