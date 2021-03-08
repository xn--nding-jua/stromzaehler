package de.phoenixstudios.stromzaehler;

import java.nio.ByteBuffer;

public class AppData {
    public static int length = 7410;

    // 1x 2 byte
    public static short Version = 1;
    
    // 4x 4 byte = 16 byte
    public float value_180=0;
    public float value_280=0;
    public float value_180_day=0;
    public float value_280_day=0;

    // 11x 4 byte = 44 byte
    public class cValues {
        public float power_total=0;
        public float power_phase1=0;
        public float power_phase2=0;
        public float power_phase3=0;
        public float current_phase1=0;
        public float current_phase2=0;
        public float current_phase3=0;
        public float voltage_phase1=0;
        public float voltage_phase2=0;
        public float voltage_phase3=0;
        public float temperature=0;
    }
    
    // 168x 44 byte = 7392 byte
    public cValues[] Values = new cValues[168];
    
    public AppData() {
        // initialize the Class
        for(int i=0; i<Values.length; i++) {
            Values[i] = new cValues();
        }
    }
    
    public ByteBuffer toByteBuffer(int Elements) {
        ByteBuffer buffer = ByteBuffer.allocate(Elements*44+18);
        buffer.putShort(Version);

        buffer.putFloat(value_180);
        buffer.putFloat(value_280);
        buffer.putFloat(value_180_day);
        buffer.putFloat(value_280_day);

        for (int i=0; i<Elements; i++) {
            buffer.putFloat(Values[i].power_total);
            buffer.putFloat(Values[i].power_phase1);
            buffer.putFloat(Values[i].power_phase2);
            buffer.putFloat(Values[i].power_phase3);
            buffer.putFloat(Values[i].current_phase1);
            buffer.putFloat(Values[i].current_phase2);
            buffer.putFloat(Values[i].current_phase3);
            buffer.putFloat(Values[i].voltage_phase1);
            buffer.putFloat(Values[i].voltage_phase2);
            buffer.putFloat(Values[i].voltage_phase3);
            buffer.putFloat(Values[i].temperature);
        }

        // from here version 2

        buffer.flip();       
        
        return buffer;
    }
    
    public void fromByteBuffer(ByteBuffer ByteBufferData, int Elements) {
        Short ReceivedVersion = ByteBufferData.getShort();
        
        value_180 = ByteBufferData.getFloat();
        value_280 = ByteBufferData.getFloat();
        value_180_day = ByteBufferData.getFloat();
        value_280_day = ByteBufferData.getFloat();

        for (int i=0; i<Elements; i++) {
            Values[i].power_total = ByteBufferData.getFloat();
            Values[i].power_phase1 = ByteBufferData.getFloat();
            Values[i].power_phase2 = ByteBufferData.getFloat();
            Values[i].power_phase3 = ByteBufferData.getFloat();
            Values[i].current_phase1 = ByteBufferData.getFloat();
            Values[i].current_phase2 = ByteBufferData.getFloat();
            Values[i].current_phase3 = ByteBufferData.getFloat();
            Values[i].voltage_phase1 = ByteBufferData.getFloat();
            Values[i].voltage_phase2 = ByteBufferData.getFloat();
            Values[i].voltage_phase3 = ByteBufferData.getFloat();
            Values[i].temperature = ByteBufferData.getFloat();
        }
        
        // bei neuerer Version einfach hier hinten weitere Daten anhängen
        if (ReceivedVersion>=2){
        }
        if (ReceivedVersion>=3){
        }
    }
}
