**Symptom**

Software stops function correctly because of a network problem.

**Reason and Prerequisites**

Network communication problems are one of the challenges any company faces in order to keep their system functional. To detect and find this interuption is very challenging. This SAP note, provides a Java utility, NetworkCheck, that checks the behaviour of a network of servers. Here  are the benefit can be achieved from this Utility:

1. If your software requires to communicate with more than one node. ( i.e VM nodes, or physical servers )
2. If from time to time communication between your VM nodes or physical severs got interupted 
 

**Requirement**

Java SDK 1.7 to compile the NetworkCheck.java 
Text Editor 
 

**Solution**

1. Unzip the attached source code into a folder ( i.e c:\java ) 
2. Open the Java source code in a text editor
3. Go to line number: 8 and change the directory where the log file should be writen. By default it is "c:/SAP"
4. Go to line numer 10 and edit the list of the hosts that you want the utility to monitor and add to the list if you have
5. more than two nodes to monitor. Replace the following with your host name, <HOST-NAME-TO-BE-REPLACED>  
6. By default, the utility listens on port 2203, you can modify that by going to line 16 and changing the value of CHECKER_PORT 
7. From the command prompt after you are satisfy with the changes to the utility, create a folder called "c:\java\classes"
8. Copy the NetworkCheck.java to "c:\java" folder
9. Now compile the source code as follows: javac -d c:\java\classes NetworkCheck.java 
10. Run the utility by executing this command from within the c:\java directory as follow: java -classpath c:\java\classes misc.NetworkCheck

**Note:** If you need to run the java program as a service instead of keeping Windows command prompt open for weeks at a time, then you could use JSL ( Java Service Launcher ) which can be accessed from
the following URL: [http://sourceforge.net/projects/jslwin/](http://sourceforge.net/projects/jslwin/)
  
Each node will generate its own log file. By default c:\SAP\NetworkCheck-nodename.log. If the output directory in the source code change, then that would be the location where the log is going to reside 
Note:

When you  start running the program, you cannot do it exactly simultaneously on each node, so some nodes will be temporarily unable to connect to other(s), e.g.
 2014-11-13T10:37:30.332 Initial connection to server <HOST-NAME>: java.net.ConnectException: Connection timed out: connect 
After all nodes have established connections to one another, they should not log exceptions any more, and they will do so differently, as in
<timestamp> Exception while <some activity>
   Java stack trace 

Example:

2014-11-13T10:51:27.743 Exception while reading from client /xx.xx.xx.xx:xxxxx: java.net.SocketTimeoutException: Read timed out 
        at java.net.SocketInputStream.socketRead0(Native Method)

        at java.net.SocketInputStream.read(SocketInputStream.java:150)

        at java.net.SocketInputStream.read(SocketInputStream.java:121)

        at java.net.SocketInputStream.read(SocketInputStream.java:203)

 

**Next Step**

If any such exceptions occur (that is any with text "Exception while" and/or a stack trace), that will give you the proof that you do have a network communication problems and you need to fixed this communication with your network administrator.  
This NetworkCheck should be left running on all system until the issue has been resolved 
 
