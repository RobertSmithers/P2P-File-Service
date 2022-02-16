# P2P-File-Service
Building the frameworks behind the infamous Napster service in a weekend!

<div style="text-align: center;">
    <img 
        style="display: block; 
            margin-left: auto;
            margin-right: auto;
            width: 60%;"
        src="images/p2pImage.jpeg" 
        alt="Peer-To-Peer Image">
    </img>
</div>


In this demo, Client 1 and Client 2 are running on separate networks. To demonstrate this on our device, we assign them distinct TCP Ports (simulating a separate internet address internet) and separate the connections with isolated threads.

### How to run
Compile and run `Server.java` from the Distributed File Transfer Service.

Compile and run  `Client.java` from Client1 and client.java from Client2

Once client is running on client1 and client2, files can be registered to the server via the command line. When client1 or client2 request files from the server, it will respond with the proper peer-client capable to distributing the requested file. In essence, this is the functionality of the Peer-to-Peer network.

With this information, the client service is able to initiate a separate TCP handshake to our client address and port, requesting the filename and receiving the desired information.

This concludes creating the basic Napster service. What a fun project!!
