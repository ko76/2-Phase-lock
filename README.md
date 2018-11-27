#p4 2 phase lock concurrency control with deadlock detection

Anthony Ko
CS448, P4

Implementation of the 2 phase lock (2PL) method of dealing with concurrent transactions. Processes each action in each transaction in a round robin fashion.

Handling deadlocks: Creates a wait-for-graph using an adjacency list to construct a graph with directed nodes. Detecting a cycle means there is a deadlock. Handles the deadlock by aborting a transaction, rolling back and unwriting the transactions write commands.


Log file:

Write commands - W:Timestamp,TransactionID,RecordID,oldvalue,newvalue,timestamp of previous log entry of this transaction

Read commands - R:Timestamp,TransactionID,RecordID,value read, timestamp of previous log entry of this transaction

Commit commands - C:Timestamp,TransactionID,timestamp of previous log entry of this transaction

Abort commands - A:Timestamp,TransactionID, timestamp of previous log entry of this transaction



log file name = log.txt

to compile, run
        make

to start program, run
        java Project4

log file will only record the log of current concurrent transactions.
