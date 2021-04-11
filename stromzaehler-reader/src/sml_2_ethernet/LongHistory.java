package sml_2_ethernet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LongHistory {
    public class LongHistoryElement{
        byte Hour; // 0...23
        byte Day; // 1..31
        byte Month; // 1..12
        short Year; // 2020...2xxx
        int value_180;
        int value_280;
    } // takes 13 bytes

    private List<LongHistoryElement> LongHistoryList = new ArrayList<>();

    public LongHistory(){
        // initialize the class
    }
    
    public void add(int value_180, int value_280) {
        LongHistoryElement LHE = new LongHistoryElement();

        Calendar calendar = Calendar.getInstance();
        LHE.Hour = (byte)calendar.get(Calendar.HOUR_OF_DAY);
        LHE.Day = (byte)calendar.get(Calendar.DAY_OF_MONTH);
        LHE.Month = (byte)(calendar.get(Calendar.MONTH)+1); // January is 0, so add 1
        LHE.Year = (short)calendar.get(Calendar.YEAR);
        LHE.value_180=value_180;
        LHE.value_280=value_280;

        LongHistoryList.add(LHE);
    }
    
    // we have several overloaded get() functions for the individual time-ranges
    // return 180 and 280 for a complete year
    public LongHistoryElement get(int Year){
        LongHistoryElement LHE = new LongHistoryElement();
        LHE.Hour=-1;
        LHE.Day=-1;
        LHE.Month=-1;
        LHE.Year=(short)Year;
        LHE.value_180=0;
        LHE.value_280=0;
        
        // accumulate hour-values for a specific year
        for (LongHistoryElement LongHistoryList1 : LongHistoryList) {
            if ((LongHistoryList1.Year==Year)) {
                LHE.value_180+=LongHistoryList1.value_180;
                LHE.value_280+=LongHistoryList1.value_280;
            }
        }

        return LHE;
    }

    // return 180 and 280 for a total month
    public LongHistoryElement get(int Month, int Year){
        LongHistoryElement LHE = new LongHistoryElement();
        LHE.Hour=-1;
        LHE.Day=-1;
        LHE.Month=(byte)Month;
        LHE.Year=(short)Year;
        LHE.value_180=0;
        LHE.value_280=0;
        
        // accumulate hour-values for a specific month
        for (LongHistoryElement LongHistoryList1 : LongHistoryList) {
            if ((LongHistoryList1.Month==Month) && (LongHistoryList1.Year==Year)) {
                LHE.value_180+=LongHistoryList1.value_180;
                LHE.value_280+=LongHistoryList1.value_280;
            }
        }

        return LHE;
    }
    
    // return 180 and 280 for a day
    public LongHistoryElement get(int Day, int Month, int Year){
        LongHistoryElement LHE = new LongHistoryElement();
        LHE.Hour=-1;
        LHE.Day=(byte)Day;
        LHE.Month=(byte)Month;
        LHE.Year=(short)Year;
        LHE.value_180=0;
        LHE.value_280=0;
        
        // accumulate hour-values for a specific day
        for (LongHistoryElement LongHistoryList1 : LongHistoryList) {
            if ((LongHistoryList1.Day==Day) && (LongHistoryList1.Month==Month) && (LongHistoryList1.Year==Year)) {
                LHE.value_180+=LongHistoryList1.value_180;
                LHE.value_280+=LongHistoryList1.value_280;
            }
        }
        
        return LHE;
    }

    public List<LongHistoryElement> get(Date Begin, Date End){
        // the elements are in order - so we can search for the first and last element and put it to the new list
        // so first step: search for the first and last element
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Begin);
        byte Day = (byte)calendar.get(Calendar.DAY_OF_MONTH);
        byte Month = (byte)(calendar.get(Calendar.MONTH)+1);
        byte Year = (byte)calendar.get(Calendar.YEAR);
        int FirstElement = 0;
        for (int i=0; i<LongHistoryList.size(); i++) {
            if ((LongHistoryList.get(i).Year==Year) && (LongHistoryList.get(i).Month==Month) && (LongHistoryList.get(i).Day==Day)) {
                FirstElement=i;
                break;
            }
        }

        calendar.setTime(End);
        Day = (byte)calendar.get(Calendar.DAY_OF_MONTH);
        Month = (byte)(calendar.get(Calendar.MONTH)+1);
        Year = (byte)calendar.get(Calendar.YEAR);
        int LastElement = 0;
        for (int i=LongHistoryList.size(); i>0; i--) {
            if ((LongHistoryList.get(i).Year==Year) && (LongHistoryList.get(i).Month==Month) && (LongHistoryList.get(i).Day==Day)) {
                LastElement=i;
                break;
            }
        }
        
        // second step: return a sublist with all elements between first and last element
        return LongHistoryList.subList(FirstElement, LastElement);
    }

    public int sizeInBytes() {
        return 4+13*LongHistoryList.size();
    }
    
    public ByteBuffer toByteBuffer(){
        // 4 byte size + 13 bytes for each LongHistoryElement
        ByteBuffer buffer = ByteBuffer.allocate(sizeInBytes());

        buffer.putInt(LongHistoryList.size());
        for (int i=0; i<LongHistoryList.size(); i++) {
            buffer.put(LongHistoryList.get(i).Hour);
            buffer.put(LongHistoryList.get(i).Day);
            buffer.put(LongHistoryList.get(i).Month);
            buffer.putShort(LongHistoryList.get(i).Year);
            buffer.putInt(LongHistoryList.get(i).value_180);
            buffer.putInt(LongHistoryList.get(i).value_280);
        }

        buffer.flip();       
        
        return buffer;
    }
    
    public void fromByteBuffer(ByteBuffer ByteBufferData){
        LongHistoryList.clear();
        int numElements = ByteBufferData.getInt();
        for (int i=0; i<numElements; i++) {
            LongHistoryElement LHE = new LongHistoryElement();
            
            LHE.Hour=ByteBufferData.get();
            LHE.Day=ByteBufferData.get();
            LHE.Month=ByteBufferData.get();
            LHE.Year=ByteBufferData.getShort();
            LHE.value_180=ByteBufferData.getInt();
            LHE.value_280=ByteBufferData.getInt();

            LongHistoryList.add(LHE);
        }
    }
}
