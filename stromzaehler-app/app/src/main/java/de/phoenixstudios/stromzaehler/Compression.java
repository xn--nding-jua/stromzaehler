package de.phoenixstudios.stromzaehler;

import android.util.Base64;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {
    static byte[] CompressByteArray(byte[] ByteArray, boolean Base64EncodedStream){
        byte[] UncompressedByteArray;
        byte[] CompressedByteArray;

        if (Base64EncodedStream) {
            UncompressedByteArray = Base64.encode(ByteArray, Base64.DEFAULT);
        }else{
            UncompressedByteArray = ByteArray;
        }

        try{
            ByteArrayOutputStream bos = new ByteArrayOutputStream(UncompressedByteArray.length);
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(UncompressedByteArray);
            gzip.close();
            CompressedByteArray = bos.toByteArray();
            bos.close();
        }catch(IOException error){
            CompressedByteArray = new byte[]{};
        }

        return CompressedByteArray;
    }

    static byte[] DecompressByteArray(byte[] CompressedByteArray, boolean Base64EncodedStream){
        // recover uncompressed size from last four bytes
        int b4 = (int)CompressedByteArray[CompressedByteArray.length-4] & 0xff;
        int b3 = (int)CompressedByteArray[CompressedByteArray.length-3] & 0xff;
        int b2 = (int)CompressedByteArray[CompressedByteArray.length-2] & 0xff;
        int b1 = (int)CompressedByteArray[CompressedByteArray.length-1] & 0xff;
        int UncompressedSize = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;

        byte[] readBuffer = new byte[UncompressedSize];
        byte[] ByteArray;
        byte[] UncompressedByteArray;

        try {
            // decompress gzip-compressed byte-array
            ByteArrayInputStream bis = new ByteArrayInputStream(CompressedByteArray);
            GzipCompressorInputStream gis = new GzipCompressorInputStream(bis, false);
            int readBytes = gis.read(readBuffer);
            ByteArray = Arrays.copyOf(readBuffer, readBytes);
            gis.close();
            bis.close();

            if (Base64EncodedStream){
                UncompressedByteArray = Base64.decode(ByteArray, Base64.DEFAULT);
            }else{
                UncompressedByteArray = ByteArray;
            }
        }catch(IOException error){
            UncompressedByteArray = new byte[]{0};
            System.out.println(error.toString());
        }

/*
        try {
            // decompress gzip-compressed byte-array
            ByteArrayInputStream bis = new ByteArrayInputStream(CompressedByteArray);

            GZIPInputStream gis = new GZIPInputStream(bis);
            int readBytes = gis.read(readBuffer,0, readBuffer.length);
            System.out.println("readBytes"+readBytes);
            UncompressedByteArray = Arrays.copyOf(readBuffer, readBytes);
            System.out.println("UncompressedByteArray"+UncompressedByteArray.length);
            gis.close();
            bis.close();
        }catch(IOException error){
            UncompressedByteArray = new byte[]{0};
            System.out.println(error.toString());
        }
*/
        return UncompressedByteArray;
    }
}
