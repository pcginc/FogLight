package com.ociweb.iot.grove;

import com.ociweb.iot.hardware.I2CConnection;
import com.ociweb.iot.hardware.IODevice;
import com.ociweb.pronghorn.iot.schema.GroveResponseSchema;
import com.ociweb.pronghorn.pipe.Pipe;

public class NunchuckTwig implements IODevice{

	public byte addr = 0x52;
	public NunchuckTwig() {
	}

	@Override
	public I2CConnection getI2CConnection(){
		byte[] NUNCHUCK_READCMD = {0x00};
	    byte[] NUNCHUCK_SETUP = {0x40, 0x00};
	    byte NUNCHUCK_ADDR = 0x52;
	    byte NUNCHUCK_BYTESTOREAD = 6;
	    byte NUNCHUCK_REGISTER = 0;
	    return new I2CConnection(this, NUNCHUCK_ADDR, NUNCHUCK_READCMD, NUNCHUCK_BYTESTOREAD, NUNCHUCK_REGISTER, NUNCHUCK_SETUP);
	}
	
	
	public int[] interpretData(byte[] backing, int position, int length, int mask){
		assert(length==6) : "Non-Nunchuck data passed into the NunchuckTwig class";
		int[] temp = {0,0,0,0,0,0};
		for (int i = 0; i < 5; i++) {
			temp[i] = (backing[(position+i)&mask] ^ 0x17) + 0x17;
		}
		temp[5] = backing[(position+5)&mask];
		return temp;
	}
	
	@Override
	public boolean isValid(byte[] backing, int position, int length, int mask){
		return true; //TODO: fill in valid testing
	}

	@Override
	public boolean isInput() {
		return true;
	}

	@Override
	public boolean isOutput() {
		return false;
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
	public boolean isGrove() {
		return false;
	}
    
	@Override
    public int response() {       
       return 10;      
    }
}
