# chat_client
Java based multiuser chat program (terminal based) with normal and encrypted mode for chat<br>

Details for running :<br>
compile everything using : bash compile.sh<br>

To run the server : java server <em>mode</em> <br>
The program runs in 3 different modes - <br>
1. Normal Mode (Non encrypted mode)<br>
2. Encrypted Mode (Uses SHA-256 for encryption)<br>
3. Signature + Encryption (Used SHA-256 and verifies signature of user from whom the message was received<br>

To run the client : java client <em>username</em> <em>mode</em> <em>IP</em><br>
The client must connect to the server in the mode in which server is running<br>
The client and server must be connected on same network (same wifi/hotspot)<br>

  
