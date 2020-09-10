# Snapshots-on-a-P2P-Distributed-System
Implementation of a <b>snapshot algorithm</b> that retrieves the current total balance (of bitcakes - currency) in a distributed peer to peer system in which a large number of transactions are constantly being made. The snapshot algorithm is a mix between the <b>Lai Yang - Li</b> and the <b>Spezialetti-Kearns algorithms</b>.

## Overview
The peer to peer network is a graph where communication between nodes is <b>asinhronous and non-FIFO</b>.<br>
Two nodes can communicate only if they are connected (adjecent) on the graph. <br>
Each node has a starting bitcake amount. Constant transactions rapidly change this value, which in addition to the asinhronous and non-FIFO communication makes it hard to retrieve the corrent total currency in the system, due to late or unrecieved transaction messages at the time of computation. <br>
This problem is solved by a mix of two algorithms:
1. Lai Yang - Li variation (enables each node to perform multiple snapshots at a time)
2. Specialetti - Kearns (enables multiple nodes to perform a snapshot concurrently by working together)

The system supports <b>scripted</b> launching, for running multiple nodes simultaneously and providing input commands to nodes using text files as input. 

## Technical details
To run the system, a MultipleServentStarter class is provided. This class starts separate Node programs using the <b>ProcessBuilder</b>.<br>
The user specifies a network graph in the config file which the MultipleServentStarter reads and provides Nodes with their specified port and id number via the program arguments.<br>
Also the System.out, System.err and System.in are redirected to files /output/serventID_out.txt, /error/serventID_err.txt and /input/serventID_in.txt, to allow the user to supply all nodes with input commands simultaneously. <br>
The user can also interact with nodes using the CLI (command line interface). <br>
The sending of each message is delayed by a small random amount to simulate a realistic distributed system (because the system is tested locally on one machine).

# README IN DEVELOPMENT
