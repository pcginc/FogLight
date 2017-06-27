package com.ociweb.iot.grove;

import com.ociweb.iot.hardware.I2CConnection;

import com.ociweb.iot.hardware.IODevice;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.pronghorn.iot.schema.I2CCommandSchema;
import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;

import static com.ociweb.iot.grove.Grove_OLED_128x64_Constants.*;
import static com.ociweb.iot.grove.Grove_OLED_128x64_Constants.Direction.*;
import static com.ociweb.iot.grove.Grove_OLED_128x64_Constants.Orientation.*;
import com.ociweb.iot.grove.display.OLED_128x64;

/**
 * Singleton utility class that communicates with the i2c Grove OLED 128x64 display, includes basic functionality such as printing
 * bitmap or CharSequence.
 * @author Ray Lo, Nathan Tippy
 *
 */
public class Grove_OLED_128x64 implements IODevice{
	
	private static Grove_OLED_128x64 instance = null;
	
	/**
	 * Private constructor for singleton design pattern.
	 */
	private Grove_OLED_128x64(){
	}

	/**
	 * Returns the singleton instance of Grove_OLED_128x64, lazily initializes the instance if it's still.
	 * @return singleton instance
	 */
	public static Grove_OLED_128x64 getInstace(){
		if (instance == null){
			instance = new Grove_OLED_128x64();
		}
		return instance;
	}

	/**
	 * Dynamically allocates an instance of {@link OLED_128x64}
	 * @param ch {@link FogCommandChannel} reference to be held onto by the new {@link OLED_128x64}
	 * @return the new instance of {@link OLED_128x64} created.
	 */
	public static OLED_128x64 newObj(FogCommandChannel ch){
		return new OLED_128x64(ch);
	}

	public static boolean isStarted = false;

	/**
	 * Flashes the display screen off and then on and ensures that the inverse_display and scrolling functions
	 *  are turned off. The display is left in the Horizontal mode afterwards.
	 * @param target is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection.
	 * @return true if the commands were sent, returns false if any single command was not sent.
	 */
	public static boolean init(FogCommandChannel target, int[] output){
		output[0] = PUT_DISPLAY_TO_SLEEP;
		output[1] = WAKE_DISPLAY;
		output[2] = TURN_OFF_INVERSE_DISPLAY;
		output[3] = DEACTIVATE_SCROLL;
		output[4] = SET_MEMORY;
		output[5] = 0x00;
		output[6] = SET_DISPLAY_OFFSET;
		output[7] = 0x00;
		return sendCommands(target, output, 0, 8);
	}

	/**
	 * Send an array of data
	 * Implemented by calling {@link #sendData(FogCommandChannel, int[], int, int, int)}, which recursively calls itself
	 * exactly 'm' times, where 'm' is the number of batches requires to send the data array specified by the start and length.
	 * @param ch
	 * @param data
	 * @param start
	 * @param length
	 * @return true if the i2c bus is ready, false otherwise.
	 */
	private static boolean sendData(FogCommandChannel ch, int[] data, int start, int length){
		if (!ch.i2cIsReady()){
			return false;
		}
		//call the helper method to recursively send batches
		return sendData(ch,data,start,BATCH_SIZE, start+length);
	}

