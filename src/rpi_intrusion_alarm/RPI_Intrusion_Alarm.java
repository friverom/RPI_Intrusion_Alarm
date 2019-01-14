
package rpi_intrusion_alarm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/** This class activates the Intrusion Alarm feature on RPI_IO board.
 * An IntrusionALarm class is first instantiated that will start a thread
 * to run the intrusion alarm application.
 * Then it start to listen on port 30001 for commands to control the application
 * @author Federico
 */
public class RPI_Intrusion_Alarm {

    static IntrusionAlarm alarm = null;
    static boolean runFlag = true;
    static ServerSocket serversocket = null;
    static Socket socket = null;
    static InputStream in = null;
    static BufferedReader input = null;
    static PrintWriter output = null;
    
    public static void main(String[] args) throws IOException {
    //Check if arguments passed to main. If no args, start INtrusionAlarm class
    //with default address "localhost". Otherwise pass the address to the 
    //IntrusionAlarm constructor.
        if (args.length==0){
            alarm = new IntrusionAlarm();
        } else {
            alarm = new IntrusionAlarm(args[0]);
        }
        alarm.start();
        
        //Start to listen on port 30001 for commands
        try {
            serversocket = new ServerSocket(30001);
        } catch (IOException ex) {
            Logger.getLogger(RPI_Intrusion_Alarm.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Loop until Kill Thread command received
        while(runFlag){
            try {
                waitRequest(); //Wait for command and process request.
            } catch (IOException ex) {
                Logger.getLogger(RPI_Intrusion_Alarm.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        serversocket.close();
        
    }
    /**
     * This method wait for a connection, get the command.
     * @throws IOException 
     */
    private static void waitRequest() throws IOException {
       
        String request = "";
        String reply = "";
        socket = serversocket.accept();
        in = socket.getInputStream();
        input = new BufferedReader(new InputStreamReader(in));
        output = new PrintWriter(socket.getOutputStream(), true);
        request = input.readLine(); //Get Command
        reply = processRequest(request); //Process command
        output.println(reply);
        input.close();
        output.close();
    }
    
    /**
     * This method Process the command
     * @param request
     * @return 
     */
    private static String processRequest(String request){
        String reply="";
        String command="";
        int data=0;
        
        String parts[]=request.split(",");
        
        if(parts.length==1){
            command=request;
        }else{
            command=parts[0];
            data=Integer.parseInt(parts[1]);
        }
        
        switch(command){
            case "get status":
                reply=getStatus();
                break;
            //Enable intrusion alarm    
            case "enable alarm":
                reply=alarm.enableAlarm();
                break;
                
            case "disable alarm":
                reply=alarm.disableAlarm();
                break;
                
            case "reset alarm":
                reply=alarm.resetAlarm();
                break;
                
            case "kill thread":
                runFlag = false;
                reply=alarm.killThread();
                break;
            // Automatic alarm reset after x minutes.
            case "set alarm timer":
                reply=alarm.reset_value(data);
                break;
            case "get alarm timer":
                reply=alarm.getTimer();
                break;
                
            default:
                reply="invalid command";
        }
        return reply;
    }
    
    private static String getStatus(){
        return alarm.getStatus();
    }
    
    private static String enableAlarm(){
        return alarm.enableAlarm();
    }
    
    private static String disableAlarm(){
        return alarm.disableAlarm();
    }
    
    private static String resetAlarm(){
        return alarm.resetAlarm();
    }
    private static String killThread(){
        alarm.killThread();
        runFlag=false;
        return "killed";
    }
    
}
