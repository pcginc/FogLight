package com.ociweb.iot.grove.oled.oled2;

import com.ociweb.gl.api.transducer.StartupListenerTransducer;
import com.ociweb.iot.grove.oled.BinaryOLED;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.IODeviceTransducer;
import com.ociweb.iot.maker.image.*;
import com.ociweb.pronghorn.iot.schema.I2CCommandSchema;
import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ociweb.iot.grove.oled.oled2.OLEDCommands.*;

class OLED96x96Witer {
    private final int i2cAddress = 0x3c;
    private final FogCommandChannel ch;
    private final int interleavelimit;
    private DataOutputBlobWriter writer;
    private int interleaveCounter = 0;
    private boolean beginWithData = false;
    private boolean mustBegin = true;

    OLED96x96Witer(FogCommandChannel ch, int interleavelimit) {
        this.ch = ch;
        this.interleavelimit = interleavelimit;
        this.ch.ensureI2CWriting(10000 * 4, 64);
    }

    public void start() {
        this.beginWithData = false;
        this.mustBegin = true;
    }

    public void appendCommand(int elem) {
        beginBatchCheck();
        writer.write(COMMAND_MODE);
        writer.write(elem & 0xFF);
        interleaveCounter += 2;
        if (interleaveCounter >= interleavelimit) {
            endBatch();
        }
    }

    public void beginData() {
        beginBatchCheck();
        writer.write(DATA_MODE);
        interleaveCounter += 1;
        // don't interleave
    }

    public void appendData(int elem) {
        beginBatchCheck();
        writer.write(elem & 0xFF);
        interleaveCounter += 1;
        if (interleaveCounter >= interleavelimit) {
            endBatch();
            beginWithData = true;
        }
    }

    public void end() {
        if (interleaveCounter > 0) {
            endBatch();
        }
    }

    private void beginBatchCheck() {
        if (mustBegin) {
            this.mustBegin = false;
            this.interleaveCounter = 0;
            this.writer = ch.i2cCommandOpen(i2cAddress);
            if (beginWithData) {
                beginData();
            }
        }
    }

    private void endBatch() {
        ch.i2cCommandClose();
        ch.i2cFlushBatch();
        this.mustBegin = true;
    }
}

public class OLED96x96Transducer implements IODeviceTransducer, StartupListenerTransducer, FogBmpDisplayable {
    Logger logger = LoggerFactory.getLogger((BinaryOLED.class));

    private final FogCommandChannel ch;
    private final OLED96x96Witer bus;

    private final int[] data = new int[4608];
    private final int[] cmd = new int[32];

    private final int i2cAddress = 0x3c;
    private final int channelBatch = cmd.length * 2;

    public OLED96x96Transducer(FogCommandChannel ch) {
        this.ch = ch;
        this.bus = new OLED96x96Witer(ch, 500);
    }

    @Override
    public void startup() {
        bus.start();
        bus.appendCommand(DISPLAY_OFF);
        bus.appendCommand(SET_D_CLOCK);
        bus.appendCommand(0x50);
        bus.appendCommand(SET_ROW_ADDRESS);
        bus.appendCommand(SET_CONTRAST);
        bus.appendCommand(0x70);
        bus.appendCommand(REMAP_SGMT);
        bus.appendCommand(ENTIRE_DISPLAY_ON);
        bus.appendCommand(NORMAL_DISPLAY);
        bus.appendCommand(SET_EXT_VPP);
        bus.appendCommand(0x80);
        bus.appendCommand(SET_COMMON_SCAN_DIR);
        bus.appendCommand(SET_PHASE_LENGTH);
        bus.appendCommand(0x1F);
        bus.appendCommand(SET_VCOMH_VOLTAGE);
        bus.appendCommand(0x20);
        //injectInitScreen();
        bus.appendCommand(DISPLAY_ON);
        bus.end();
    }

