/*
 * Copyright 2021 Dr.-Ing. Christian Nöding
 *
 * This ESP8266-program reads the calculated control-value of the SML_2_Ethernet-App
 * via Ethernet (WiFi) and outputs this value as PWM on pin 12 and via DMX512 on pin 
 *
 * For more information visit http://www.pcdimmer.de
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this application.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This software uses parts of the OpenMUC-software copyright 2011-2020 Fraunhofer ISE.
 * For more information visit http://www.openmuc.org
 *
 */

#include <ESP8266WiFi.h>
#include <Ticker.h> // timer
#include <stdlib.h> // atoi()
#include "espDMX.h" // Schematics and more here: https://github.com/mtongnz/espDMX, https://github.com/mtongnz/espDMX/blob/master/examples/dmxChaser/dmxChaser.ino, https://www.instructables.com/ESP8266-Artnet-to-DMX/

extern "C" {
#include "user_interface.h"
#include "wpa2_enterprise.h"
#include "c_types.h"
}

// define the hardware-configuration
#define LED1 5 // D1
#define LED2 4 // D2
#define PWM1 12 // D6
#define PWM2 13 // D7
//#define PWM3 14 // D5

// define Tickers
Ticker TimerSeconds;

// Setup the WiFi
bool WiFi_WPA2EAP = true; // if true -> WPA2-Enterprise, if false -> WPA2-PSK
const char *ssid = "MyWiFi";
const char *password = "PSK-Password";
char wpa2eap_ssid[] = "MyWiFi";
char wpa2eap_user[] = "Username";
char wpa2eap_identity[] = "Username";
char wpa2eap_password[] = "Password";
//uint8_t target_esp_mac[6] = {0x24, 0x0a, 0xc4, 0x9a, 0x58, 0x28};

// SocketCommunication to SML_2_Ethernet-Server
WiFiClient client;
const char* host = "192.168.0.33"; //IP des Java-Servers
const int serverPort = 51534; //Port des Java-Servers (ServerSocket)

// Begin of variable-definitions ----------------------------------------------------
union uData16bit
{
   uint16_t data_u16;
   int16_t data_i16;
   uint8_t data_u8[2];
   int8_t data_i8[2];
};
uData16bit myData16bit;

union uData32bit
{
   float data_f;
   uint32_t data_u32;
   int32_t data_i32;
   uint16_t data_u16[2];
   int16_t data_i16[2];
   uint8_t data_u8[4];
   int8_t data_i8[4];
};
uData32bit myData32bit;

int i_var;
uint8_t b_var;
boolean SecTick=false;
uint8_t DMXvalue=0;
uint8_t DMXdata[32];

// End of variable-definitions ----------------------------------------------------
// Beginn of general functions ----------------------------------------------------

int endianSwap4int(int a) {
    uData32bit un;
    un.data_i32 = a;

    // swap bytes
    uint8_t c1 = un.data_u8[0];
    un.data_u8[0] = un.data_u8[3];
    un.data_u8[3] = c1;
    c1 = un.data_u8[1];
    un.data_u8[1] = un.data_u8[2];
    un.data_u8[2] = c1;

    return un.data_i32;
}

// End of functions -----------------------------------------------------

void TimerSecondsFcn() {
  // toggle Status-LED
  digitalWrite(LED_BUILTIN, !digitalRead(LED_BUILTIN));

  SecTick=true;
}

