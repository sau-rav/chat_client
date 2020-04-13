import java.util.*;
import java.io.*;
import java.net.*;

public class server{

    static String mode;
    public static void main(String[] args) throws Exception{
        ServerSocket connect = new ServerSocket(6789);
        try{
            if(args[0].equals("1"))
                System.out.println("Unencrypted Mode");
            else if(args[0].equals("2"))
                System.out.println("Encrypted Mode");
            else if(args[0].equals("3"))
                System.out.println("Signature Mode");
            else{
                System.out.println("Error : Not a valid mode");
                System.out.println("Mode 1. Unencrypted Mode\nMode 2. Encrypted Mode\nMode 3. Signature Mode");
                System.out.println("Use one of the above modes");
                System.exit(1);
            }
        }
        catch(Exception e){
            System.out.println("Format not correct. Please use format \"server <mode>\"");
            System.out.println("Mode 1. Unencrypted Mode\nMode 2. Encrypted Mode\nMode 3. Signature Mode");
            System.out.println("Use one of the above modes");
            System.exit(1);
        }

        while(true){
            Socket outSocket = connect.accept(); 
            Socket inSocket = connect.accept();

            DataInputStream inFromClient = new DataInputStream(inSocket.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(outSocket.getOutputStream());

            if(args[0].equals("1")){
                mode = args[0];
                ClientUnencrypted clientManager = new ClientUnencrypted(inSocket,outSocket,inFromClient,outToClient);
                Thread client = new Thread(clientManager);
                client.start();
            }
            else if(args[0].equals("2")){
                mode = args[0];
                ClientEncrypted clientManager = new ClientEncrypted(inSocket,outSocket,inFromClient,outToClient);
                Thread client = new Thread(clientManager);
                client.start();
            }
            else if(args[0].equals("3")){
                mode = args[0];
                ClientSignature clientManager = new ClientSignature(inSocket,outSocket,inFromClient,outToClient);
                Thread client = new Thread(clientManager);
                client.start();
            }
            else{
                System.out.println("Error : Not a valid mode");
                System.out.println("Mode 1. Unencrypted Mode\nMode 2. Encrypted Mode\nMode 3. Signature Mode");
                System.out.println("Use one of the above modes");
                System.exit(1);
            }
        }
    }
}

//----------------------------------------------------  Client Unencrypted  ---------------------------------------------------

class ClientUnencrypted extends Thread{

    Socket inSocket;
    Socket outSocket;
    DataInputStream inFromClient;
    DataOutputStream outToClient;
    static HashMap<String, Socket> outHashMap = new HashMap<>();
    static HashMap<String, Socket> inHashMap = new HashMap<>();
    String username;
    String recipientname;
    boolean sendRegistration;
    boolean recRegistration;

    public ClientUnencrypted(Socket in, Socket out,DataInputStream dis, DataOutputStream dos){
        inSocket = in;
        outSocket = out;
        inFromClient = dis;
        outToClient = dos;
        username = "Anonymous";
        sendRegistration = false;
        recRegistration = false;
    }

    public void run(){
        String message;
        try{
            message = inFromClient.readUTF();
            if(message.substring(5).equals(server.mode)){
                outToClient.writeUTF("OK");
            }
            else{
                outToClient.writeUTF("NOPE "+server.mode);
            }
        }
        catch(Exception e){
            System.out.println("Unknown Error");
        }
        while(true){
            try{
                message = inFromClient.readUTF();
                if(!(sendRegistration && recRegistration)){
                    registrationHandler(message);
                }
                else{
                    messageHandler(message);
                }
            }
            catch(Exception e){
                try{
                    System.out.println("Connection aborted for "+username);
                    inHashMap.remove(username);
                    outHashMap.remove(username);
                    return;
                }
                catch(Exception u){
                    System.out.println(u);
                }
            }
        }
    }  
    
