package com.ociweb.iot.hardware;

import static com.ociweb.pronghorn.pipe.PipeWriter.publishWrites;
import static com.ociweb.pronghorn.pipe.PipeWriter.tryWriteFragment;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.iot.hardware.HardConnection.ConnectionType;
import com.ociweb.pronghorn.iot.i2c.impl.I2CNativeLinuxBacking;
import com.ociweb.pronghorn.pipe.DataInputBlobReader;
import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;
import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.pipe.RawDataSchema;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class AnalogDigitalInputStage extends PronghornStage{
	//TODO: Change the Schema
	private final Pipe<RawDataSchema> toListener;
	private final Pipe<RawDataSchema> goPipe;

	private DataOutputBlobWriter<RawDataSchema> writeListener;
	private DataInputBlobReader<RawDataSchema> readGo;

	private Hardware hardware;

	private static final Logger logger = LoggerFactory.getLogger(JFFIStage.class);

	public AnalogDigitalInputStage(GraphManager graphManager, Pipe<RawDataSchema> toListener, Pipe<RawDataSchema> goPipe, Hardware hardware) {
		super(graphManager, toListener, goPipe); 

		////////
		//STORE OTHER FIELDS THAT WILL BE REQUIRED IN STARTUP
		////////

		this.toListener = toListener;
		this.goPipe = goPipe;

		this.writeListener = new DataOutputBlobWriter<RawDataSchema>(toListener);
		this.readGo = new DataInputBlobReader<RawDataSchema>(goPipe);
		this.hardware = hardware;


	}
	public void startup() {
		for (int i = 0; i < hardware.digitalOutputs.length; i++) {
			if(hardware.digitalOutputs[i].type.equals(ConnectionType.Direct)) hardware.configurePinsForDigitalInput(hardware.digitalOutputs[i].connection);
		}
		for (int i = 0; i < hardware.analogInputs.length; i++) {
			if(hardware.analogInputs[i].type.equals(ConnectionType.Direct)) hardware.configurePinsForAnalogInput(hardware.analogInputs[i].connection);
		}

	}
	@Override
	public void run() {
		for (int i = 0; i < this.hardware.digitalOutputs.length; i++) {
			if(this.hardware.digitalOutputs[i].type.equals(ConnectionType.Direct)){
				if (tryWriteFragment(toListener, RawDataSchema.MSG_CHUNKEDSTREAM_1)) { //TODO: Do we want to open and close pipe writer for every poll?
					DataOutputBlobWriter.openField(writeListener);
					try {
						writeListener.write(hardware.digitalRead(hardware.digitalOutputs[i].connection)); //TODO: Use some other Schema to pass connection
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}

					DataOutputBlobWriter.closeHighLevelField(writeListener, RawDataSchema.MSG_CHUNKEDSTREAM_1_FIELD_BYTEARRAY_2);
					publishWrites(toListener);
				}else{
					System.out.println("unable to write fragment");
				}
			}	
		}
		for (int i = 0; i < this.hardware.analogInputs.length; i++) {
			if(this.hardware.analogInputs[i].type.equals(ConnectionType.Direct)){
				if (tryWriteFragment(toListener, RawDataSchema.MSG_CHUNKEDSTREAM_1)) { //TODO: Do we want to open and close pipe writer for every poll?
					DataOutputBlobWriter.openField(writeListener);
					try {
						writeListener.write(hardware.analogRead(hardware.analogInputs[i].connection)); //TODO: Use some other Schema
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}

					DataOutputBlobWriter.closeHighLevelField(writeListener, RawDataSchema.MSG_CHUNKEDSTREAM_1_FIELD_BYTEARRAY_2);
					publishWrites(toListener);
				}else{
					System.out.println("unable to write fragment");
				}
			}	
		}
		//TODO: add support for listeners you only ping once in a while.

	}

}