	private static boolean sendData(FogCommandChannel ch, int[] data, int start, int length, int finalTargetIndex){
		DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
		i2cPayloadWriter.write(DATA_MODE);
		int i;
		for (i = start; i < Math.min(start + length, finalTargetIndex); i++){
			i2cPayloadWriter.write(data[i]);
		}
		ch.i2cCommandClose();
		ch.i2cFlushBatch();
		if (i == finalTargetIndex){
			return true;
		}
		return sendData(ch, data, i + 1, BATCH_SIZE, finalTargetIndex); //calls itself recursively until we reach finalTargetIndex
	}
/*
	private static boolean iterativeSendData(FogCommandChannel ch, int[] data, int start, int length){
		
		 * 
		DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
		int counter = 1;
		i2cPayloadWriter.write(DATA_MODE);

		//TODO: Fix for loop to not check batch size every iteration
		for (int i = start; i < start+length; i ++){
			if (counter < BATCH_SIZE){	
				i2cPayloadWriter.write(data[i]);
				counter = counter + 1;
			}
			else {
				ch.i2cCommandClose();
				ch.i2cFlushBatch();

				i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
				i2cPayloadWriter.write(DATA_MODE);
				i2cPayloadWriter.write(data[i]);
				counter = 2;
			}
		}
		if (counter > 0){
			ch.i2cCommandClose();
			ch.i2cFlushBatch();
		}
		 */
		/*
		final int num_batches = length / BATCH_SIZE;
		DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
		i2cPayloadWriter.write(DATA_MODE);

		//iterate through all the batches we need to send

		for (int i = 0; i < num_batches; i++){
			//iterate through all the indiviusal bytes in each batch
			for (int j = start + (i * BATCH_SIZE); j < start + ( (i + 1) * BATCH_SIZE); j++){
				i2cPayloadWriter.write(data[j]);
			}
			//flush the batch once a batch is full
			ch.i2cCommandClose();
			ch.i2cFlushBatch();
			i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
			i2cPayloadWriter.write(DATA_MODE);
		}

		//send the last fraction of batch worth of remaining bytes.
		int start_last_batch = start + (num_batches * BATCH_SIZE);

		for (int j = start_last_batch; j < start + length; j++){
			i2cPayloadWriter.write(data[j]);
		}
		ch.i2cCommandClose();
		ch.i2cFlushBatch();

		return true;
		}
		 */


	/**
	 * Sends a "data" identifier byte followed by the user-supplied byte over the i2c.
	 * @param ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection to this OLED.
	 * @param data is the array of data to be sent in the form of an integer array (the L.S. 8 bits of each int are used.)
	 * @return true if the command byte and the supplied byte were succesfully sent, false otherwise.
	 */
	public static boolean sendData(FogCommandChannel ch, int[] data ){
		return sendData(ch, data,0, data.length);
	}

	/**
	 * Sets the contrast (olso refered to as brightness) of the display.
	 * @param ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection to this OLED.
	 * @param contrast is a value ranging from 0 to 255. A bit-mask is enforced so that only the lowest 8 bits of the supplied integer will matter.
	 * @return true if the command byte and the contrast byte were sent, false otherwise.
	 */
	public static boolean setContrast(FogCommandChannel ch, int contrast, int[] output){
		output[0] = SET_CONTRAST_CONTROL;
		output[1] = contrast & 0xFF;
		return sendCommands(ch, output, 0, 2);
	}

	/**
	 * Sets the display in page mode, necessary for {@link #setTextRowCol(FogCommandChannel,int,int, int[])}, {@link #printCharSequence(FogCommandChannel, CharSequence, int[])} . 
	 * @param ch is the ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection to this OLED.
	 * @return true if all three bytes needed were sent, false otherwise.
	 * @see <a href = "https://github.com/SeeedDocument/Grove_OLED_Display_0.96/raw/master/resource/SSD1308_1.0.pdf">SSD1308.pdf</a>
	 *for information on page mode.
	 */
	public static boolean setPageMode(FogCommandChannel ch, int[] output){		
		output[0] = SET_MEMORY;
		output[1] = 0x02;
		return sendCommands(ch, output, 0, 2);
	}

	/**
	 * Sets the display in horizontal mode, necessary for {@link #drawBitmap(FogCommandChannel, int[], int[])} and {@link OLED_128x64#displayImage(int[][])}
	 * Note that both {@link #drawBitmap(FogCommandChannel, int[], int[])} and {@link OLED_128x64#displayImage(int[][])} already automatically set the display in
	 * horizontal mode.
	 * @param ch is the ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection to this OLED.
	 * @return true if all three bytes needed were sent, false otherwise.
	 * @see <a href = "https://github.com/SeeedDocument/Grove_OLED_Display_0.96/raw/master/resource/SSD1308_1.0.pdf">SSD1308.pdf</a>
	 * for information on horizontal mode. 
	 */

	public static boolean setHorizontalMode(FogCommandChannel ch, int[] output){
		output[0] = SET_MEMORY;
		output[1] = 0x00;
		return sendCommands(ch, output, 0, 2);
	}

