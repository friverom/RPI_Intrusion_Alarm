/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rpi_intrusion_alarm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rpio_client.Net_RPI_IO;
import util.ReadTextFile;
import util.WriteTextFile;

/** 
 *
 * @author Federico
 */
public class IntrusionAlarm {
    
    public static final int RDR_DOOR = 1; //door sensor input port
    public static final int GEN_DOOR = 2; //door sensor input port
    public static final int ALARM = 2; //alarm output relay
    public static final int INTRUSIONTASK = 1;
    public static final int TASKLEVEL = 1;
        
    Net_RPI_IO rpio = null;
    private String address = "";
    private boolean runFlag = false;
    private boolean enableAlarm = true;
    private int alarmState = 0;
    private int reset_value = 3600; //Reset alarm timer 1 hours
    private int reset_timer = 0;
    
    public IntrusionAlarm(String address) throws IOException{
        this.address=address;
        this.rpio = new Net_RPI_IO(this.address,30000);
        readSettings();
    }
    public IntrusionAlarm() throws IOException{
        this.address="localhost";
        this.rpio = new Net_RPI_IO(this.address,30000);
        readSettings();
    }
    
    public void start(){
        runFlag = true;
        Thread alarmTask = new Thread(new IntrusionTask(),"Intrusion Task");
        alarmTask.start();
    }
    
    public String killThread() throws IOException{
        runFlag = false;
        rpio.resetRly(INTRUSIONTASK, TASKLEVEL, ALARM);
        saveSettings();
        return "Intrusion task stopped";
    }
    // Reset alarm after xx secs
    public String reset_value(int value) throws IOException{
        reset_value=(int)value*60;
        saveSettings();
        return "Reset Timer: "+value;
    }
    
    public String getTimer(){
        return ""+(int)reset_value/60;
    }
    
    public String resetAlarm(){
        alarmState=0;
        reset_timer=0;
        rpio.resetRly(INTRUSIONTASK, TASKLEVEL, ALARM);
        return "alarm reseted";
    }

    public String disableAlarm() throws IOException{
        enableAlarm=false;
        saveSettings();
        return "disabled";
    }
    
    public String enableAlarm() throws IOException{
        enableAlarm=true;
        saveSettings();
        return "enabled";
    }
    
    public String getStatus(){
        String resp="";
        String rdr_door="";
        String gen_door="";
        String alarm="";
        String status="";
        
        if(get_door_status(RDR_DOOR)){
            rdr_door="CLOSE";
        } else {
            rdr_door="OPEN";
        }
        
        if(get_door_status(GEN_DOOR)){
            gen_door="CLOSE";
        } else {
            gen_door="OPEN";
        }
        
        if(enableAlarm){
            alarm="ENABLE";
        } else {
            alarm="DISABLE";
        }
        
        if(alarmState==1){
            status="ALARM";
        } else {
            status="NORMAL";
        }
        resp="Radar Room Door: "+rdr_door+"\n";
        resp=resp+"Generator Room Door: "+gen_door+"\n";
        resp=resp+"Intrusion Alarm: "+alarm+"\n";
        resp=resp+"Alarm status: "+status+"\n";
        
        return resp;
    }
    
    public class IntrusionTask implements Runnable{
        
        String resp="";
        @Override
        public void run() {

            resp=rpio.setLock(INTRUSIONTASK,TASKLEVEL,ALARM);

            while (runFlag) {
                if (enableAlarm && alarmState == 0) {
                    if (!get_door_status(RDR_DOOR) || !get_door_status(GEN_DOOR)) {
                        resp=rpio.setRly(INTRUSIONTASK, TASKLEVEL, ALARM);
                        alarmState = 1;
                        reset_timer=0;
                    }
                } else if (alarmState == 1){
                    if (get_door_status(RDR_DOOR) && get_door_status(GEN_DOOR)){
                        reset_timer++;
                        if(reset_timer>=reset_value){
                            resp=rpio.resetRly(INTRUSIONTASK, TASKLEVEL, ALARM);
                            alarmState=0;
                            reset_timer=0;
                        }
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(IntrusionAlarm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            rpio.releaseLock(INTRUSIONTASK, TASKLEVEL, ALARM);
        }
        
    }
    private boolean get_door_status(int door){
        String resp = rpio.getInput(INTRUSIONTASK, TASKLEVEL, door);
        String parts[] = resp.split(",");
        boolean status;
        if(parts.length==3){
            status=Boolean.parseBoolean(parts[2]);
        } else {
            status = true;
        }
        return status;
    }
    
    /**
     * Reads variable setting for Intrusion task.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void readSettings() throws FileNotFoundException, IOException{
        String path = "/home/pi/NetBeansProjects/RPI_Intrusion_Alarm/vars.txt";
        File file = new File(path);
       
        if(!file.exists()){
            file.createNewFile();
            saveSettings();
        }
        
        ReadTextFile rf = new ReadTextFile(path);
        String[] lines=rf.openFile();
        
        String text = null;
        String[] parts = lines[0].split(";",3);
        
        if(parts.length==3){
                reset_value=Integer.parseInt(parts[0]);
                enableAlarm=Boolean.parseBoolean(parts[1]);
                
            }
      
    }
    
     /**
     * Saves variable settings to file
     * @return
     * @throws IOException 
     */
    private void saveSettings() throws IOException{
        
        String path = "/home/pi/NetBeansProjects/RPI_Intrusion_Alarm/vars.txt";
        
        WriteTextFile write = new WriteTextFile(path,false);        
        String data="";
        data=reset_value+";"+enableAlarm+";"+"spare"+"\n";
        write.writeToFile(data);
    }
}
