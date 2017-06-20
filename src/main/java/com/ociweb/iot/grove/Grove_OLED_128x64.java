package com.ociweb.iot.grove;

import com.ociweb.iot.hardware.I2CConnection;
import com.ociweb.iot.hardware.IODevice;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.pronghorn.iot.schema.I2CCommandSchema;
import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;

import static com.ociweb.iot.grove.Grove_OLED_128x64_Constants.*;
import static com.ociweb.iot.grove.Grove_OLED_128x64_Constants.Direction.*;
public class Grove_OLED_128x64 implements IODevice{

	//this enum is here because makers will need this and they would already have the Grove_OLED_128 class imported
	public static enum ScrollSpeed{
		Scroll_2Frames(0x07),
		Scroll_3Frames(0x04),
		Scroll_4Frames(0x05),
		Scroll_5Frames(0x00),
		Scroll_25Frames(0x06),
		Scroll_64Frames(0x01),
		Scroll_128Frames(0x02),
		Scroll_256Frames(0x03);

		final int command;
		private ScrollSpeed(int command){ 
			this.command = command;
		};

	};

	public static boolean isStarted = false;
	public static boolean init(FogCommandChannel target){
		int commands[] = {PUT_DISPLAY_TO_SLEEP, WAKE_DISPLAY, TURN_OFF_INVERSE_DISPLAY, DEACTIVATE_SCROLL};
		return sendCommands(target, commands);
	}

	public static boolean sendData(FogCommandChannel ch, int b ){
		if (!ch.i2cIsReady()){
			return false;
		}
		DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
		i2cPayloadWriter.write(Grove_OLED_128x64_Constants.DATA_MODE);
		i2cPayloadWriter.write(b);
		ch.i2cCommandClose();
		return true;
	}

	public static boolean setContrast(FogCommandChannel ch, int contrast){
		int[] commands = {SET_CONTRAST_CONTROL, contrast & 0xFF};
		return sendCommands(ch, commands);
	}

	public static boolean setPageMode(FogCommandChannel ch){
		int[] commands = {SET_MEMORY, 0x02};
		return sendCommands(ch, commands);
	}

	public static boolean setHorizontalMode(FogCommandChannel ch){
		int[] commands = {SET_MEMORY, 0x00};
		return sendCommands(ch, commands);
	}

	public static boolean setVerticalMode(FogCommandChannel ch){
		int[] commands = {SET_MEMORY, 0x01};
		return sendCommands(ch,commands);
	}

	public static boolean turnOnInverseDisplay(FogCommandChannel ch){
		return sendCommands(ch, TURN_ON_INVERSE_DISPLAY);
	}

	public static boolean turnOffInverseDisplay(FogCommandChannel ch){
		return sendCommands(ch, TURN_OFF_INVERSE_DISPLAY);
	}

	public static boolean activateScroll(FogCommandChannel ch){
		return sendCommands(ch, ACTIVATE_SCROLL);
	}

	public static boolean deactivateScroll(FogCommandChannel ch){
		return sendCommands(ch, DEACTIVATE_SCROLL);
	}	
	
	public static boolean setMultiplexRatio(FogCommandChannel ch, int mux_ratio){
		int [] commands = {SET_MUX_RATIO, mux_ratio & 0x3F};
		return sendCommands(ch, commands);
	}
	
	public static boolean setClockDivRatioAndOscFreq(FogCommandChannel ch, int clock_div_ratio, int osc_freq){
		int [] commands = {SET_CLOCK_DIV_RATIO, (clock_div_ratio & 0x0F) | (osc_freq << 4 & 0xF0)};
		return sendCommands(ch,commands);
	}
	public static boolean setVerticalDisplayOffset(FogCommandChannel ch, int offset){
		int [] commands = {SET_DISPLAY_OFFSET,(offset & 0x3F)};
		return sendCommands(ch, commands);
	}