	/**
	 * Sets the display in vertical mode.
	 * @param ch is the ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection to this OLED.
	 * @return true if all three bytes needed were sent, false otherwise. 
	 * @see <a href = "https://github.com/SeeedDocument/Grove_OLED_Display_0.96/raw/master/resource/SSD1308_1.0.pdf">SSD1308.pdf</a>
	 * for information on vertical mode.
	 */

	public static boolean setVerticalMode(FogCommandChannel ch, int[] output){
		output[0] = SET_MEMORY;
		output[1] = 0x01;
		return sendCommands(ch, output, 0, 2);
	}

	/**
	 * Turns on the inverse feature which switches all black pixels with white pixels and vice versa.
	 * @param ch is the ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection of this OLED.
	 * @return true if all two necessary bytes were sent, false otherwise.
	 */
	public static boolean turnOnInverseDisplay(FogCommandChannel ch){
		return sendCommand(ch, TURN_ON_INVERSE_DISPLAY);
	}

	/**
	 * Turns off the inverse feature which switches all black pixels with white pixels and vice versa.
	 * @param ch is the ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection of this OLED.
	 * @return true if all two necessary bytes were sent, false otherwise.
	 */
	public static boolean turnOffInverseDisplay(FogCommandChannel ch){
		return sendCommand(ch, TURN_OFF_INVERSE_DISPLAY);
	}

	/**
	 * Activates the scroll feature.
	 * NOTE: One of the four set-up methods ({@link #setUpRightContinuousVerticalHorizontalScroll(FogCommandChannel, ScrollSpeed, int, int, int, int[])},
	 * {@link #setUpLeftContinuousVerticalHorizontalScroll(FogCommandChannel, ScrollSpeed, int, int, int, int[])}, {@link #setUpRightContinuousHorizontalScroll(FogCommandChannel, ScrollSpeed, int, int, int[])},
	 * and {@link #setUpLeftContinuousVerticalHorizontalScroll(FogCommandChannel, ScrollSpeed, int, int, int, int[])}) needs to be invoked first.
	 * @param ch is the ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection of this OLED.
	 * @return true if all two necessary bytes were sent, false otherwise.
	 * @see <a href = "https://github.com/SeeedDocument/Grove_OLED_Display_0.96/raw/master/resource/SSD1308_1.0.pdf">SSD1308.pdf</a>
	 * for information on scrolling.
	 */
	public static boolean activateScroll(FogCommandChannel ch){
		return sendCommand(ch, ACTIVATE_SCROLL);
	}

	/**
	 * Deactivates the scroll feature.
	 * @param ch is the ch is the {@link com.ociweb.iot.maker.FogCommandChannel} in charge of the i2c connection of this OLED.
	 * @return true if all two necessary bytes were sent, false otherwise.
	 * @see <a href = "https://github.com/SeeedDocument/Grove_OLED_Display_0.96/raw/master/resource/SSD1308_1.0.pdf">SSD1308.pdf</a>
	 * for information on scrolling.
	 */
	public static boolean deactivateScroll(FogCommandChannel ch){
		return sendCommand(ch, DEACTIVATE_SCROLL);
	}	


	public static boolean setMultiplexRatio(FogCommandChannel ch, int mux_ratio, int[] output){
		output[0] = SET_MUX_RATIO;
		output[1] = mux_ratio & 0x3F;
		return sendCommands(ch, output, 0,2);
	}

	public static boolean setClockDivRatioAndOscFreq(FogCommandChannel ch, int clock_div_ratio, int osc_freq, int [] output){
		output[0] = SET_CLOCK_DIV_RATIO;
		output[1] = (clock_div_ratio & 0x0F) | (osc_freq << 4 & 0xF0);
		return sendCommands(ch, output, 0,2);

	}
	public static boolean setVerticalOffset(FogCommandChannel ch, int offset, int[] output){
		output[0] = SET_DISPLAY_OFFSET;
		output[1] = offset & 0x3F;
		return sendCommands(ch, output, 0,2);
	}

