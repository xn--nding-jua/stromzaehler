/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sml_2_ethernet;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openmuc.jsml.structures.ASNObject;
import org.openmuc.jsml.structures.Integer16;
import org.openmuc.jsml.structures.Integer32;
import org.openmuc.jsml.structures.Integer64;
import org.openmuc.jsml.structures.Integer8;
import org.openmuc.jsml.structures.OctetString;
import org.openmuc.jsml.structures.SmlListEntry;
import org.openmuc.jsml.structures.Unsigned16;
import org.openmuc.jsml.structures.Unsigned32;
import org.openmuc.jsml.structures.Unsigned64;
import org.openmuc.jsml.structures.Unsigned8;

/**
 *
 * @author chris
 */
public class HelperFunctions {
    static void CallHttp(String Url){
        try{
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(Url.replace(" ", "%20"));
            CloseableHttpResponse response1 = httpclient.execute(httpGet);
            response1.close();
        }catch(IOException error){
            System.out.println(error);
        }finally{
        }
    }

    static String convertBytesToHexString(byte[] data) {
        return bytesToHex(data);
    }
    
    static final String HEXES = "0123456789ABCDEF";

    static String bytesToHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
    
    static class ValueContainer {
        final Value value;
        private final ValueType valueType;

        public ValueContainer(Value value, ValueType valueType) {
            this.value = value;
            this.valueType = valueType;
        }
    }

    static ValueContainer extractValueOf(SmlListEntry entry) {
        double value = 0;
        ValueType valueType = ValueType.DOUBLE;

        ASNObject obj = entry.getValue().getChoice();
        if (obj.getClass().equals(Integer64.class)) {
            Integer64 val = (Integer64) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer32.class)) {
            Integer32 val = (Integer32) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer16.class)) {
            Integer16 val = (Integer16) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer8.class)) {
            Integer8 val = (Integer8) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned64.class)) {
            Unsigned64 val = (Unsigned64) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned32.class)) {
            Unsigned32 val = (Unsigned32) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned16.class)) {
            Unsigned16 val = (Unsigned16) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned8.class)) {
            Unsigned8 val = (Unsigned8) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(OctetString.class)) {
            OctetString val = (OctetString) obj;
            return new ValueContainer(new StringValue(new String(val.getValue())), ValueType.STRING);
        }
        else {
            return new ValueContainer(new DoubleValue(Double.NaN), valueType);
        }

        byte scaler = entry.getScaler().getVal();
        double scaledValue = value * Math.pow(10, scaler);

        return new ValueContainer(new DoubleValue(scaledValue), valueType);
    }    
}
