// client <username> <mode> <IP>
// eg. client saurav 1 localhost
import java.io.*;
import java.net.*;
import java.security.KeyPair;

public class client{
    
    static byte[] publicKey;
    static byte[] privateKey;
    static byte[] recipientPublicKey;
    static String username;
    static String recipientname;
    static boolean flag;
    static String[] Mode = {"Unencrypted","Encrypted","Encrypted With Signature"};
    public static void main(String[] args){
        client client1 = new client();
        try{
            username = args[0];
            System.out.println("-------------------------------------------------");
            System.out.println("-->  Your username is " +username);
            client1.start(args[1],args[2]);
        }
        catch(Exception e){
            System.out.println("Format not correct. Please use format \"client <username> <mode> <IP>\"");
            System.out.println("Mode 1. Unencrypted Mode\nMode 2. Encrypted Mode\nMode 3. Signature Mode");
            System.exit(1);
        }
    }

    public void start(String mode, String ip){
        try{
            Socket inConnect = new Socket(ip, 6789);
            Socket outConnect = new Socket(ip, 6789);

            System.out.println("-->  Connecting.. to "+ip+" on port 6789");
            System.out.println("-------------------------------------------------");

            DataInputStream inFromServer = new DataInputStream(inConnect.getInputStream());
            DataOutputStream outToServer = new DataOutputStream(outConnect.getOutputStream());

            String reply;

            outToServer.writeUTF("MODE "+mode);
            reply = inFromServer.readUTF();
            if(!reply.equals("OK")){
                System.out.println("Mode of server and user dont match. Try connecting in mode "+reply.substring(5)+" "+Mode[Integer.parseInt(reply.substring(5))-1]);
                System.exit(1);
            }

            outToServer.writeUTF("REGISTER TOSEND "+username);
            reply = inFromServer.readUTF();

            if(reply.contains("REGISTERED")){
                System.out.println("Server : "+reply);
            }
            else {
                System.out.println("Server : "+reply);
                System.exit(1);
            }
            
            outToServer.writeUTF("REGISTER TORECV "+username);
            reply = inFromServer.readUTF();
            if(reply.contains("REGISTERED")){
                System.out.println("Server : "+reply);
            }
            else {
                System.out.println("Server : "+reply);
                System.exit(1);
            }
        
            if(mode.equals("1")){
                System.out.println("Unencrypted Mode");
                System.out.println("-------------------------------------------------");
                UnencryptedInputHandlerThread inputHandler = new UnencryptedInputHandlerThread(inFromServer, outToServer);
                UnencryptedOutputHandlerThread outputHandler = new UnencryptedOutputHandlerThread(outToServer, inFromServer);
                
                Thread inputThread = new Thread(inputHandler);
                Thread outputThread = new Thread(outputHandler);
                outputThread.start();
                inputThread.start();
            }
            else if(mode.equals("2")){
                System.out.println("Encrypted Mode");
                System.out.println("-------------------------------------------------");

                KeyPair generateKeyPair = Cryptography.generateKeyPair();
                client.publicKey = generateKeyPair.getPublic().getEncoded();
                client.privateKey = generateKeyPair.getPrivate().getEncoded();

                outToServer.writeUTF("KEY "+java.util.Base64.getEncoder().encodeToString(client.publicKey));
                reply = inFromServer.readUTF();
                if(!reply.equals("OK")){
                    System.out.println("Error in saving key");
                    System.exit(1);
                }

                EncryptedInputHandlerThread inputHandler = new EncryptedInputHandlerThread(inFromServer, outToServer);
                EncryptedOutputHandlerThread outputHandler = new EncryptedOutputHandlerThread(outToServer, inFromServer);
                
                Thread inputThread = new Thread(inputHandler);
                Thread outputThread = new Thread(outputHandler);
                outputThread.start();
                inputThread.start();
            }
            else if(mode.equals("3")){
                System.out.println("Signature Mode");
                System.out.println("-------------------------------------------------");

                KeyPair generateKeyPair = Cryptography.generateKeyPair();
                client.publicKey = generateKeyPair.getPublic().getEncoded();
                client.privateKey = generateKeyPair.getPrivate().getEncoded();
                //System.out.println("key of "+username+" is "+java.util.Base64.getEncoder().encodeToString(client.publicKey));

                outToServer.writeUTF("KEY "+java.util.Base64.getEncoder().encodeToString(client.publicKey));
                reply = inFromServer.readUTF();
                if(!reply.equals("OK")){
                    System.out.println("Error in saving key");
                    System.exit(1);
                }

                SignatureInputHandlerThread inputHandler = new SignatureInputHandlerThread(inFromServer, outToServer);
                SignatureOutputHandlerThread outputHandler = new SignatureOutputHandlerThread(outToServer, inFromServer);
                
                Thread inputThread = new Thread(inputHandler);
                Thread outputThread = new Thread(outputHandler);
                outputThread.start();
                inputThread.start();
            }
            else{
                System.out.println("Not a valid mode");
                System.out.println("Mode 1. Unencrypted Mode\nMode 2. Encrypted Mode\nMode 3. Signature Mode");
                System.out.println("Use one of the above modes");
                System.exit(1);
            }
        }   
        catch(Exception e){
            System.out.println(e.getStackTrace());
        }
    }
}

