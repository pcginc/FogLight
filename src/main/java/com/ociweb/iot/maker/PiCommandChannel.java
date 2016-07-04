package com.ociweb.iot.maker;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ociweb.pronghorn.iot.schema.TrafficOrderSchema;
import com.ociweb.pronghorn.iot.schema.GroveRequestSchema;
import com.ociweb.pronghorn.iot.schema.I2CCommandSchema;
import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;
import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.pipe.PipeWriter;
import com.ociweb.pronghorn.pipe.RawDataSchema;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class PiCommandChannel extends CommandChannel{

	private Pipe<GroveRequestSchema> output;
	private Pipe<I2CCommandSchema> i2cOutput;
	private Pipe<TrafficOrderSchema> goPipe;
	private AtomicBoolean aBool = new AtomicBoolean(false);    
	private DataOutputBlobWriter<RawDataSchema> i2cWriter;  
	private int runningI2CCommandCount;
	private byte channelIdx;
	

	public PiCommandChannel(GraphManager gm, Pipe<GroveRequestSchema> output, Pipe<I2CCommandSchema> i2cOutput, Pipe<TrafficOrderSchema> goPipe, byte commandIndex) { 
			//TODO:Was this protected for security reasons? I'm making it in hardware now.
		super(gm, output, i2cOutput, goPipe);
		this.output = output;
		this.i2cOutput = i2cOutput;  
		this.goPipe = goPipe;
		this.channelIdx = 1;//TODO: should be different for i2c vs adout. 1 is i2c, 0 is digital

	}


	public boolean digitalBlock(int connector, int duration) { //TODO: Make this work for Pi I2C

		assert(enterBlockOk()) : "Concurrent usage error, ensure this never called concurrently";
		try {            

		    boolean msg;
			if (PipeWriter.tryWriteFragment(i2cOutput, I2CCommandSchema.MSG_BLOCK_10)) {

				PipeWriter.writeInt(i2cOutput, I2CCommandSchema.MSG_BLOCK_10_FIELD_ADDRESS_12, connector);
				PipeWriter.writeLong(i2cOutput, I2CCommandSchema.MSG_BLOCK_10_FIELD_DURATION_13, duration);

				PipeWriter.publishWrites(i2cOutput);

				msg = true;
			} else {
			    msg = false;
			}
		
            if(msg && PipeWriter.tryWriteFragment(goPipe, TrafficOrderSchema.MSG_GO_10)) { 
    
                PipeWriter.writeByte(goPipe, TrafficOrderSchema.MSG_GO_10_FIELD_PIPEIDX_11, channelIdx);
                PipeWriter.writeByte(goPipe, TrafficOrderSchema.MSG_GO_10_FIELD_COUNT_12, (byte) 1);
                System.out.println("CommandChannel sends digitalWrite i2c go");
    
                PipeWriter.publishWrites(goPipe);
                return true;
            } else {
                return false; ///TODO: error this is very bad.
            }
        
		} finally {
		    assert(exitBlockOk()) : "Concurrent usage error, ensure this never called concurrently";      
		}
        
	}
	
	
	//Build templates like this once that can be populated and sent without redefining the part that is always the same.
	private final byte[] digitalMessageTemplate = {0x04, 0x05, 0x01, 0x02, -1, -1, 0x00};

	public boolean digitalSetValue(int connector, int value) {

		assert(enterBlockOk()) : "Concurrent usage error, ensure this never called concurrently";
		try {
			boolean msg;
			if (PipeWriter.tryWriteFragment(i2cOutput, I2CCommandSchema.MSG_COMMAND_7)) { 

				digitalMessageTemplate[4] = (byte)connector;
				digitalMessageTemplate[5] = (byte)value;
				
				PipeWriter.writeInt(i2cOutput, I2CCommandSchema.MSG_BLOCK_10_FIELD_ADDRESS_12, digitalMessageTemplate[0]); //hack for now
                
				PipeWriter.writeBytes(i2cOutput, I2CCommandSchema.MSG_COMMAND_7_FIELD_BYTEARRAY_2, digitalMessageTemplate);
								
				
				System.out.println("CommandChannel sends digitalWrite i2c message");
				PipeWriter.publishWrites(i2cOutput);
				msg = true;
			}else{
				msg = false;
			}
				
			if(msg && PipeWriter.tryWriteFragment(goPipe, TrafficOrderSchema.MSG_GO_10)) { 

					PipeWriter.writeByte(goPipe, TrafficOrderSchema.MSG_GO_10_FIELD_PIPEIDX_11, channelIdx);
					PipeWriter.writeByte(goPipe, TrafficOrderSchema.MSG_GO_10_FIELD_COUNT_12, (byte) 1);
					System.out.println("CommandChannel sends digitalWrite i2c go");

					PipeWriter.publishWrites(goPipe);
				return true;
			} else {
				return false;
			}

		} finally {
			assert(exitBlockOk()) : "Concurrent usage error, ensure this never called concurrently";      
		}
	}


	public boolean analogSetValue(int connector, int value) {

		assert(enterBlockOk()) : "Concurrent usage error, ensure this never called concurrently";
		try {            
			if (PipeWriter.tryWriteFragment(output, GroveRequestSchema.MSG_ANALOGSET_140)) {

				PipeWriter.writeInt(output, GroveRequestSchema.MSG_ANALOGSET_140_FIELD_CONNECTOR_141, connector);
				PipeWriter.writeInt(output, GroveRequestSchema.MSG_ANALOGSET_140_FIELD_VALUE_142, value);

				PipeWriter.publishWrites(output);

				return true;
			} else {
				return false;
			}

		} finally {
			assert(exitBlockOk()) : "Concurrent usage error, ensure this never called concurrently";      
		}
	}

	public boolean i2cIsReady() {
		if (null==i2cWriter) {
			i2cWriter = new DataOutputBlobWriter(i2cOutput);//hack for now until we can get this into the scheduler TODO: nathan follow up.
		}


		//TODO: need to set this as a constant driven from the known i2c devices and the final methods
		int maxCommands = 16;

		return PipeWriter.hasRoomForWrite(output) && PipeWriter.hasRoomForFragmentOfSize(i2cOutput, Pipe.sizeOf(i2cOutput, I2CCommandSchema.MSG_COMMAND_7)*maxCommands);

	}

	public DataOutputBlobWriter<RawDataSchema> i2cCommandOpen(int targetAddress) {       
		assert(enterBlockOk()) : "Concurrent usage error, ensure this never called concurrently";
		try {
			if (PipeWriter.tryWriteFragment(i2cOutput, I2CCommandSchema.MSG_COMMAND_7)) {
			    PipeWriter.writeInt(i2cOutput, I2CCommandSchema.MSG_COMMAND_7_FIELD_ADDRESS_12, targetAddress);
				DataOutputBlobWriter.openField(i2cWriter);
				return i2cWriter;
			} else {
				return null;//can not write try again later.
			}
		} finally {
			assert(exitBlockOk()) : "Concurrent usage error, ensure this never called concurrently";      
		}
	}

	public void i2cCommandClose() {  
		assert(enterBlockOk()) : "Concurrent usage error, ensure this never called concurrently";
		try {
			runningI2CCommandCount++;
			DataOutputBlobWriter.closeHighLevelField(i2cWriter, I2CCommandSchema.MSG_COMMAND_7_FIELD_BYTEARRAY_2);
			PipeWriter.publishWrites(i2cOutput);
		} finally {
			assert(exitBlockOk()) : "Concurrent usage error, ensure this never called concurrently";      
		}
	}

	public boolean i2cFlushBatch() {        
//		assert(enterBlockOk()) : "Concurrent usage error, ensure this never called concurrently";
//		try {
//			if (PipeWriter.tryWriteFragment(output, GroveRequestSchema.MSG_I2CWRITE_400) ) { 
//				PipeWriter.writeInt(output, GroveRequestSchema.MSG_I2CWRITE_400_FIELD_MESSAGECOUNT_410, runningI2CCommandCount);
//				PipeWriter.publishWrites(output);
//				runningI2CCommandCount = 0;                
//				return true;
//			}
//			return false;            
//		} finally {
//			assert(exitBlockOk()) : "Concurrent usage error, ensure this never called concurrently";      
//		}
	    return false;
	}


	private boolean enterBlockOk() {
		return aBool.compareAndSet(false, true);
	}

	private boolean exitBlockOk() {
		return aBool.compareAndSet(true, false);
	}


}
