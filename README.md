# README

# Distributed Systems Assignment 3

## Design / Workflow
The program functions on a peer to peer system. Each peer connects and interacts with the other peers via a loop which connects to different ports that the peers are located on. If a peer is not found, the program will continue until it has attempted to contact all peers.

Peers can be initiated manually or via the testing scripts (see Automated Testing). Each member then has the option to start a Paxos Run. The Paxos Run occurs as follows:

### Prepare Stage

- A Member will listen for a command, if the command is **prepare** they will send a prepare message to all other members.
- Each other member will have a server thread upon creation, which listens for messages from other peers and starts additional threads to handle the request
- When another member receives a connection they will evaluated the message for its type. In this case prepare. Upon recieving the prepare message the Member will check the ID of the sender to see if it is the maximum ID recieved

	-  If yes: The member will send a **prepare-ok** or **preAccept** (see Proposal Stage) response back to the sender
	- If no: The member will send a **fail** response back to the sender

- There are now three outcomes:

	- The Member will receive a majority of successful response and will then move on to the Proposal Stage
	- The Member will be unable to connect to a majority and will simply terminate the run
	- The Member will receive a majority fails. If this occurs the Member will increase its ID to a higher value (its original id + the total members) and repeat the prepare request until it receives a majority success.

### Proposal Stage

- If a Member has received a successful majority promises from others it will propose. If all of the successful responses are "prepare-ok" then it will propose its own ID as the proposal value. If any of the responses are "preAccept" then the Member knows that consensus has already been reached and a value was already previously proposed in which case it will use the highest value it received from preAccept
- Other members will still be listening for any messages. If a message contains "propose" they will:

	-	Evaluate the message ID. If the ID matches the ID they promised to they will send an **Accepted** to the proposer.
	-	If the ID does not match they will send a **fail**
- Upon receiving a majority of accepts the proposing member will now know who the council leader is (either themselves or someone who already got the role) and will either create or update a local file with the new leader.

### Regarding the local file
- In this case since all peers are under the same local host in the same directory they will all draw from the same local file. But in the theoretical scenario where the system would be distributed, the Members would **NEED TO PERFORM A PAXOS RUN** in order to update or create a local file to draw from. In the case of this assignment, members do not need to perform Paxos to learn previous consensus however.

### End Term Stage
- To end a particular run, the endTerm command can be initiated by the user to inform all others that the current council leader would like to end their term.
- The endTerm command will only work if it fed to the current leader
- When the endTerm command is parsed, the leader will inform all other members that it no longer desired to lead the council. This will reset all relevant values and make it so members no longer remember who they promised to or any value they accepted. 


## Compiling
Compiling the code works as follows:

- Compile the Request class using `javac -d . Request.java`
- Compile the Member class using `javac Member.java`

## Manual Operation and Testing

In order to successfully run this system manually, you will need to initiate separate terminals and run each Member separately. 
To get a Member running there are several options. The most basic command is `java Member n` where n is the desired Member number. By default any member initiated in this way will have an immediate response time. To specify a different response time simply provide another argument (e.g. if you want a late responding member use `java Member n late`). The available response times are immediate, medium, late & never. Members M2 and M3 response times are determined by chance so whichever response time you input will not effect these two members in particular. All other members will keep the inputed response time.

If you would like a member to instantly prepare without requiring a manual input you can use `java Member n medium prepare`. This is not necessary however as simply inputing `prepare`into the console after the Member is already running will also work. This is mainly used for automated testing.

By default you will have to initiate 9 for the code to work since 9 will be the initial value for the `TOTAL_MEMBERS`in the code. The code will work with any number of members so long as this `TOTAL_MEMBERS` value is changed. Additionally, by default the starting port will be 2000 (so M1 will run on port 2001, M2 on port 2002 etc.). To change the desired set of ports, change the `STARTING_PORT` value. Changing either of these values will require recompiling. **IMPORTANT: Changing the number of members will cause the automated testing to fail unless automated testing scripts are also edited to accommodate this change**.

## Automated Testing
There are several bash scripts that will automated the creation of 9 separate Members and run through Paxos automatically. Results of these scripts will be placed into matching output files. These output files contain the output of the **proposer only**. The outputs of all other Members will be displayed on the console.

To run these tests change to testing directory and run the bash files from there.

**Please note:** these tests utilize GNU parallel to perform concurrent tasks meaning it will need installation on your machine if it doesn't already have it. GNU parrallel takes advantage of multiple CPU cores to perform concurrent tasks meaning that if a given CPU has less than 9 Logical Processors the default tests may fail. 

To change the automated tests, follow the comments within the tests themselves.