    final int[] SeeedLogo = new int[] {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x60, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xC0, 0x06, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xC0, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x01, 0xC0, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x03, 0x80, 0x03, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x80, 0x03, 0x80,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x80, 0x03, 0xC0, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x07, 0x80, 0x01, 0xC0, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20,
                0x07, 0x80, 0x01, 0xE0, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x0F, 0x80, 0x01, 0xE0,
                0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x0F, 0x00, 0x01, 0xE0, 0x08, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x30, 0x0F, 0x00, 0x01, 0xE0, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30,
                0x0F, 0x00, 0x01, 0xE0, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x0F, 0x00, 0x01, 0xE0,
                0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x38, 0x0F, 0x00, 0x01, 0xE0, 0x18, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x38, 0x0F, 0x00, 0x01, 0xE0, 0x38, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x38,
                0x0F, 0x80, 0x01, 0xE0, 0x38, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3C, 0x0F, 0x80, 0x01, 0xE0,
                0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x0F, 0x80, 0x03, 0xE0, 0x78, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x1E, 0x07, 0x80, 0x03, 0xE0, 0xF8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1E,
                0x07, 0x80, 0x03, 0xE0, 0xF0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0x07, 0x80, 0x03, 0xC1,
                0xF0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F, 0x87, 0xC0, 0x07, 0xC1, 0xF0, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x0F, 0x83, 0xC0, 0x07, 0x83, 0xE0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0F,
                0xC3, 0xC0, 0x07, 0x87, 0xE0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0xE1, 0xE0, 0x07, 0x0F,
                0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xF0, 0xE0, 0x0F, 0x0F, 0x80, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0xF8, 0xF0, 0x0E, 0x1F, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                0xF8, 0x70, 0x1C, 0x3F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFC, 0x30, 0x18, 0x7E,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0x18, 0x30, 0xFC, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x1F, 0x88, 0x21, 0xF0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x0F, 0xC4, 0x47, 0xE0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xE0, 0x0F, 0x80,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF8, 0x3E, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x0E, 0xE0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x6C, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x02, 0x00, 0x06, 0x00, 0x00, 0x6C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x06,
                0x00, 0x00, 0x60, 0x00, 0x7E, 0x3F, 0x0F, 0xC3, 0xF0, 0xFA, 0x0F, 0xDF, 0xE1, 0x9F, 0xEC, 0x7E,
                0xE6, 0x73, 0x9C, 0xE7, 0x39, 0xCE, 0x1C, 0xDF, 0xE1, 0xB9, 0xEC, 0xE7, 0xE0, 0x61, 0xD8, 0x66,
                0x1B, 0x86, 0x1C, 0x06, 0x61, 0xB0, 0x6D, 0xC3, 0x7C, 0x7F, 0xFF, 0xFF, 0xFF, 0x06, 0x0F, 0x86,
                0x61, 0xB0, 0x6D, 0x83, 0x3E, 0x7F, 0xFF, 0xFF, 0xFF, 0x06, 0x07, 0xC6, 0x61, 0xB0, 0x6D, 0x83,
                0xC3, 0x61, 0x18, 0x46, 0x03, 0x86, 0x18, 0x66, 0x61, 0xB0, 0x6D, 0xC3, 0xFE, 0x7F, 0x9F, 0xE7,
                0xF9, 0xFE, 0x1F, 0xE6, 0x3F, 0x9F, 0xEC, 0xFE, 0x7E, 0x3F, 0x0F, 0xC3, 0xF0, 0xFA, 0x0F, 0xC6,
                0x3F, 0x9F, 0xEC, 0x7E, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7C, 0x00,
                0x00, 0x20, 0x82, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x44, 0x00, 0x00, 0x20, 0x82, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x6C, 0xF3, 0xCF, 0x70, 0x9E, 0x79, 0xE7, 0x80, 0x00, 0x00,
                0x00, 0x00, 0x7D, 0x9E, 0x68, 0x20, 0xB2, 0xC8, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00, 0x47, 0x9E,
                0x6F, 0x20, 0xB2, 0xF9, 0xE7, 0x80, 0x00, 0x00, 0x00, 0x00, 0x46, 0x9A, 0x61, 0x20, 0xB2, 0xCB,
                0x60, 0x80, 0x00, 0x00, 0x00, 0x00, 0x7C, 0xF3, 0xCF, 0x30, 0x9E, 0x79, 0xE7, 0x90, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7C, 0x02, 0x00, 0x00, 0x82, 0x60, 0x00, 0x00,
                0xF8, 0x00, 0x00, 0x40, 0x40, 0x02, 0x00, 0x00, 0x83, 0x60, 0x00, 0x00, 0x8C, 0x00, 0x00, 0x40,
                0x60, 0xB7, 0x79, 0xE7, 0x81, 0xC7, 0x92, 0x70, 0x89, 0xE7, 0x9E, 0x78, 0x7C, 0xE2, 0xC9, 0x2C,
                0x81, 0xCC, 0xD2, 0x40, 0xFB, 0x21, 0xB2, 0x48, 0x40, 0x62, 0xF9, 0x2C, 0x80, 0x8C, 0xD2, 0x40,
                0x8B, 0xE7, 0xB0, 0x48, 0x40, 0xE2, 0xC9, 0x2C, 0x80, 0x84, 0xD2, 0x40, 0x8B, 0x2D, 0x92, 0x48,
                0x7D, 0xB3, 0x79, 0x27, 0x80, 0x87, 0x9E, 0x40, 0x8D, 0xE7, 0x9E, 0x48, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    boolean sendData2(DataOutputBlobWriter writer, int data) {
        //System.out.print("D");
        writer.write(DATA_MODE);
        writer.write(data & 0xFF);
        return true;
    }

    boolean sendCommand2(DataOutputBlobWriter writer, int data) {
        //System.out.print("C");
        writer.write(COMMAND_MODE);
        writer.write(data & 0xFF);
        return true;
    }

    private boolean injectInitScreen2() {

        System.out.print("injectInitScreen2\n");
        setOrientation(OLEDOrientation.horizontal);

        int Row = 0, column_l = 0x00, column_h = 0x11;

        for(int i=0;i<(96*96/8);i++) //1152
        {
            if (!ch.i2cIsReady(1)) {
                System.out.print("\nI2C is not ready\n");
                return false;
            }

            int tmp = 0x00;
            for(int b = 0; b < 8; b++)
            {
                int bits = SeeedLogo[i];
                tmp |= ( ( bits >> ( 7 - b ) ) & 0x01 ) << b;
            }
            //tmp = 0xFF;

            DataOutputBlobWriter<I2CCommandSchema> writer = ch.i2cCommandOpen(i2cAddress);
            if (!sendCommand2(writer, 0xb0 + Row)) return false;
            if (!sendCommand2(writer, column_l)) return false;
            if (!sendCommand2(writer, column_h)) return false;
            if (!sendData2(writer, tmp)) return false;
            ch.i2cCommandClose();

            Row++;
            if(Row >= 12){ // 192
                Row = 0;
                column_l++;
                //logger.info("End Row {}", column_l);
                if(column_l >= 16){
                    column_l = 0x00;
                    column_h += 0x01;
                    //logger.info("End Col {}", column_h);
                }
            }
            ch.i2cFlushBatch();
        }
        return true;
    }

    public boolean clearDisplay() {
       // bus.start();
        System.out.print("clearDisplay\n");
        return injectInitScreen2();
       // bus.end();
    }

    public boolean setContrast(byte contrast) {
        cmd[0] = SET_CONTRAST;
        cmd[1] = contrast;
        return sendCommands(2);
    }

    public boolean setOrientation(OLEDOrientation orientation) {
        cmd[0] = REMAP_SGMT;
        cmd[1] = orientation.COMMAND;
        return sendCommands(2);
    }

    public boolean setScrollActivated(boolean activated) {
        cmd[0] = activated ? SeeedGrayOLED_Activate_Scroll_Cmd : SeeedGrayOLED_Dectivate_Scroll_Cmd;
        return sendCommands(1);
    }

    public boolean setHorizontalScrollProperties(OLEDScrollDirection direction, int startRow, int endRow, int startColumn, int endColumn, OLEDScrollSpeed speed)  {
        cmd[0] = direction.COMMAND;
        cmd[1] = 0x00;
        cmd[2] = startRow;
        cmd[3] = speed.COMMAND;
        cmd[4] = endRow;
        cmd[5] = startColumn+8;
        cmd[6] = endColumn+8;
        cmd[7] = 0x00;
        return sendCommands(8);
    }

    public boolean invertDisplay() {
        cmd[0] = SeeedGrayOLED_Inverse_Display_Cmd;
        return sendCommands(1);
    }

    public boolean normalizeDisplay() {
        cmd[0] = SeeedGrayOLED_Normal_Display_Cmd;
        return sendCommands(1);
    }

    @Override
    public FogBitmapLayout newBmpLayout() {
        FogBitmapLayout bmpLayout = new FogBitmapLayout(FogColorSpace.gray);
        bmpLayout.setComponentDepth((byte) 4);
        bmpLayout.setWidth(COL_COUNT);
        bmpLayout.setHeight(ROW_COUNT);
        return bmpLayout;
    }

    @Override
    public FogBitmap newEmptyBmp() {
        return new FogBitmap(newBmpLayout());
    }

    @Override
    public FogPixelScanner newPreferredBmpScanner(FogBitmap bmp) { return new FogPixelProgressiveScanner(bmp); }

    @Override
    public boolean display(FogPixelScanner scanner) {
    /*    int Row = 0, column_l = 0x00, column_h = 0x11;

        setOrientation(OLEDOrientation.horizontal);
        for (int i=0;i<bytes;i++)
        {
            cmd[0] = 0xb0 + Row;
            cmd[1] = column_l;
            cmd[2] = column_h;
            sendCommands(3);

            int bits = bitmap.getComponent(0, 0, 0);
            int tmp = 0x00;
            for(int b = 0; b < 8; b++) {
                tmp |= ((bits>>(7-b))&0x01)<<b;
            }
            sendData(tmp);
            Row++;
            if (Row >= 12) {
                Row = 0;
                column_l++;
                if (column_l >= 16) {
                    column_l = 0x00;
                    column_h += 0x01;
                }
            }
        }*/
        return true;
    }

    private boolean sendCommands(int length) {
        if (!ch.i2cIsReady( length * 2)) {
            logger.trace("I2C is not ready");
            return false;
        }
        DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(i2cAddress);
        for (int i = 0; i < length; i++) {
            i2cPayloadWriter.write(COMMAND_MODE);
            i2cPayloadWriter.write(cmd[i] & 0xFF);
        }
        ch.i2cCommandClose();
        ch.i2cFlushBatch();
        return true;
    }

    private boolean sendData(int length){
        if (!ch.i2cIsReady( ( (length + 1) * 2 / channelBatch) + 1) ) {
            return false;
        }
        //call the helper method to recursively send batches
        return sendData(0, channelBatch, length);
    }

    private boolean sendData(int start, int length, int finalTargetIndex){
        DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(i2cAddress);
        i2cPayloadWriter.write(DATA_MODE);
        int i;
        for (i = start; i < Math.min(start + length - 1, finalTargetIndex); i++){
            i2cPayloadWriter.write(data[i] & 0xFF);
        }
        ch.i2cCommandClose();
        ch.i2cFlushBatch();
        if (i == finalTargetIndex){
            return true;
        }
        //calls itself recursively until we reach finalTargetIndex
        return sendData(i, channelBatch, finalTargetIndex);
    }
}