//-------------------------------------------------------- Unencrypted Chat -----------------------------------------------------------

class UnencryptedInputHandlerThread extends Thread{
    
    DataInputStream din;
    DataOutputStream dout;  
    String message;

    public UnencryptedInputHandlerThread(DataInputStream din, DataOutputStream dout){
        this.din = din;
        this.dout = dout;
    }   

    public void run(){
        try{
            while(true){
                message = din.readUTF();
                if(message.contains("FORWARD")){
                    forwardHandler(message);
                }
                else if(message.contains("You have been unregistered")){
                    System.out.println("Server : "+message);
                    System.exit(1);
                }
                else{
                    System.out.println("Server : "+message);
                }
            }
        }
        catch(Exception e){
            System.out.println("Server down");
            System.exit(1);
        }
    }

    public void forwardHandler(String message){
        try{
            String contentLen = din.readUTF();
            String blankLine = din.readUTF();
            String content = din.readUTF();
            contentLen = contentLen.substring(contentLen.indexOf(":")+2);
            if(contentLen == null){
                dout.writeUTF("ERROR 103 Header Incomplete");
            }
            else{
                dout.writeUTF("RECEIVED "+message.substring(8));
                String name = message.substring(8);
                System.out.println(name+" : "+content);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}

class UnencryptedOutputHandlerThread extends Thread{
    
    DataOutputStream dout;
    DataInputStream din;
    String message;
    BufferedReader br;
    
    public UnencryptedOutputHandlerThread(DataOutputStream dout, DataInputStream din){
        this.dout = dout;
        this.din = din;
        br = new BufferedReader(new InputStreamReader(System.in));
    }
    
    public void run(){
        try{
            while(true){
                message = br.readLine();
                if(message.contains("@") && !message.substring(message.indexOf(" ")+1).equals("")){
                    messageSendHandler(message);
                }
                else if(message.equals("UNREGISTER") || message.equals("unregister")){
                    dout.writeUTF(message);
                }
                else{
                    System.out.println("Invalid message type");
                }
            }
        }
        catch(Exception e){
            System.out.println(e.getStackTrace());
        }
    }

    void messageSendHandler(String message){
        try{
            int idx = message.indexOf(" ");
            client.recipientname = message.substring(1,idx);
            message = message.substring(idx+1);

            dout.writeUTF("SEND "+client.recipientname);
            	dout.writeUTF("Content-length: "+message.length());
            dout.writeUTF("");
            dout.writeUTF(message);
        }
        catch(Exception e){
            System.out.println("Invalid Message Type");
        }
    }
}

//-------------------------------------------------------- Encrypted Chat -------------------------------------------------------------------------

class EncryptedInputHandlerThread extends Thread{
    
    DataInputStream din;
    DataOutputStream dout;  
    String message;

    public EncryptedInputHandlerThread(DataInputStream din, DataOutputStream dout){
        this.din = din;
        this.dout = dout;
    }   

    public void run(){
        try{
            while(true){
                message = din.readUTF();
                if(message.contains("FORWARD")){
                    forwardHandler(message);
                }
                else if(message.contains("You have been unregistered")){
                    System.out.println("Server : "+message);
                    System.exit(1);
                }
                else if(message.contains("KEY")){
                    if(!message.contains("KEY NOT FOUND")){
                        message = message.substring(4);
                        client.recipientPublicKey = java.util.Base64.getDecoder().decode(message);
                    }
                }
                else{
                    System.out.println("Server : "+message);
                }
            }
        }
        catch(Exception e){
            System.out.println("Server down");
            System.exit(1);
        }
    }

    public void forwardHandler(String message){
        try{
            String contentLen = din.readUTF();
            String blankLine = din.readUTF();
            String content = din.readUTF();
            contentLen = contentLen.substring(contentLen.indexOf(":")+2);
            if(contentLen == null){
                dout.writeUTF("ERROR 103 Header Incomplete");
            }
            else{
                dout.writeUTF("RECEIVED "+message.substring(8));
                String name = message.substring(8);
                byte[] decryptedData = Cryptography.decrypt(client.privateKey, java.util.Base64.getDecoder().decode(content));
                String finalMessage = new String(decryptedData);
                //System.out.println(name+" : "+content);  //*    rain check       */
                System.out.println(name+" : "+finalMessage);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}

class EncryptedOutputHandlerThread extends Thread{
    
    DataOutputStream dout;
    DataInputStream din;
    String message;
    BufferedReader br;
    
    public EncryptedOutputHandlerThread(DataOutputStream dout, DataInputStream din){
        this.dout = dout;
        this.din = din;
        br = new BufferedReader(new InputStreamReader(System.in));
    }
    
    public void run(){
        try{
            while(true){
                message = br.readLine();
                if(message.contains("@") && !message.substring(message.indexOf(" ")+1).equals("")){
                    messageSendHandler(message);
                }
                else if(message.equals("UNREGISTER") || message.equals("unregister")){
                    dout.writeUTF(message);
                }
                else{
                    System.out.println("Invalid message type");
                }
            }
        }
        catch(Exception e){
            System.out.println(e.getStackTrace());
        }
    }

    void messageSendHandler(String message){
        try{
            int idx = message.indexOf(" ");
            client.recipientname = message.substring(1,idx);
            message = message.substring(idx+1);
            dout.writeUTF("FETCH "+client.recipientname);
            Thread.sleep(50);
            byte[] encryptedData = Cryptography.encrypt(client.recipientPublicKey,message.getBytes());
            String encryptedMessage = java.util.Base64.getEncoder().encodeToString(encryptedData);
            dout.writeUTF("SEND "+client.recipientname);
            dout.writeUTF("Content-length: "+encryptedMessage.length());
            dout.writeUTF("");
            dout.writeUTF(encryptedMessage);
        }
        catch(Exception e){
            try{
                dout.writeUTF("SEND "+client.recipientname);
                dout.writeUTF("Content-length: "+message.length());
                dout.writeUTF("");
                dout.writeUTF(message);
            }
            catch(Exception f){
                System.out.println(f);
            }
        }
    }
}

//----------------------------------------------------- Signature Chat ---------------------------------------------------------------------

class SignatureInputHandlerThread extends Thread{
    
    DataInputStream din;
    DataOutputStream dout;  
    String message;

    public SignatureInputHandlerThread(DataInputStream din, DataOutputStream dout){
        this.din = din;
        this.dout = dout;
    }   

    public void run(){
        try{
            while(true){
                message = din.readUTF();
                if(message.contains("FORWARD")){
                    forwardHandler(message);
                }
                else if(message.contains("You have been unregistered")){
                    System.out.println("Server : "+message);
                    System.exit(1);
                }
                else if(message.contains("KEY")){
                    if(!message.contains("KEY NOT FOUND")){
                        message = message.substring(4);
                        client.recipientPublicKey = java.util.Base64.getDecoder().decode(message);
                        client.flag = true;
                    }
                }
                else{
                    System.out.println("Server : "+message);
                }
            }
        }
        catch(Exception e){
            System.out.println("Server down");
            System.exit(1);
        }
    }

    public void forwardHandler(String message){
        try{
            String contentLen = din.readUTF();
            String blankLIne = din.readUTF();
            String content = din.readUTF();
            String signature = din.readUTF();
            contentLen = contentLen.substring(contentLen.indexOf(":")+2);
            if(contentLen == null){
                dout.writeUTF("ERROR 103 Header Incomplete");
            }
            else{
                try{
                    dout.writeUTF("RECEIVED "+message.substring(8));
                    String name = message.substring(8);
                    byte[] messageContentEncrypted = java.util.Base64.getDecoder().decode(content);
                    byte[] decryptedData = Cryptography.decrypt(client.privateKey, messageContentEncrypted);
                    byte[] orignalMessageHash = Cryptography.getHash(messageContentEncrypted);
                    String finalMessage = new String(decryptedData);
                    byte[] signatureByte = java.util.Base64.getDecoder().decode(signature);
                    
                    dout.writeUTF("FETCH "+name);
                    message = din.readUTF();
                    if(message.contains("KEY")){
                        message = message.substring(4);
                        client.recipientPublicKey = java.util.Base64.getDecoder().decode(message);
                    }
                    else{
                        System.out.println("Key fetch error hererererer");
                    }

                    byte[] privateHash = Cryptography.decryptUsingPublic(client.recipientPublicKey, signatureByte);
                    //System.out.println(name+" : "+content);  /*    rain check       */
                    if(new String(orignalMessageHash).equals(new String(privateHash))){
                        System.out.println(name+" : "+finalMessage+"                // Signature verified");
                    }
                    else{
                        System.out.println(name+" : "+finalMessage+"                // Signature could not be verified");
                    }
                }
                catch(Exception e){
                    System.out.println("Server : "+"Message can't be displayed"+"                // Signature could not be verified");
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}

class SignatureOutputHandlerThread extends Thread{
    
    DataOutputStream dout;
    DataInputStream din;
    String message;
    BufferedReader br;
    
    public SignatureOutputHandlerThread(DataOutputStream dout, DataInputStream din){
        this.dout = dout;
        this.din = din;
        br = new BufferedReader(new InputStreamReader(System.in));
    }
    
    public void run(){
        try{
            while(true){
                message = br.readLine();
                if(message.contains("@") && !message.substring(message.indexOf(" ")+1).equals("")){
                    messageSendHandler(message);
                }
                else if(message.equals("UNREGISTER") || message.equals("unregister")){
                    dout.writeUTF(message);
                }
                else{
                    System.out.println("Invalid message type");
                }
            }
        }
        catch(Exception e){
            System.out.println(e.getStackTrace());
        }
    }

    void messageSendHandler(String message){
        try{
            int idx = message.indexOf(" ");
            client.recipientname = message.substring(1,idx);
            message = message.substring(idx+1);
            dout.writeUTF("FETCH "+client.recipientname);
            Thread.sleep(50);
            byte[] encryptedData = Cryptography.encrypt(client.recipientPublicKey,message.getBytes());
            String encryptedMessage = java.util.Base64.getEncoder().encodeToString(encryptedData);
            byte[] signatureHash = Cryptography.getHash(encryptedData);
            byte[] encryptedSignatureHash = Cryptography.encryptUsingPrivate(client.privateKey, signatureHash);
            String signature = java.util.Base64.getEncoder().encodeToString(encryptedSignatureHash);

            dout.writeUTF("SEND "+client.recipientname);
            dout.writeUTF("Content-length: "+encryptedMessage.length());
            dout.writeUTF("");
            dout.writeUTF(encryptedMessage);
            dout.writeUTF(signature);
        }
        catch(Exception e){
            try{
                System.out.println(e);
                dout.writeUTF("SEND "+client.recipientname);
                dout.writeUTF("Content-length: "+message.length());
                dout.writeUTF("");
                dout.writeUTF(message);
                dout.writeUTF("tempSignature");
            }
            catch(Exception f){
                System.out.println(f);
            }
        }
    }
}