// setup-function called before loop()
void setup() {
  // setup hardware-pins
  pinMode(LED_BUILTIN, OUTPUT);     // Initialize the LED_BUILTIN pin as an output
  pinMode(LED1, OUTPUT);
  pinMode(LED2, OUTPUT);
  pinMode(PWM1, OUTPUT);
  pinMode(PWM2, OUTPUT);
  //pinMode(PWM3, OUTPUT);

  // Start the system
  digitalWrite(LED1, HIGH);
  digitalWrite(LED2, HIGH);
  analogWrite(PWM1, 0);
  analogWrite(PWM2, 0);
  //analogWrite(PWM3, 0);
  delay(1000);

  Serial.begin(115200);
  Serial.println();

  // Start WiFi (AP or Client)
  WiFi.disconnect();
  Serial.println("Configuring wifi client-connection...");
  WiFi.mode(WIFI_STA);

  if (WiFi_WPA2EAP){
    Serial.println("Using WPA2-Enterprise-Mode...");
    wifi_set_opmode(STATION_MODE);
    struct station_config wifi_config;

    memset(&wifi_config, 0, sizeof(wifi_config));
    strcpy((char*)wifi_config.ssid, wpa2eap_ssid);
    strcpy((char*)wifi_config.password, wpa2eap_password);

    wifi_station_set_config(&wifi_config);
    //wifi_set_macaddr(STATION_IF,target_esp_mac);
    

    wifi_station_set_wpa2_enterprise_auth(1);

    // Clean up to be sure no old data is still inside
    wifi_station_clear_cert_key();
    wifi_station_clear_enterprise_ca_cert();
    wifi_station_clear_enterprise_identity();
    wifi_station_clear_enterprise_username();
    wifi_station_clear_enterprise_password();
    wifi_station_clear_enterprise_new_password();
    
    wifi_station_set_enterprise_identity((uint8*)wpa2eap_identity, strlen(wpa2eap_identity));
    wifi_station_set_enterprise_username((uint8*)wpa2eap_user, strlen(wpa2eap_user));
    wifi_station_set_enterprise_password((uint8*)wpa2eap_password, strlen((char*)wpa2eap_password));

    wifi_station_connect();
  }else{
    Serial.println("Using WPA2-PSK-Mode...");
    WiFi.begin(ssid, password);
  }

  // Wait for connection
  while (WiFi.status() != WL_CONNECTED) {
      delay(500);
      Serial.print(".");
  }
  Serial.println("WiFi connected");
  Serial.print("My IP address: ");
  Serial.println(WiFi.localIP());
  Serial.print("WiFi Signal: ");
  Serial.print(WiFi.RSSI());
  Serial.println(" dBm");

  // Initiate the timers
  TimerSeconds.attach_ms(1000, TimerSecondsFcn);
  Serial.println("Timer started");

  // Initiate DMX512 on second Serial-Port. DMXA = Serial (Standard-UART), DMXB = Serial1
  dmxB.begin(14, 255); // LED-Pin, Intensity (0...255)
  Serial.println("DMX512 started");

  digitalWrite(LED1, LOW);
  digitalWrite(LED2, LOW);
  Serial.println("Startup finished. Entering Main-Loop...");
}

void loop() {
  Serial.printf("Connecting to %s ... ", host);
  if (client.connect(host, serverPort)) {
    Serial.println("connected");
    //client.setTimeout(3000);

    while (client.connected()) {
      if (SecTick) {
        SecTick=false;

        // clear receive-buffer
        for (int i=0; i<client.available(); i++) {
          client.read();
        }

        // write command to server
        //myData32bit.data_i32 = endianSwap4int(14); // number of UTF8-chars (16-bit per char!)
        //client.write(myData32bit.data_u8, 4); // we swapped the bytes before, so we can transmit all 4 bytes which are big-endian now
        client.write(14); // send number of following chars
        client.print("C:PWRCTRL_CALC"); // send command

        /*
          // statt mit union kann man auch pointer nehmen
          float temp_float;
          int *temp_float;
          temp_float = 14.3;
          *temp_float = &temp_float;
          client.write(*temp_float, 4);
        
          // oder so:
          float f1 = 14.3;
          byte* fb = (byte*) &f1;
        
          Serial.println(fb[0]);    // Take a look at the bytes
          Serial.println(fb[1]);
          Serial.println(fb[2]);
          Serial.println(fb[3]);
        */
    
        // read the answer from server
        //int = client.read()
        //int = client.read(uint8_t* buf, size_t size)
        //int = client.read(char* buf, size_t size)

        // wait for data
        delay(500);
        while (client.available()<4) {
          delay(10);
        }
        // if we received more than expected, ignore some of the bytes
        if (client.available()>4){
          for (int i=0; i<client.available()-3; i++) {
            client.read();
          }
        }        
        int ReadBytes = client.read(myData32bit.data_u8, 4);
        myData32bit.data_i32 = endianSwap4int(myData32bit.data_i32);
        Serial.printf("Received new Data = %f -> ", myData32bit.data_f);
        
        // do something with the result
    
        // limit the value between 0 and 1
        if (myData32bit.data_f<0) {
          myData32bit.data_f=0;
        }else if (myData32bit.data_f>1000000) {
          // illegal number -> set PWM to 0%
          myData32bit.data_f=0;
        }else if (myData32bit.data_f>1) {
          myData32bit.data_f=1;
        }
        
        // write calculated power-control-value to PWM-output. It will increase duty-cycle on high sun-radiation and reduce the duty-cycle on less or no radiation
        analogWrite(PWM1, myData32bit.data_f*1023); // convert to 10-bit-data 0...1023
        Serial.printf("Set PWM to %f\n", myData32bit.data_f);
        DMXvalue=myData32bit.data_f*255;        
        for (int i=0; i<32; i++) {
          DMXdata[i]=DMXvalue; // convert to DMX-data 0...255
        }
        dmxB.setChans(DMXdata, 32, 1); // output 32-bytes starting from channel 1
      }else{
        // wait for the next second        
        delay(1);
      }
    }
  }else{
    Serial.println("connection failed!");
    client.stop();
  }
  
  //delay(5000);
}