	public static boolean setUpRightContinuousHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, int endPage){
		return setUpContinuousHorizontalScroll(ch, speed, startPage, endPage, Right);
	}
	public static boolean setUpLeftContinuousHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, int endPage){
		return setUpContinuousHorizontalScroll(ch, speed, startPage, endPage, Left);
	}
	
	public static boolean setUpRightContinuousVerticalHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, 
			int endPage, int offset){
		return setUpContinuousVerticalHorizontalScroll(ch, speed, startPage, endPage, offset, Vertical_Right);
	}
	
	public static boolean setUpLeftContinuousVerticalHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, 
			int endPage, int offset){
		return setUpContinuousVerticalHorizontalScroll(ch, speed, startPage, endPage, offset, Vertical_Left);
	}
	
	private static boolean setUpContinuousHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, int endPage, 
			Direction orientation){
		int dir_command = 0;
		switch (orientation){
		case Right:
			dir_command = SET_RIGHT_HOR_SCROLL;
			break;
		case Left:
			dir_command = SET_LEFT_HOR_SCROLL;
			break;
		}
		int [] commands = {
				dir_command, 
				0x00,  //dummy byte as required
				startPage & 0x07, 
				speed.command, 
				endPage & 07, 
				0xFF, //dummy byte as required
				0x00};//dummy byte as required

		sendCommands(ch,commands);
		return false;
	}

	private static boolean setUpContinuousVerticalHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, int endPage,
			int offset, Direction orientation){
		int dir_command = 0;
		switch (orientation){
		case Vertical_Left:
			dir_command =  SET_VER_AND_RIGHT_HOR_SCROLL;
			break;
		case Vertical_Right:
			dir_command =  SET_VER_AND_LEFT_HOR_SCROLL;
			break;	
		}
		
		int [] commands = {
				dir_command, 
				0x00,  //dummy byte as required
				startPage & 0x07, 
				speed.command, 
				endPage & 0x07, 
				offset & 0x1F};
		
		return true;
		
	}
	/**
	 * NOTE: this method leaves the display in horizontal mode
	 * @param ch
	 * @param map
	 * @return true if the i2c commands were succesfully sent, false otherwise
	 */
	public static boolean drawBitmap(FogCommandChannel ch, int[] map){
		if (!setHorizontalMode(ch)){
			return false;
		}
		for (int bitmap: map){

			if (!sendData(ch, (byte) bitmap)){
				return false;
			}
			ch.i2cFlushBatch();
		}

		return true;
	}

	public static boolean printChar(FogCommandChannel ch, char c){
		if (c > 127 || c < 32){
			//'c' has no defined font for Grove_OLED_128x64");
			return false;
		}
		for (int i = 0; i < 8; i++){
			if (sendData(ch, (byte)BASIC_FONT[c-32][i])){	//successful send is expected to be more common
				ch.i2cFlushBatch();
			}
			else {
				ch.i2cFlushBatch();
				return false;
			}
		}
		return true;
	}

	public static boolean printString(FogCommandChannel ch, String s){
		for (char c: s.toCharArray()){
			if (printChar(ch,c)){
			} 
			else {
				return false;
			}
		}
		return true;
	}


	public static boolean setTextRowCol(FogCommandChannel ch, int row, int col){ //only works in Page Mode
		//bit-mask because x and y can only be within a certain range (0-7)

		//TODO: avoid three seperate if-statements by ANDing them in the condtional, is there a better way?
		if (sendCommand(ch, (ROW_START_ADDRESS_PAGE_MODE + (row & 0x07))) 
				&& sendCommand(ch,  (LOWER_COL_START_ADDRESS_PAGE_MODE + (8*col & 0x0F)))
				&& sendCommand(ch,  (HIGHER_COL_START_ADDRESS_PAGE_MODE) + ((8*col >> 4) & 0x0F)))
		{
			ch.i2cFlushBatch();
			return true;
		}
		ch.i2cFlushBatch();
		return false;
	}

	public static boolean setPageModeAndTextRowCol(FogCommandChannel ch, int row, int col){
		return setPageMode(ch) && setTextRowCol(ch,row,col);

	}

	public static boolean clear(FogCommandChannel ch){
		if (setPageMode(ch)){
			
		} else {
			return false;
		}

		for (int row = 0; row < 8; row++){
			setTextRowCol(ch, row, 0);
			if (printString(ch,"                ")){
				//16 spaces will clear up an entire row
			}
			else {
				return false;
			}
		}
		return true;
	}

	public static boolean cleanClear(FogCommandChannel ch){
		if (sendCommand(ch, PUT_DISPLAY_TO_SLEEP)
				&& clear(ch) 
				&& sendCommand(ch, WAKE_DISPLAY))
		{
			ch.i2cFlushBatch();
			return true;
		}
		ch.i2cFlushBatch();		
		return false;
	}

	public static boolean sendCommand(FogCommandChannel ch, int b){
		if (!ch.i2cIsReady()){
			return false;
		}
		DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
		i2cPayloadWriter.write(Grove_OLED_128x64_Constants.COMMAND_MODE);
		i2cPayloadWriter.write(b);
		ch.i2cCommandClose();

		return true;
	}

	public static boolean sendCommands(FogCommandChannel ch, int... commands){
		for (int com: commands)
			if (sendCommand(ch, com)){
				ch.i2cFlushBatch();
			} else {
				ch.i2cFlushBatch();
				return false;
			}

		return true;
	}

	@Deprecated 
	private static boolean writeByteSequence(FogCommandChannel ch, byte[] seq){
		if(!ch.i2cIsReady()){
			return false;
		}
		DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
		i2cPayloadWriter.write(seq);
		ch.i2cCommandClose();
		return true;
	}

	//Overloading the function to automatically mask ints and use their least significant 8-bits as our bytes to send
	//Ideally, for best performance, we should send byte array and not int array to avoid this extra function call
	private static boolean writeByteSequence(FogCommandChannel ch, int[] seq){
		byte[] byteSeq = new byte[seq.length];
		int counter = 0;
		for (int i: seq){
			byteSeq[counter] = (byte)(i & 0xFF); //this mask turns anything but the smallest 8 bits to 0
			counter++;
		}
		return writeByteSequence(ch, byteSeq);
	}

	@Override
	public int response() {
		return 20;
	}

	@Override
	public int scanDelay() {
		return 0;
	}

	@Override
	public boolean isInput() {
		return false;
	}

	@Override
	public boolean isOutput() {
		return true;
	}

	@Override
	public boolean isPWM() {
		return false;
	}

	@Override
	public int range() {
		return 0;
	}

	@Override
	public I2CConnection getI2CConnection() {
		byte[] LED_READCMD = {};
		byte[] LED_SETUP = {};
		byte LED_ADDR = 0x04;
		byte LED_BYTESTOREAD = 0;
		byte LED_REGISTER = 0;
		return new I2CConnection(this, LED_ADDR, LED_READCMD, LED_BYTESTOREAD, LED_REGISTER, LED_SETUP);
	}

	@Override
	public boolean isValid(byte[] backing, int position, int length, int mask) {
		return true;
	}

	@Override
	public int pinsUsed() {
		return 1;
	}

}
