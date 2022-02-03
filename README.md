## Distributed Systems Search with Multithreaded Architecture 
###  *Java*
• Implemented a search application with an input double array of up to 2^32 elements (dependent upon heap size)
</br>
• Partitioned and distributed the array among 5 nodes, each with 8 cores
</br>
• Distributed the workload in each system utilizing 1 thread per core
</br>
• Reduced the total search time to 2.5% of the original run

### Table of Contents

- [Description](#description)
- [How To Use](#how-to-use)
- [License](#license)
- [Author Info](#author-info)

---

## Description
[Detailed Diagrams](https://docs.google.com/presentation/d/1LBytJ9_9IhutClPNqJBD7_YAJCPsVM5TO3V6dauEsFs/edit?usp=sharing)
- The is software will generate an array of doubles and find the kth smallest element in the array using parallel and distributed methodologies
  - The theoretical limit of the array size is Integer.MAX_VALUE
  if your heap is large enough to support an array of that size <br/>
- This software consists of both a server component and a client component
  - You may connect as many clients to the server as you would like,
  with regard for hardware limitations
  - It is recommended that each client be run on a separate machine to achieve the highest degree of efficiency
#### Initial Setup
  - The server generates the array of doubles
  - The server chunks the array based on how many clients will connect to it and distributes one chunk of the array to each client
#### Loop
  - The server repeatedly asks one client at random to pick a pivot value
    - If the client still has numbers in its chunk, the pivot value is chosen at random from the chunk and sent to the server
    - If the client does not have any numbers left in its chunk, the client notifies the server (via a -Integer.MAX_VALUE) that the server must choose another client to pick the pivot
      - The client that has run out of numbers in its chunk will then shut down and disconnect from the server 
  - Once the server has received the pivot value, the server distributes the pivot value to the other clients that did not choose the pivot and do not have the pivot in their chunk
    - These clients add the pivot value to the end of their chunk so that they can partition the numbers in their chunk around this pivot value 
  - The clients each [partition](#partitioning-process) their chunks in parallel using threads, producing a tally of how many elements are less than the pivot index (meaning they are less than the pivot value) post partition
    - The server receives this tally from each client and combines them to determine the total amount of elements to the left of the pivot index, with consideration for every client's chunk
  - The server then tells every client which side of their chunk to delete:
    - If the kth index > total left tally:
      - Save the left tally inside the server because the client will no longer be keeping track of the amount of left elements the server tells them to delete
      - Tell clients to delete the left side of their chunk
    - If the kth index < total left tally:
      - Tell clients to delete the right side of their chunk
  - Repeat the process until kth index == total left tally and then return that pivot value
    
#### Partitioning Process
- Values to the left of the pivot index in the end of the partitioning process will be less than the pivot value, while values to the right of the pivot index will be greater than the pivot value
- If the client chose the pivot, and it is not yet at the end of the chunk, it is swapped to the end position
- Each client divides the length of their chunk by the number of threads they have available to them to determine each thread's chunk size
  - The final thread's chunk will be the standard chunk size + the modulo of elements that would not fit evenly in a standard chunk
  - The last element of the final chunk will be the pivot value
  - See these [diagrams](https://docs.google.com/presentation/d/1LBytJ9_9IhutClPNqJBD7_YAJCPsVM5TO3V6dauEsFs/edit?usp=sharing) for more detail on this process
- The pivot value is then added to the end of every other thread's chunk
- Every thread partitions its chunk around the pivot value 
- The client's main, singular chunk is then reconstructed in three steps
  1. For each thread's chunk, the numbers before the pivot index are added back to the main singular chunk
  2. The pivot is then added back to the main singular chunk
  3. For each thread's chunk, the numbers after the pivot index are added back to the main singular chunk
- Once the array is fully reconstructed, a tally of how many elements are to the left of the pivot index is sent to the server
#### Technologies

- Java
  - Client & Server
    - Socket
    - Object Input Stream
    - Object Output Stream 
    - Array
    - ArrayList
    - List
    - Math.Random
    - IOException
  - Server Only
    - Server Socket
  - Client Only
    - Executor Service 
    - Executors 
    - Execution Exception
    - Future

[Back To The Top](#table-of-contents)   

---

## How To Use
### Installation
- Download the .zip file
- Extract the .zip file and open it in your preferred IDE to edit Java code
### Server Setup
- **Server:** Change the following value to the number of clients you wish to run
```java
static int numOfClients = 5;
```
- **Server:** Change the following value to the kth smallest value you  wish to find in the generated array
```java
static int kth = 1_501_000;
```
- **Server:** Change the following value to change the amount and range of numbers generated
```java
static int numOfMasterNums = 10_000_000;
```
### Client Setup
- **Client:** Change the following string to the IPv4 address of the computer you are using to run the server 
```java
Socket clientSocket = new Socket("Enter IPv4 Address Here", 6789);
```
- Do the same on every machine you wish to run the clients on
  - You may edit your IDE's run configurations if necessary to allow you to run multiple clients on one machine
### Run
- Run the TCPServer.java file on one machine
- Run the TCPClient.java file on every other node
---

## License

MIT License

[Back To The Top](#table-of-contents)

---

## Author Info
I completed this project in my sophomore year of college at California Lutheran University (2019). </br>It has since been updated several times and the efficiency has been improved. 
- [LinkedIn](https://www.linkedin.com/in/nicholas-sarullo)
- [Portfolio](https://nsarullo.github.io/portfolio)

[Back To The Top](#table-of-contents)
