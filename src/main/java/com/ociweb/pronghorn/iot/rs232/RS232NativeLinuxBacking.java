package com.ociweb.pronghorn.iot.rs232;

import com.ociweb.pronghorn.iot.jni.NativeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * JNI wrapper for RS232 serial operations on a UNIX system.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public final class RS232NativeLinuxBacking implements RS232NativeBacking {

    private static final Logger logger = LoggerFactory.getLogger(RS232NativeLinuxBacking.class);

    static {
        try {
            if (new File("rs232.so").exists()) {
                logger.info("rs232.so detected in working directory. Loading this file instead of the packaged one.");
                System.load(new File(".").getCanonicalPath() + File.separator + "rs232.so");
            } else {
                // TODO: Hard-coded for linux systems.
                // TODO: Uses external native utils stuff for convenience. May consider alternative?
                String arch = System.getProperty("os.arch");
                if (arch.contains("arm")) {
                    NativeUtils.loadLibraryFromJar("/jni/arm-Linux/rs232.so");
                } else {
                    NativeUtils.loadLibraryFromJar("/jni/i386-Linux/rs232.so");
                }
            }

        } catch (UnsatisfiedLinkError | IOException e) {
            e.printStackTrace();
        }
    }

    // Native methods.
    public native int open(String port, int baud);
    public native int close(int fd);
    public native int write(int fd, byte[] message);
    public native int getBytesInOutputBuffer(int fd);
    public native int getAvailableBytes(int fd);
    public native byte[] readBlocking(int fd, int size);
    public native byte[] read(int fd, int size);
    public native int writeFrom(int fd, byte[] rawBuffer, int start, int maxLength);
    public native int writeFromTwo(int fd, byte[] rawBuffer1, int start1, int maxLength1,
                                           byte[] rawBuffer2, int start2, int maxLength2);
    public native int readInto(int fd, byte[] rawBuffer, int start, int maxLength);
    public native int readIntoTwo(int fd, byte[] rawBuffer1, int start1, int maxLength1,
                                          byte[] rawBuffer2, int start2, int maxLength2);
}
