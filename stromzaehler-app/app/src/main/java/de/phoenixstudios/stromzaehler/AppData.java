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

    public byte HistoryLevel=0;

    public int[] history_value_180_hour = new int[168]; // values for the last 7 days
    public int[] history_value_280_hour = new int[168]; // values for the last 7 days
    public int[] history_power_seconds = new int[604800]; // values for the last 7 days = 168h * 60min * 60s = 604800

    public LongHistory longHistory = new LongHistory();

    public ByteBuffer toByteBuffer(int DesiredHistoryLevel) {
        HistoryLevel=(byte)DesiredHistoryLevel;
        ByteBuffer buffer;
        switch (HistoryLevel) {
            case 1:
                // current values and hourly-based values for the last 7 days
                buffer = ByteBuffer.allocate(2+21+1344); // 1.3kB per message
                break;
            case 2:
                // current values, hourly-based values and second-based power-curve for the last 7 days
                buffer = ByteBuffer.allocate(2+21+1344+2419200); // 2.3MB (uncompressed)
                break;
            case 3:
                // total hourly-based history for 180- and 280-values (13-bytes per hour -> 111kB per year)
                buffer = ByteBuffer.allocate(2+21+4+longHistory.sizeInBytes());
                break;
            default:
                buffer = ByteBuffer.allocate(2+21);
                break;
        }
        buffer.putShort(Version);

        buffer.putInt(value_180);
        buffer.putInt(value_280);
        buffer.putInt(value_180_day);
        buffer.putInt(value_280_day);
        buffer.putInt(power);

        buffer.put(HistoryLevel);

        // add hourly-based ring-buffer of 180 and 280 values for the last 7 days
        if ((HistoryLevel==1) || (HistoryLevel==2)) {
            for (int i=0; i<history_value_180_hour.length; i++) {
                buffer.putInt(history_value_180_hour[i]);
            }
            for (int i=0; i<history_value_280_hour.length; i++) {
                buffer.putInt(history_value_280_hour[i]);
            }

            // add power on a seconds-base
            if (HistoryLevel==2) {
                for (int i=0; i<history_power_seconds.length; i++) {
                    buffer.putInt(history_power_seconds[i]);
                }
            }
        }

        // add full LongHistory
        if (HistoryLevel==3) {
            int numberOfBytes = longHistory.sizeInBytes();
            byte[] LongHistoryArray = longHistory.toByteBuffer().array();
            buffer.putInt(numberOfBytes);
            buffer.put(LongHistoryArray);
        }

        // from here version 2

        buffer.flip();

        return buffer;
    }

    public void fromByteBuffer(ByteBuffer ByteBufferData) {
        Short ReceivedVersion = ByteBufferData.getShort();

        value_180 = ByteBufferData.getInt();
        value_280 = ByteBufferData.getInt();
        value_180_day = ByteBufferData.getInt();
        value_280_day = ByteBufferData.getInt();
        power = ByteBufferData.getInt();

        HistoryLevel = ByteBufferData.get();

        // get hourly-based ring-buffer of 180 and 280 values for the last 7 days
        if ((HistoryLevel==1) || (HistoryLevel==2)) {
            for (int i=0; i<history_value_180_hour.length; i++) {
                history_value_180_hour[i] = ByteBufferData.getInt();
            }
            for (int i=0; i<history_value_280_hour.length; i++) {
                history_value_280_hour[i] = ByteBufferData.getInt();
            }

            // get power on a seconds-base
            if (HistoryLevel==2) {
                for (int i=0; i<history_power_seconds.length; i++) {
                    history_power_seconds[i] = ByteBufferData.getInt();
                }
            }
        }

        // get full LongHistory
        if (HistoryLevel==3) {
            int numberOfBytes = ByteBufferData.getInt();
            byte[] LongHistoryArray = new byte[numberOfBytes];
            ByteBufferData.get(LongHistoryArray, 0, numberOfBytes);
            longHistory.fromByteBuffer(ByteBuffer.wrap(LongHistoryArray));
        }

        // bei neuerer Version einfach hier hinten weitere Daten anhängen
        if (ReceivedVersion>=2){
        }
        if (ReceivedVersion>=3){
        }
    }
}
