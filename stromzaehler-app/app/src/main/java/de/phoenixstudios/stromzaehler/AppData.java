package de.phoenixstudios.stromzaehler;

import java.nio.ByteBuffer;

public class AppData {
    // 1x 2 byte
    public static short Version = 1;

    // 5x 4 byte = 20 byte
    public int value_180=0;
    public int value_280=0;
    public int value_180_day=0;
    public int value_280_day=0;
    public int power=0;

    public int[] history_value_180_hour = new int[168]; // values for the last 7 days
    public int[] history_value_280_hour = new int[168]; // values for the last 7 days
    public int[] history_power_seconds = new int[604800]; // values for the last 7 days = 168h * 60min * 60s = 604800

    public ByteBuffer toByteBuffer(int HistoryLevel) {
        ByteBuffer buffer;
        switch (HistoryLevel) {
            case 1:
                buffer = ByteBuffer.allocate(2+20+1344);
                break;
            case 2:
                buffer = ByteBuffer.allocate(2+20+1344+2419200);
                break;
            default:
                buffer = ByteBuffer.allocate(2+20);
                break;
        }
        buffer.putShort(Version);

        buffer.putInt(value_180);
        buffer.putInt(value_280);
        buffer.putInt(value_180_day);
        buffer.putInt(value_280_day);
        buffer.putInt(power);

        if (HistoryLevel>=1) {
            for (int i=0; i<history_value_180_hour.length; i++) {
                buffer.putInt(history_value_180_hour[i]);
            }
            for (int i=0; i<history_value_280_hour.length; i++) {
                buffer.putInt(history_value_280_hour[i]);
            }
        }
        if (HistoryLevel>=2) {
            for (int i=0; i<history_power_seconds.length; i++) {
                buffer.putInt(history_power_seconds[i]);
            }
        }

        // from here version 2

        buffer.flip();

        return buffer;
    }

    public void fromByteBuffer(ByteBuffer ByteBufferData, int HistoryLevel) {
        Short ReceivedVersion = ByteBufferData.getShort();

        value_180 = ByteBufferData.getInt();
        value_280 = ByteBufferData.getInt();
        value_180_day = ByteBufferData.getInt();
        value_280_day = ByteBufferData.getInt();
        power = ByteBufferData.getInt();

        if (HistoryLevel>=1) {
            for (int i=0; i<history_value_180_hour.length; i++) {
                history_value_180_hour[i] = ByteBufferData.getInt();
            }
            for (int i=0; i<history_value_280_hour.length; i++) {
                history_value_280_hour[i] = ByteBufferData.getInt();
            }
        }
        if (HistoryLevel>=2) {
            for (int i=0; i<history_power_seconds.length; i++) {
                history_power_seconds[i] = ByteBufferData.getInt();
            }
        }

        // bei neuerer Version einfach hier hinten weitere Daten anhängen
        if (ReceivedVersion>=2){
        }
        if (ReceivedVersion>=3){
        }
    }
}