    public void registrationHandler(String message){
        try{
            if(message.contains("REGISTER TOSEND") || message.contains("REGISTER TORECV")){
                int ptr = message.indexOf(" ",9);
                try{
                    username = message.substring(ptr+1);
                }
                catch(Exception e){
                    outToClient.writeUTF("ERROR 100 Malformed Username");
                    System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                }
                if(message.contains("REGISTER TOSEND")){
                    if(outHashMap.get(username) != null){
                        outToClient.writeUTF("ERROR 404 Username Already Exists. Please restart the session");
                        System.out.println("Sent to "+username+" ERROR 404 Username Already Exists. Please restart the session");
                    }
                    else if(username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("REGISTERED TOSEND "+username);
                        System.out.println("Sent to "+username+" REGISTERED TOSEND "+username);
                        sendRegistration = true;
                        if(sendRegistration && recRegistration){
                            outHashMap.put(username, outSocket);
                            inHashMap.put(username, inSocket);
                        }
                    }
                    else if(!username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("ERROR 100 Malformed Username");
                        System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                    }
                    else{
                        outToClient.writeUTF("ERROR 101 No user registered");
                        System.out.println("Sent to "+username+" ERROR 101 No user registered");
                    }
                }
                else{
                    if(outHashMap.get(username) != null){
                        outToClient.writeUTF("ERROR 404 Username Already Exists. Please restart the session");
                        System.out.println("Sent to "+username+" ERROR 404 Username Already Exists. Please restart the session");
                    }
                    else if(username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("REGISTERED TORECV "+username);
                        System.out.println("Sent to "+username+" REGISTERED TORECV "+username);
                        recRegistration = true;
                        if(sendRegistration && recRegistration){
                            outHashMap.put(username, outSocket);
                            inHashMap.put(username, inSocket);
                        }
                    }
                    else if(!username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("ERROR 100 Malformed Username");
                        System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                    }
                    else{
                        outToClient.writeUTF("ERROR 101 No user registered");
                        System.out.println("Sent to "+username+" ERROR 101 No user registered");
                    }
                }
            }
            else{
                outToClient.writeUTF("ERROR 101 No user registered");
                System.out.println("Sent to "+username+" ERROR 101 No user registered");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void messageHandler(String message){
        try{
            if(message.contains("SEND")){
                recipientname = message.substring(5);
                String contentLen = inFromClient.readUTF();
                String blankLine = inFromClient.readUTF();
                contentLen = contentLen.substring(contentLen.indexOf(":")+2);
                String content = inFromClient.readUTF();

                if(contentLen == null){
                    outToClient.writeUTF("ERROR 103 Header incomplete");
                    System.out.println("Sent to "+username+" ERROR 103 Header incomplete. You have been unregistered. Please Start a New Connection");
                    outHashMap.remove(username);
                    inHashMap.remove(username);
                }
                else{
                    Socket tempOut = outHashMap.get(recipientname);
                    if(tempOut == null){
                        outToClient.writeUTF("ERROR 102 Unable to send");
                        System.out.println("Sent to "+username+" ERROR 102 Unable to send");
                        return;
                    }
                    else{
                        tryToSend(content);
                    }
                }
            }
            else if(message.contains("RECEIVED")){
                recipientname = message.substring(9);
                Socket tempOut = outHashMap.get(recipientname);
                DataOutputStream dout = new DataOutputStream(tempOut.getOutputStream());
                dout.writeUTF("SENT "+username);
                System.out.println("Sent to "+recipientname+" SENT "+username);
            } 
            else if(message.equals("UNREGISTER") || message.equals("unregister")){
                outToClient.writeUTF("You have been unregistered");
                outHashMap.remove(username);
                inHashMap.remove(username);
                System.out.println("User "+username+" has been unregistered");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void tryToSend(String message){
        try{
            Socket tempOut = outHashMap.get(recipientname);
            DataOutputStream dout = new DataOutputStream(tempOut.getOutputStream());

            dout.writeUTF("FORWARD "+username);
            dout.writeUTF("Content-length: "+message.length());
            dout.writeUTF("");
            dout.writeUTF(message);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}

//----------------------------------------------------  Client Encrypted  ---------------------------------------------------

class ClientEncrypted extends Thread{

    Socket inSocket;
    Socket outSocket;
    DataInputStream inFromClient;
    DataOutputStream outToClient;
    static HashMap<String, Socket> outHashMap = new HashMap<>();
    static HashMap<String, Socket> inHashMap = new HashMap<>();
    static HashMap<String, String> publicKeyList = new HashMap<>();
    String username;
    String recipientname;
    boolean sendRegistration;
    boolean recRegistration;

    public ClientEncrypted(Socket in, Socket out,DataInputStream dis, DataOutputStream dos){
        inSocket = in;
        outSocket = out;
        inFromClient = dis;
        outToClient = dos;
        username = "Anonymous";
        sendRegistration = false;
        recRegistration = false;
    }

    public void run(){
        String message;
        try{
            message = inFromClient.readUTF();
            if(message.substring(5).equals(server.mode)){
                outToClient.writeUTF("OK");
            }
            else{
                outToClient.writeUTF("NOPE "+server.mode);
            }
        }
        catch(Exception e){
            System.out.println("Unknown Error");
        }
        while(true){
            try{
                message = inFromClient.readUTF();
                if(!(sendRegistration && recRegistration)){
                    registrationHandler(message);
                }
                else{
                    messageHandler(message);
                }
            }
            catch(Exception e){
                try{
                    System.out.println("Connection aborted for "+username);
                    inHashMap.remove(username);
                    outHashMap.remove(username);
                    return;
                }
                catch(Exception u){
                    System.out.println(u);
                }
            }
        }
    }  
    
    public void registrationHandler(String message){
        try{
            if(message.contains("REGISTER TOSEND") || message.contains("REGISTER TORECV")){
                int ptr = message.indexOf(" ",9);
                try{
                    username = message.substring(ptr+1);
                }
                catch(Exception e){
                    outToClient.writeUTF("ERROR 100 Malformed Username");
                    System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                }
                if(message.contains("REGISTER TOSEND")){
                    if(outHashMap.get(username) != null){
                        outToClient.writeUTF("ERROR 404 Username Already Exists. Please restart the session");
                        System.out.println("Sent to "+username+" ERROR 404 Username Already Exists. Please restart the session");
                    }
                    else if(username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("REGISTERED TOSEND "+username);
                        System.out.println("Sent to "+username+" REGISTERED TOSEND "+username);
                        sendRegistration = true;
                        if(sendRegistration && recRegistration){
                            outHashMap.put(username, outSocket);
                            inHashMap.put(username, inSocket);
                        }
                    }
                    else if(!username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("ERROR 100 Malformed Username");
                        System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                    }
                    else{
                        outToClient.writeUTF("ERROR 101 No user registered");
                        System.out.println("Sent to "+username+" ERROR 101 No user registered");
                    }
                }
                else{
                    if(outHashMap.get(username) != null){
                        outToClient.writeUTF("ERROR 404 Username Already Exists. Please restart the session");
                        System.out.println("Sent to "+username+" ERROR 404 Username Already Exists. Please restart the session");
                    }
                    else if(username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("REGISTERED TORECV "+username);
                        System.out.println("Sent to "+username+" REGISTERED TORECV "+username);
                        recRegistration = true;
                        if(sendRegistration && recRegistration){
                            outHashMap.put(username, outSocket);
                            inHashMap.put(username, inSocket);
                        }
                    }
                    else if(!username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("ERROR 100 Malformed Username");
                        System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                    }
                    else{
                        outToClient.writeUTF("ERROR 101 No user registered");
                        System.out.println("Sent to "+username+" ERROR 101 No user registered");
                    }
                }
            }
            else{
                outToClient.writeUTF("ERROR 101 No user registered");
                System.out.println("Sent to "+username+" ERROR 101 No user registered");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void messageHandler(String message){
        try{
            if(message.contains("SEND")){
                recipientname = message.substring(5);
                String contentLen = inFromClient.readUTF();
                String blankLine = inFromClient.readUTF();
                contentLen = contentLen.substring(contentLen.indexOf(":")+2);
                String content = inFromClient.readUTF();

                if(contentLen == null){
                    outToClient.writeUTF("ERROR 103 Header incomplete");
                    System.out.println("Sent to "+username+" ERROR 103 Header incomplete. You have been unregistered. Please Start a New Connection");
                    outHashMap.remove(username);
                    inHashMap.remove(username);
                }
                else{
                    Socket tempOut = outHashMap.get(recipientname);
                    if(tempOut == null){
                        outToClient.writeUTF("ERROR 102 Unable to send");
                        System.out.println("Sent to "+username+" ERROR 102 Unable to send");
                        return;
                    }
                    else{
                        tryToSend(content);
                    }
                }
            }
            else if(message.contains("RECEIVED")){
                recipientname = message.substring(9);
                Socket tempOut = outHashMap.get(recipientname);
                DataOutputStream dout = new DataOutputStream(tempOut.getOutputStream());
                dout.writeUTF("SENT "+username);
                System.out.println("Sent to "+recipientname+" SENT "+username);
            } 
            else if(message.equals("UNREGISTER") || message.equals("unregister")){
                outToClient.writeUTF("You have been unregistered");
                outHashMap.remove(username);
                inHashMap.remove(username);
                System.out.println("User "+username+" has been unregistered");
            }
            else if(message.contains("KEY")){
                try{
                    String key = message.substring(4);
                    publicKeyList.put(username, key);
                    outToClient.writeUTF("OK");
                    System.out.println("Saved Key of "+username);
                }
                catch(Exception e){
                    outToClient.writeUTF("NOPE");
                }
            }
            else if(message.contains("FETCH")){
                try{
                    String tempUser = message.substring(6);
                    String reply = publicKeyList.get(tempUser);
                    if(reply == null){
                        outToClient.writeUTF("KEY NOT FOUND");
                    }
                    else 
                        outToClient.writeUTF("KEY "+reply);
                }
                catch(Exception e){
                    System.out.println("Key return error");
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void tryToSend(String message){
        try{
            Socket tempOut = outHashMap.get(recipientname);
            DataOutputStream dout = new DataOutputStream(tempOut.getOutputStream());

            dout.writeUTF("FORWARD "+username);
            dout.writeUTF("Content-length: "+message.length());
            dout.writeUTF("");
            dout.writeUTF(message);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}

//----------------------------------------------------  Client Signature  ---------------------------------------------------

class ClientSignature extends Thread{

    Socket inSocket;
    Socket outSocket;
    DataInputStream inFromClient;
    DataOutputStream outToClient;
    static HashMap<String, Socket> outHashMap = new HashMap<>();
    static HashMap<String, Socket> inHashMap = new HashMap<>();
    static HashMap<String, String> publicKeyList = new HashMap<>();
    String username;
    String recipientname;
    boolean sendRegistration;
    boolean recRegistration;

    public ClientSignature(Socket in, Socket out,DataInputStream dis, DataOutputStream dos){
        inSocket = in;
        outSocket = out;
        inFromClient = dis;
        outToClient = dos;
        username = "Anonymous";
        sendRegistration = false;
        recRegistration = false;
    }

    public void run(){
        String message;
        try{
            message = inFromClient.readUTF();
            if(message.substring(5).equals(server.mode)){
                outToClient.writeUTF("OK");
            }
            else{
                outToClient.writeUTF("NOPE "+server.mode);
            }
        }
        catch(Exception e){
            System.out.println("Unknown Error");
        }
        while(true){
            try{
                message = inFromClient.readUTF();
                if(!(sendRegistration && recRegistration)){
                    registrationHandler(message);
                }
                else{
                    messageHandler(message);
                }
            }
            catch(Exception e){
                try{
                    System.out.println("Connection aborted for "+username);
                    inHashMap.remove(username);
                    outHashMap.remove(username);
                    return;
                }
                catch(Exception u){
                    System.out.println(u);
                }
            }
        }
    }  
    
    public void registrationHandler(String message){
        try{
            if(message.contains("REGISTER TOSEND") || message.contains("REGISTER TORECV")){
                int ptr = message.indexOf(" ",9);
                try{
                    username = message.substring(ptr+1);
                }
                catch(Exception e){
                    outToClient.writeUTF("ERROR 100 Malformed Username");
                    System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                }
                if(message.contains("REGISTER TOSEND")){
                    if(outHashMap.get(username) != null){
                        outToClient.writeUTF("ERROR 404 Username Already Exists. Please restart the session");
                        System.out.println("Sent to "+username+" ERROR 404 Username Already Exists. Please restart the session");
                    }
                    else if(username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("REGISTERED TOSEND "+username);
                        System.out.println("Sent to "+username+" REGISTERED TOSEND "+username);
                        sendRegistration = true;
                        if(sendRegistration && recRegistration){
                            outHashMap.put(username, outSocket);
                            inHashMap.put(username, inSocket);
                        }
                    }
                    else if(!username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("ERROR 100 Malformed Username");
                        System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                    }
                    else{
                        outToClient.writeUTF("ERROR 101 No user registered");
                        System.out.println("Sent to "+username+" ERROR 101 No user registered");
                    }
                }
                else{
                    if(outHashMap.get(username) != null){
                        outToClient.writeUTF("ERROR 404 Username Already Exists. Please restart the session");
                        System.out.println("Sent to "+username+" ERROR 404 Username Already Exists. Please restart the session");
                    }
                    else if(username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("REGISTERED TORECV "+username);
                        System.out.println("Sent to "+username+" REGISTERED TORECV "+username);
                        recRegistration = true;
                        if(sendRegistration && recRegistration){
                            outHashMap.put(username, outSocket);
                            inHashMap.put(username, inSocket);
                        }
                    }
                    else if(!username.matches("[a-zA-Z0-9]*")){
                        outToClient.writeUTF("ERROR 100 Malformed Username");
                        System.out.println("Sent to "+username+" ERROR 100 Malformed Username");
                    }
                    else{
                        outToClient.writeUTF("ERROR 101 No user registered");
                        System.out.println("Sent to "+username+" ERROR 101 No user registered");
                    }
                }
            }
            else{
                outToClient.writeUTF("ERROR 101 No user registered");
                System.out.println("Sent to "+username+" ERROR 101 No user registered");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void messageHandler(String message){
        try{
            if(message.contains("SEND")){
                recipientname = message.substring(5);
                String contentLen = inFromClient.readUTF();
                contentLen = contentLen.substring(contentLen.indexOf(":")+2);
                String blankLine = inFromClient.readUTF();
                String content = inFromClient.readUTF();
                String signature = inFromClient.readUTF();

                if(contentLen == null){
                    outToClient.writeUTF("ERROR 103 Header incomplete");
                    System.out.println("Sent to "+username+" ERROR 103 Header incomplete. You have been unregistered. Please Start a New Connection");
                    outHashMap.remove(username);
                    inHashMap.remove(username);
                }
                else{
                    Socket tempOut = outHashMap.get(recipientname);
                    if(tempOut == null){
                        outToClient.writeUTF("ERROR 102 Unable to send");
                        System.out.println("Sent to "+username+" ERROR 102 Unable to send");
                        return;
                    }
                    else{
                        tryToSend(content,signature);
                    }
                }
            }
            else if(message.contains("RECEIVED")){
                recipientname = message.substring(9);
                Socket tempOut = outHashMap.get(recipientname);
                DataOutputStream dout = new DataOutputStream(tempOut.getOutputStream());
                dout.writeUTF("SENT "+username);
                System.out.println("Sent to "+recipientname+" SENT "+username);
            } 
            else if(message.equals("UNREGISTER") || message.equals("unregister")){
                outToClient.writeUTF("You have been unregistered");
                outHashMap.remove(username);
                inHashMap.remove(username);
                System.out.println("User "+username+" has been unregistered");
            }
            else if(message.contains("KEY")){
                try{
                    String key = message.substring(4);
                    publicKeyList.put(username, key);
                    outToClient.writeUTF("OK");
                    System.out.println("Saved Key of "+username);
                }
                catch(Exception e){
                    outToClient.writeUTF("NOPE");
                }
            }
            else if(message.contains("FETCH")){
                try{
                    String tempUser = message.substring(6);
                    String reply = publicKeyList.get(tempUser);
                    if(reply == null){
                        outToClient.writeUTF("KEY NOT FOUND");
                    }
                    else {
                        outToClient.writeUTF("KEY "+reply);
                    }
                }
                catch(Exception e){
                    System.out.println("Key return error");
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void tryToSend(String message, String signature){
        try{
            Socket tempOut = outHashMap.get(recipientname);
            DataOutputStream dout = new DataOutputStream(tempOut.getOutputStream());
	    
            dout.writeUTF("FORWARD "+username);
            dout.writeUTF("Content-length: "+message.length());
            dout.writeUTF("");
            dout.writeUTF(message);
            dout.writeUTF(signature);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
