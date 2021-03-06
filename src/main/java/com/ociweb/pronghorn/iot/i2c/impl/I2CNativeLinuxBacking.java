package com.ociweb.pronghorn.iot.i2c.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.iot.i2c.I2CBacking;

/**
 * Native I2C backing implementation for a GrovePi using ioctl.h.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public class I2CNativeLinuxBacking implements I2CBacking {

    private static final byte[] EMPTY = new byte[]{};

    private static final Logger logger = LoggerFactory.getLogger(I2CNativeLinuxBacking.class);

    //Highest value for an I2C address. TODO: Is this the right value?
    private static final int I2C_MAX_ADDRESSES = 0x77;

    //Native C library.
    private static final UnixIoctlLib c = UnixIoctlLib.instance;

    private boolean configured = false;

    //Native I2C file handle.
    private int i2cFile = -1;

    //Most recent address we've handled in order to restrict duplicat IOCTL calls.
    private byte lastAddress = (byte) -127;

    /**
     * Configures I2C to communicate with the specified byte address.
     *
     * @param address Byte address of the I2C device to configure for.
     */
    private boolean ensureI2CDevice(byte address) {
        if (lastAddress == address) {
            return true;
        }
        return checkNewAddress(address);
    }

    private boolean checkNewAddress(byte address) {
        if (address > 0 && address <= I2C_MAX_ADDRESSES) {
            /**
             * IOCTL will return -1 if it fails for any reason.
             */
            if (c.ioctl(i2cFile, UnixIoctlLib.I2C_SLAVE_FORCE, address) >= 0) {
                lastAddress = address;
                logger.debug("IOCTL configured for I2C device at {}", address);
                return true;
            } else {
                throw new RuntimeException(
                        "Could not configure IOCTL for I2C device at 0x" + Integer.toHexString(address));
            }
        } else {
            throw new RuntimeException(
                    "I2C Device 0x" + Integer.toHexString(address) + " is outside of the possible I2C address range.");
        }
    }

    @Override
    public I2CNativeLinuxBacking configure(byte bus) {
        if (configured) {
            throw new IllegalStateException("Bus is already configured.");
        } else {
            configured = true;
        }

        String device = "/dev/i2c-" + bus;

        //Get the I2C file.
        i2cFile = c.open(device, UnixIoctlLib.O_RDWR);

        //Make sure it worked....
        if (i2cFile < 0) {
            logger.debug("unable to open {}", device);
            throw new RuntimeException("Could not open " + device);
        } else {
            logger.debug("Successfully opened {}", device);
        }

        //Close the file when the application shuts down.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (i2cFile >= 0) {
                    c.close(i2cFile);
                }
            }
        });

        return this;
    }

    @Override
    public byte[] read(byte address, byte[] target, int bufferSize) {
        if (!configured) {
            throw new IllegalStateException("Bus is not configured yet.");
        }

        //Check if we need to load the address into memory.
        if (ensureI2CDevice(address)) {
            int result = c.read(i2cFile, target, bufferSize);
            if (result != bufferSize) {//did not read so flag this as an error
                target[0] = -2;
            }
            return target;
        } else {
            return EMPTY;
        }
    }

    @Override
    public boolean write(byte address, byte[] message, int length) {
        if (!configured) {
            throw new IllegalStateException("Bus is not configured yet.");
        }

        assert (length >= 0);
        //System.out.println("write to address:"+ Integer.toHexString(address));

        //Check if we need to load the address into memory.
        ensureI2CDevice(address); //throws on failure
        return -1 != c.write(i2cFile, message, length);

    }
}