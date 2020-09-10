# Snapshots-on-a-P2P-Distributed-System
Implementation of a <b>snapshot algorithm</b> that retrieves the current total balance (of bitcakes - currency) in a distributed peer to peer system in which a large number of transactions is constantly being made in every moment. The snapshot algorithm is a mix of the <b>Lai Yang - Li</b> and the <b>Spezialetti-Kearns algorithms</b>.

## Overview
The peer to peer network is a graph where communication between nodes is <b>asinhronous and non-FIFO</b>.<br>
Two nodes can communicate only if they are connected (adjecent) on the graph. <br>
Each node has a starting bitcake amount. Constant transactions rapidly change this value, which in addition to the asinhronous and non-FIFO communication makes it hard to retrieve the corrent total currency in the system, due to late or unrecieved transaction messages at the time of computation. <br>
This problem is solved by a mix of two algorithms:
1. Lai Yang - Li variation (enables each node to perform multiple snapshots at a time)
2. Specialetti - Kearns (enables multiple nodes to perform a snapshot concurrently by working together)

## Technical details


# README IN DEVELOPMENT
