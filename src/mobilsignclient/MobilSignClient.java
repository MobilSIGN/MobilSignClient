/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobilsignclient;

import java.io.*;
import java.net.*;
//import javax.net.ssl.SSLSocket;
//import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author Marek Spalek <marekspalek@gmail.com>
 */
public class MobilSignClient {

    public static final String SERVER_HOSTNAME = "localhost";
    //public static final String SERVER_HOSTNAME = "10.0.1.3";
    public static final int SERVER_PORT = 2002;
 
    public static void main(String[] args)
    {
        MobilSignClient.connectToServer(SERVER_HOSTNAME, SERVER_PORT);
    }
    
    private static void connectToServer(String hostName, int port)
    {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
           // SSL
//           SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
//           SSLSocket sslsocket = (SSLSocket)factory.createSocket(SERVER_HOSTNAME, SERVER_PORT);
            
           // Connect to Server
           Socket socket = new Socket(hostName, port);
           in = new BufferedReader(
               new InputStreamReader(socket.getInputStream()));
           out = new PrintWriter(
               new OutputStreamWriter(socket.getOutputStream()));
           System.out.println("Connected to server " +
              hostName + ":" + port);
        } catch (IOException ioe) {
           System.err.println("Can not establish connection to " +
               hostName + ":" + port);
           ioe.printStackTrace(System.out);
           System.exit(-1);
        }
 
        // Create and start Sender thread
        Sender sender = new Sender(out);
        sender.setDaemon(true);
        sender.start();
 
        try {
           // Read messages from the server and print them
            String message;
           while ((message=in.readLine()) != null) {
               System.out.println(message);
               MobilSignClient.dispatchMessage(message);
           }
        } catch (IOException ioe) {
           System.err.println("Connection to server broken.");
           ioe.printStackTrace(System.out);
        }
    }
    
    private static void dispatchMessage(String aMessage)
    {
        if (aMessage.length() > 5 && aMessage.substring(0, 5).equals("SEND:")) {
            System.out.println("Message recieved: [" + aMessage.substring(5) + "]");
            return;
        }
        
        if (aMessage.length() > 5 && aMessage.substring(0, 5).equals("RESP:")) {
            if(aMessage.substring(5).equals("paired")) {
                System.out.println("Response: [" + aMessage.substring(5) + "]");
                return;
            }
        }
        System.out.println("Unknown message: [" + aMessage + "]");
    }
}
