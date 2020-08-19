/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hackerdan.sml;

/**
 *
 * @author Dr.-Ing. Christian Nöding
 */
public class SmlConfig{
    private static SmlConfig instance;
    
    private String SerialPort = "/dev/ttyUSB0";
    private int Baudrate = 9600;
    private int ServerIPPort = 51354;
    private String WiringPiBinaryPath = "/usr/local/bin/gpio";
    private int WiringPiLEDGPIO = 0;
    
    public static SmlConfig getInstance() {
        if (instance == null) {
            instance = new SmlConfig();
        }
        return instance;
    }
    
    // getter and setter for this class
    public void setSerialPort(String value) { SerialPort = value; }
    public String getSerialPort() { return SerialPort; }

    public void setBaudrate(int value) { Baudrate = value; }
    public int getBaudrate() { return Baudrate; }

    public void setServerIPPort(int value) { ServerIPPort = value; }
    public int getServerIPPort() { return ServerIPPort; }

    public void setWiringPiBinaryPath(String port) { WiringPiBinaryPath = port; }
    public String getWiringPiBinaryPath() { return WiringPiBinaryPath; }

    public void setWiringPiLEDGPIO(int value) { WiringPiLEDGPIO = value; }
    public int getWiringPiLEDGPIO() { return WiringPiLEDGPIO; }
}
