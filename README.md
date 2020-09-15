# Snapshots-on-a-P2P-Distributed-System
Implementation of a <b>snapshot algorithm</b> that retrieves the current total balance (of bitcakes - currency) in a distributed peer to peer system in which a large number of transactions are constantly being made. The snapshot algorithm is a mix between the <b>Lai Yang - Li</b> and the <b>Spezialetti-Kearns algorithms</b>.

## Overview
The peer to peer network is a graph where communication between nodes is <b>asynchronous and non-FIFO</b>.<br>
Two nodes can communicate only if they are connected (adjacent) on the graph. <br>
Each node has a starting bitcake amount. Constant transactions rapidly change this value, which in addition to the asynchronous and non-FIFO communication makes it hard to retrieve the current total currency in the system, due to late or unreceived transaction messages at the time of computation. <br>
This problem is solved by a mix of two algorithms:
1. Lai Yang - Li variation (enables each node to perform multiple snapshots at a time)
2. Specialetti - Kearns (enables multiple nodes to perform a snapshot concurrently by working together)

The system supports <b>scripted</b> launching, for running multiple nodes simultaneously and providing input commands to nodes using text files as input. 

## Technical details
To run the system, a MultipleServentStarter class is provided. This class starts separate Node programs using the <b>ProcessBuilder</b>.<br>
The user specifies a network graph in the config file which the MultipleServentStarter reads and provides Nodes with their specified port and id number via the program arguments.<br>
Also the System.out, System.err and System.in are redirected to files /output/serventID_out.txt, /error/serventID_err.txt and /input/serventID_in.txt, to allow the user to supply all nodes with input commands simultaneously. <br>
The user can also interact with nodes using the CLI (command line interface). <br>
The sending of each message is <b>delayed</b> by a small random amount to <b>simulate a realistic distributed system</b> (because the system is tested locally on one machine).

### Lai Yang - Li
This algorithm is able to compute the correct total balance by storing a separate history, for all potential snapshot initiators, of all sent and received transaction messages for all adjacent (neighbor) nodes. This history is used to detect unreceived transaction messages, and add them to the total balance.

### Specialetti - Kearns
This algorithm enables multiple nodes to compute the snapshot concurrently by forming so called "regions". When a node receives a snapshot message from a certain initiator, if it's the first snapshot message it receives during concurrent snapshot initiation, it saves that initiator and belongs to "his" region. All other initiators are declined and a border is formed with their regions. So once these regions are formed, snapshots are computed within them, after which the initiators exchange results in multiple rounds (starting with neighbor regions) until the final result is formed (all regions exchanged results).

### Supported commands:
* pause X (pauses the CLI for X amount of seconds, useful for timing certain input commands during testing)
* transaction_burst (sends a burst of 5 transaction messages (random small amount of bitcakes) to all neighbors)
* bitcake_info (initiates a snapshot)
* stop (stops the node)

### Properties file (config):
Parameters are read and set during application launch and cannot be changed during operation.<br><br>
File structure:<br><br>
servent_count=16 - number of nodes in the system<br>
clique=false - (not important, leftover from past implementation)<br>
fifo=false - non fifo communication<br>
snapshot=ly - (not important, leftover from past implementation)<br>
servent0.port=1100 - port numbers<br>
servent1.port=1200<br>
servent2.port=1300<br>
...<br>
servent0.neighbors=1,2 - graph connections defined<br>
servent1.neighbors=0<br>
servent2.neighbors=0,3,4<br>
...<br>
servent0.init=true - only initiator nodes can initiate a snapshot<br>
servent1.init=false<br>
servent2.init=false<br>
...

## Usage example:
In this example there are 16 nodes in the network, 4 of them are initiators (nodes 0, 6, 10, 14). <br>
Each node started with 1000 bitcakes, so there are 16000 bitcakes in the system. <br>
If the algorithm works correctly the snapshot result should always be 16000, no matter how many transactions are concurrently happening, and at what time.<br>
Here we take a look at the algorithm output on node\[0\] after the snapshot is initiated. <br><br>
![Alt text](images/ex1.png?raw=true "")<br>
In this image we can see in the highlighted area that during the snapshot computation process node\[0\] didn't receive all sent messages from node\[1\]. <br>
The missing amount on node\[0\] is caught, thanks to the transaction history of both nodes, and added to the bitcake sum.<br><br><br>

![Alt text](images/ex2.png?raw=true "")<br>
In this photo we see the bitcake sum result of this region (node\[0\] region).<br>
The final part of combining region results is initiated. <br><br><br>

![Alt text](images/ex3.png?raw=true "")<br>
Multiple rounds of regions exchanging results. <br><br><br>

![Alt text](images/ex4.png?raw=true "")<br>
Finally after all regions exchanged results, in the highlighted area we can see the final bitcake sum result of the entire system.

## Sidenote
This project was an assignment as a part of the course - Concurrent and Distributed Systems during the 8th semester at the Faculty of Computer Science in Belgrade. All system functionalities were defined in the assignment specifications.

## Contributors
- Stefan Ginic - <stefangwars@gmail.com>