	public static boolean setUpRightContinuousHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, int endPage, int[] output){
		return setUpContinuousHorizontalScroll(ch, speed, startPage, endPage, Right, output);
	}
	public static boolean setUpLeftContinuousHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, int endPage, int[] output){
		return setUpContinuousHorizontalScroll(ch, speed, startPage, endPage, Left, output);
	}

	public static boolean setUpRightContinuousVerticalHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, 
			int endPage, int offset,  int[] output){
		return setUpContinuousVerticalHorizontalScroll(ch, speed, startPage, endPage, offset, Vertical_Right, output);
	}

	public static boolean setUpLeftContinuousVerticalHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, 
			int endPage, int offset,  int[] output){
		return setUpContinuousVerticalHorizontalScroll(ch, speed, startPage, endPage, offset, Vertical_Left, output);
	}

	
	private static boolean setUpContinuousHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, int endPage, 
			Direction dir, int[] output){	
		generateHorizontalScrollComamnds(speed,startPage,endPage,dir,output);
		return sendCommands(ch, output, 0,7);
	}
	
	private static void generateHorizontalScrollComamnds(ScrollSpeed speed, int startPage, int endPage, 
			Direction dir, int[] output){

		output[0] = dir == Right? SET_RIGHT_HOR_SCROLL:SET_LEFT_HOR_SCROLL;
		output[1] = 0x00; //dummy byte as required
		output[2] = startPage & 0x07;
		output[3] =speed.command;
		output[4] =endPage & 0x07;
		output[5] = 0xFF; // dummy byte as required
		output[6] = 0x00; // dummy byte as required		
	}
	
	private static boolean setUpContinuousVerticalHorizontalScroll(FogCommandChannel ch, ScrollSpeed speed, int startPage, int endPage,
			int offset, Orientation dir, int[] output){
		generateVerticalHorizontalScrollComamnds(speed,startPage,endPage,offset,dir,output);
		return sendCommands(ch,output,0,6);

	}

	private static void generateVerticalHorizontalScrollComamnds(ScrollSpeed speed, int startPage, int endPage, 
			int offset, Orientation ori, int[] output){
		
		output[0] = ori == Vertical_Right? SET_VER_AND_RIGHT_HOR_SCROLL:SET_VER_AND_LEFT_HOR_SCROLL;
		output[1] = 0x00; //dummy byte as required
		output[2] = startPage & 0x07;
		output[3] =speed.command;
		output[4] =endPage & 0x07;
		output[5] = offset & 0x1F;
	}
	
	/**
	 * NOTE: this method leaves the display in horizontal mode
	 * @param ch
	 * @param map
	 * @return true if the i2c commands were succesfully sent, false otherwise
	 */
	public static boolean drawBitmapInHorizontalMode(FogCommandChannel ch, int[] map, int[] data_output){
		if (!setHorizontalMode(ch,data_output)){
			return false;
		}
		return sendData(ch,map);
	}

	public static boolean drawBitmap(FogCommandChannel ch, int[] map, int[] cmd_output){
		return drawBitmapInPageMode(ch, map, cmd_output);
	}

	/**
	 * NOTE: drawing in page mode instead of horizontal mode sends 16 extra bytes per reflash compared to drawing
	 * in horizontal mode as we need to reset textRowCol everytime we reach a new page. It may be preferable to use
	 * drawing in page mode however, as it eliminates the need to switch between page mode and horizontal mode when doing
	 * both drawing and CharSequence printing.
	 * @param ch
	 * @param map
	 * @return true
	 */
	public static boolean drawBitmapInPageMode(FogCommandChannel ch, int[] map, int[] cmd_output){
		for (int page = 0; page <8; page++){
			if (! setTextRowCol(ch,page,0, cmd_output)){
				return false;
			}
			int startingPoint = page*128;

			if (!sendData(ch, map, startingPoint, 128)){
				return false;
			}

		}
		return true;
	}

	public static boolean encodeChar(char c, int[] output){
		return encodeChar(c, output, 0);
	}

	public static boolean encodeChar(char c, int[] output, int start){
		if (c > 127 || c < 32){
			//'c' has no defined font for Grove_OLED_128x64");
			return false;
		}
		int counter = 0;
		for (int i = start; i < start + 8; i++){
			output[i] = BASIC_FONT[c-32][counter];
			counter ++;
		}
		return true;
	}

	public static boolean encodeCharSequence(CharSequence s, int[] output){
		return encodeCharSequence(s, output, 0, s.length());
	}

	public static boolean encodeCharSequence(CharSequence s,  int[] output, int start, int CharSequence_length){
		for (int i = start; i < start + CharSequence_length; i++){
			if (encodeChar(s.charAt(i), output, i*8)){	
			} else {
				return false;
			}
		}
		return true;
	}

	public static boolean printCharSequenceAt(FogCommandChannel ch, CharSequence s, int[] data_output, int row, int col, int[] cmd_output){
		return setTextRowCol(ch,row,col, cmd_output) && printCharSequence(ch,s,data_output);
	}

	public static boolean printCharSequence(FogCommandChannel ch, CharSequence s, int[] output){
		encodeCharSequence(s, output);
		return sendData(ch, output, 0, s.length()*8);
	}


	public static boolean setTextRowCol(FogCommandChannel ch, int row, int col, int[] output){ //only works in Page Mode
		//bit-mask because x and y can only be within a certain range (0-7)

		output[0] = ROW_START_ADDRESS_PAGE_MODE + (row & 0x07);
		output[1] = LOWER_COL_START_ADDRESS_PAGE_MODE + (8*col & 0x0F);
		output[2] = HIGHER_COL_START_ADDRESS_PAGE_MODE + ((8*col >> 4) & 0x0F);
		//TODO: avoid three seperate if-statements by ANDing them in the condtional, is there a better way?

		return sendCommands(ch, output, 0, 3);
	}

	public static boolean setPageModeAndTextRowCol(FogCommandChannel ch, int row, int col, int[] output){
		return setPageMode(ch, output) && setTextRowCol(ch,row,col, output);

	}

	public static boolean setDisplayStartLine(FogCommandChannel ch, int startLine,int[] output){
		output[0] =  DISPLAY_START_LINE;
		output[1] = startLine & 0x3F;
		return sendCommands(ch, output,0,2);
	}

	public static boolean remapSegment(FogCommandChannel ch, boolean isRemapped){
		int remap_cmd = MAP_ADDRESS_0_TO_SEG0;
		if (isRemapped){
			remap_cmd = MAP_ADDRESS_127_TO_SEG0;
		}
		return sendCommand(ch, remap_cmd);
	}

	/**
	 * Note: leaves the display in page mode
	 * @param ch
	 * @return true 
	 */
	public static boolean clear(FogCommandChannel ch, int[] output){
		if (setPageMode(ch, output)){

		} else {
			return false;
		}

		for (int row = 0; row < 8; row++){
			setTextRowCol(ch, row, 0, output);
			if (sendData(ch, EMPTY_ROW)){
			} else {
				return false;
			}
		}
		return true;
	}

	public static boolean cleanClear(FogCommandChannel ch, int[] output){
		if (sendCommand(ch, PUT_DISPLAY_TO_SLEEP)
				&& clear(ch, output) 
				&& sendCommand(ch, WAKE_DISPLAY))
		{
			ch.i2cFlushBatch();
			return true;
		}
		ch.i2cFlushBatch();		
		return false;
	}

	private static boolean sendCommand(FogCommandChannel ch, int b){
		if (!ch.i2cIsReady()){
			return false;
		}
		DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
		i2cPayloadWriter.write(Grove_OLED_128x64_Constants.COMMAND_MODE);
		i2cPayloadWriter.write(b);
		ch.i2cCommandClose();

		return true;
	}


	/**
	 * Unliked send data, sendCommands makes the assumption that the call is not sending more than one batch worth of commands
	 *Each command  involves two bytes. So if the caller is trying to send a command array of size 5, they are really sending
	 *10 bytes.
	 * @param ch
	 * @param commands
	 * @param start
	 * @param length
	 * @return
	 */
	private static boolean sendCommands(FogCommandChannel ch, int[] commands, int start, int length){
		if (!ch.i2cIsReady()){
			return false;
		}

		DataOutputBlobWriter<I2CCommandSchema> i2cPayloadWriter = ch.i2cCommandOpen(OLEDADDRESS);
		
		assert(length*2 <= BATCH_SIZE);
		for (int i = start; i < start + length; i++){
		
				i2cPayloadWriter.write(COMMAND_MODE);
				i2cPayloadWriter.write(commands[i]);
		
		}
		ch.i2cCommandClose();
		ch.i2cFlushBatch();
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

	@Deprecated
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