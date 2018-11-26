/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rpi_intrusion_alarm;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rpio_client.Net_RPI_IO;

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
    
    public IntrusionAlarm(String address){
        this.address=address;
        this.rpio = new Net_RPI_IO(this.address,30000);
    }
    public IntrusionAlarm(){
        this.address="localhost";
        this.rpio = new Net_RPI_IO(this.address,30000);
    }
    
    public void start(){
        runFlag = true;
        Thread alarmTask = new Thread(new IntrusionTask(),"Intrusion Task");
        alarmTask.start();
    }
    
    public String killThread(){
        runFlag = false;
        rpio.resetRly(INTRUSIONTASK, TASKLEVEL, ALARM);
        return "Intrusion task stopped";
    }
    
    public String reset_value(int value){
        reset_value=value*60;
        return "Reset Timer: "+value;
    }
    
    public String resetAlarm(){
        alarmState=0;
        reset_timer=0;
        rpio.resetRly(INTRUSIONTASK, TASKLEVEL, ALARM);
        return "alarm reseted";
    }

    public String disableAlarm(){
        enableAlarm=false;
        return "disabled";
    }
    
    public String enableAlarm(){
        enableAlarm=true;
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
        
        @Override
        public void run() {

            rpio.setLock(INTRUSIONTASK,TASKLEVEL,ALARM);

            while (runFlag) {
                if (enableAlarm && alarmState == 0) {
                    if (!get_door_status(RDR_DOOR) || !get_door_status(GEN_DOOR)) {
                        rpio.setRly(INTRUSIONTASK, TASKLEVEL, ALARM);
                        alarmState = 1;
                    }
                } else if (alarmState == 1){
                    if (get_door_status(RDR_DOOR) && get_door_status(GEN_DOOR)){
                        reset_timer++;
                        if(reset_timer>=reset_value){
                            rpio.resetRly(INTRUSIONTASK, TASKLEVEL, ALARM);
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
}
