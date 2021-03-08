package de.phoenixstudios.stromzaehler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {
    static byte[] CompressByteArray(byte[] ByteArray){
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ByteArray.length);
        byte[] CompressedByteArray;
        try{
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(ByteArray);
            gzip.close();
            CompressedByteArray = bos.toByteArray();
            bos.close();
        }catch(IOException error){
            CompressedByteArray = new byte[]{};
        }

        return CompressedByteArray;
    }

    static byte[] DecompressByteArray(byte[] CompressedByteArray){
        // decompress gzip-compressed byte-array
        ByteArrayInputStream bis = new ByteArrayInputStream(CompressedByteArray);

        // recover uncompressed size from last four bytes
        int b4 = CompressedByteArray[CompressedByteArray.length-4] & 0xff;
        int b3 = CompressedByteArray[CompressedByteArray.length-3] & 0xff;
        int b2 = CompressedByteArray[CompressedByteArray.length-2] & 0xff;
        int b1 = CompressedByteArray[CompressedByteArray.length-1] & 0xff;
        int UncompressedSize = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;

        byte[] readBuffer = new byte[UncompressedSize];
        byte[] UncompressedByteArray;
        try {
            GZIPInputStream gis = new GZIPInputStream(bis);
            int readBytes = gis.read(readBuffer,0, readBuffer.length);
            UncompressedByteArray = Arrays.copyOf(readBuffer, readBytes);
            gis.close();
            bis.close();
        }catch(IOException error){
            UncompressedByteArray = new byte[]{0};
        }

        return UncompressedByteArray;
    }
}
