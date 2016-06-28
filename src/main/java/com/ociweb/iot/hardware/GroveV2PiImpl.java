package com.ociweb.iot.hardware;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.iot.hardware.impl.grovepi.GrovePiConstants;
import com.ociweb.pronghorn.TrafficCopStage;
import com.ociweb.pronghorn.iot.schema.AcknowledgeSchema;
import com.ociweb.pronghorn.iot.schema.GoSchema;
import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.pipe.PipeConfig;
import com.ociweb.pronghorn.pipe.RawDataSchema;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;


public class GroveV2PiImpl extends Hardware {


	private final I2CJFFIStage i2cJFFIStage;
	private final AnalogDigitalInputStage adInputStage;
	private final AnalogDigitalOutputStage adOutputStage;
	private final TrafficCopStage trafficCopStage;
	
	private final Pipe<RawDataSchema> ccToAdOut;
	private final Pipe<RawDataSchema> ccToTraffic;
	private final Pipe<RawDataSchema> ccToI2C;
	private final Pipe<GoSchema>[] goPipes; //AdOut, I2C, AdIn
	private final Pipe<AcknowledgeSchema>[] ackPipes; //AdOut, I2C
	private final Pipe<RawDataSchema> I2CToListener;
	private final Pipe<RawDataSchema> adInToListener;

	private final int SLEEP_RATE_NS = 20000000;

	private List<byte[]> readData;

	private static final Logger logger = LoggerFactory.getLogger(GrovePiImpl.class);

	public GroveV2PiImpl(GraphManager gm) {
		PipeConfig<RawDataSchema> ccToAdOutConfig = new PipeConfig<RawDataSchema>(RawDataSchema.instance, 64, 1024);
		PipeConfig<GoSchema> ccToTrafficConfig = new PipeConfig<GoSchema>(GoSchema.instance, 64, 1024);
		PipeConfig<RawDataSchema> ccToI2CConfig = new PipeConfig<RawDataSchema>(RawDataSchema.instance, 64, 1024);
		PipeConfig<GoSchema> goPipesConfig = new PipeConfig<GoSchema>(GoSchema.instance, 64, 1024);
		PipeConfig<AcknowledgeSchema> ackPipesConfig = new PipeConfig<AcknowledgeSchema>(AcknowledgeSchema.instance, 64, 1024);;
		PipeConfig<RawDataSchema> I2CToListenerConfig = new PipeConfig<RawDataSchema>(RawDataSchema.instance, 64, 1024);;
		PipeConfig<RawDataSchema> adInToListenerConfig = new PipeConfig<RawDataSchema>(RawDataSchema.instance, 64, 1024);;
		
		Pipe<RawDataSchema> ccToAdOut = new Pipe<RawDataSchema>(ccToAdOutConfig);
		Pipe<GoSchema> ccToTraffic = new Pipe<GoSchema>(ccToTrafficConfig);
		Pipe<GoSchema>[] ccToTrafficJoiner = (Pipe<GoSchema>[]) Array.newInstance(ccToTrafficConfig.getClass(), 1);
        ccToTrafficJoiner[0] = ccToTraffic;
		Pipe<RawDataSchema> ccToI2C = new Pipe<RawDataSchema>(ccToI2CConfig);
		Pipe<GoSchema> goPipe = new Pipe<GoSchema>(goPipesConfig);
		int length = 3;
		Pipe<GoSchema>[] goPipes = (Pipe<GoSchema>[]) Array.newInstance(goPipe.getClass(), length);
		Pipe<AcknowledgeSchema> ackPipe = new Pipe<AcknowledgeSchema>(ackPipesConfig);
		length = 2; 
		Pipe<AcknowledgeSchema>[] ackPipes = (Pipe<AcknowledgeSchema>[]) Array.newInstance(ackPipe.getClass(), length);
		Pipe<RawDataSchema> I2CToListener = new Pipe<RawDataSchema>(I2CToListenerConfig);
		Pipe<RawDataSchema> adInToListener = new Pipe<RawDataSchema>(adInToListenerConfig);
		
		this.i2cJFFIStage = new I2CJFFIStage(gm, goPipes[1], ccToI2C, ackPipes[1], I2CToListener, this);
		this.adInputStage = new AnalogDigitalInputStage(gm, adInToListener, goPipes[2], this);
		this.adOutputStage = new AnalogDigitalOutputStage(gm, ccToAdOut, goPipes[0], ackPipes[0], this);
		this.trafficCopStage = new TrafficCopStage(gm, ccToTrafficJoiner, ackPipes, goPipes);
	}

	public GroveV2PiImpl(boolean publishTime, boolean configI2C, HardConnection[] encoderInputs,
			HardConnection[] digitalInputs, HardConnection[] digitalOutputs, HardConnection[] pwmOutputs, HardConnection[] analogInputs, GraphManager gm) {
		super(publishTime, configI2C, encoderInputs, digitalInputs, digitalOutputs, pwmOutputs, analogInputs);
		PipeConfig<RawDataSchema> ccToAdOutConfig = new PipeConfig<RawDataSchema>(RawDataSchema.instance, 64, 1024);
		PipeConfig<GoSchema> ccToTrafficConfig = new PipeConfig<GoSchema>(GoSchema.instance, 64, 1024);
		PipeConfig<RawDataSchema> ccToI2CConfig = new PipeConfig<RawDataSchema>(RawDataSchema.instance, 64, 1024);
		PipeConfig<GoSchema> goPipesConfig = new PipeConfig<GoSchema>(GoSchema.instance, 64, 1024);
		PipeConfig<AcknowledgeSchema> ackPipesConfig = new PipeConfig<AcknowledgeSchema>(AcknowledgeSchema.instance, 64, 1024);;
		PipeConfig<RawDataSchema> I2CToListenerConfig = new PipeConfig<RawDataSchema>(RawDataSchema.instance, 64, 1024);;
		PipeConfig<RawDataSchema> adInToListenerConfig = new PipeConfig<RawDataSchema>(RawDataSchema.instance, 64, 1024);;
		
		Pipe<RawDataSchema> ccToAdOut = new Pipe<RawDataSchema>(ccToAdOutConfig);
		Pipe<GoSchema> ccToTraffic = new Pipe<GoSchema>(ccToTrafficConfig);
		Pipe<GoSchema>[] ccToTrafficJoiner = (Pipe<GoSchema>[]) Array.newInstance(ccToTrafficConfig.getClass(), 1);
        ccToTrafficJoiner[0] = ccToTraffic;
		Pipe<RawDataSchema> ccToI2C = new Pipe<RawDataSchema>(ccToI2CConfig);
		Pipe<GoSchema> goPipe = new Pipe<GoSchema>(goPipesConfig);
		int length = 3;
		Pipe<GoSchema>[] goPipes = (Pipe<GoSchema>[]) Array.newInstance(goPipe.getClass(), length);
		Pipe<AcknowledgeSchema> ackPipe = new Pipe<AcknowledgeSchema>(ackPipesConfig);
		length = 2; 
		Pipe<AcknowledgeSchema>[] ackPipes = (Pipe<AcknowledgeSchema>[]) Array.newInstance(ackPipe.getClass(), length);
		Pipe<RawDataSchema> I2CToListener = new Pipe<RawDataSchema>(I2CToListenerConfig);
		Pipe<RawDataSchema> adInToListener = new Pipe<RawDataSchema>(adInToListenerConfig);
		
		this.i2cJFFIStage = new I2CJFFIStage(gm, goPipes[1], ccToI2C, ackPipes[1], I2CToListener, this);
		this.adInputStage = new AnalogDigitalInputStage(gm, adInToListener, goPipes[2], this);
		this.adOutputStage = new AnalogDigitalOutputStage(gm, ccToAdOut, goPipes[0], ackPipes[0], this);
		this.trafficCopStage = new TrafficCopStage(gm, ccToTrafficJoiner, ackPipes, goPipes);

	}


	public void coldSetup() {
		//usedLines = buildUsedLines();
		//GrovePiGPIO.ensureAllLinuxDevices(usedLines);
	}

	public void cleanup() {
		//GrovePiGPIO.removeAllLinuxDevices(usedLines);
	}

	public void configurePinsForDigitalInput(byte connection) {
		//GrovePiGPIO.configDigitalInput(connection); //readBit
		System.out.println("No config needed");
	}

	public void configurePinsForDigitalOutput(byte connection) {
		//GrovePiGPIO.configDigitalOutput(connection);//writeBit
		System.out.println("No config needed");
	}

	public void configurePinsForAnalogInput(byte connection) {
		System.out.println("No config needed");
	}

	@Override
	public void configurePinsForAnalogOutput(byte connection) {
		System.out.println("No config needed");

	}

	public void configurePinsForI2C() {
		//			GrovePiGPIO.configI2C();
		System.out.println("No config needed1");
	}

	public void i2cDataIn() {
		//			GrovePiGPIO.configI2CDataIn();
		System.out.println("Pure Java I2c not currently supported on Pi");
	}

	public void i2cDataOut() {
		//GrovePiGPIO.configI2CDataOut();
		System.out.println("Pure Java I2c not currently supported on Pi");
	}

	public void i2cClockIn() {
		//GrovePiGPIO.configI2CClockIn();
		System.out.println("Pure Java I2c not currently supported on Pi");
	}

	public void i2cClockOut() {
		//GrovePiGPIO.configI2CClockOut();
		System.out.println("Pure Java I2c not currently supported on Pi");
	}

	public boolean i2cReadAck() {
		//			boolean ack;
		//			int voltage = analogRead(GrovePiConstants.DATA_RAW_VOLTAGE);
		//			ack = voltage<GrovePiConstants.HIGH_LINE_VOLTAGE_MARK;
		//			if (!ack) {    
		//				System.err.println("ack value "+ack+" "+Integer.toBinaryString(voltage));
		//			}
		//			return ack;
		System.out.println("Pure Java I2c not currently supported on Pi");
		return true;
	}

	@Override
	public boolean i2cReadClockBool() {
		//return i2cReadClock() == 0; //TODO: 0 or 1 for HI?
		System.out.println("Pure Java I2c not currently supported on Pi");
		return false;
	}

	public void beginPinConfiguration() {
		//super.beginPinConfiguration();        
	}

	public void endPinConfiguration() {
		//super.endPinConfiguration();
	}

	public int digitalRead(int connector) { 
		System.out.println("GPIO not currently supported on Pi");
		return 0;
	}

	//TODO: Since there's no ADC built into the Pi, we can only read HI or LO.
	public int analogRead(int connector) {
		throw new UnsupportedOperationException();
	}


	@Override
	public void analogWrite(int connector, int value) {
		throw new UnsupportedOperationException();
	}

	//Now using the JFFI stage
	public void digitalWrite(int connector, int value) {
		System.out.println("GPIO not currently supported on Pi");
	}

	//TODO: Is it right to config them as outputs before writing?
	public void i2cSetClockLow() {
		//			GrovePiGPIO.configDigitalOutput(GrovePiPinManager.I2C_CLOCK);
		//			GrovePiPinManager.writeValue(GrovePiPinManager.I2C_CLOCK, GrovePiPinManager.I2C_LOW, GrovePiGPIO.gpioLinuxPins);
		System.out.println("Pure Java I2c not currently supported on Pi");
	}

	public void i2cSetClockHigh() {
		//			GrovePiGPIO.configDigitalOutput(GrovePiPinManager.I2C_CLOCK);
		//			GrovePiPinManager.writeValue(GrovePiPinManager.I2C_CLOCK, GrovePiPinManager.I2C_HIGH, GrovePiGPIO.gpioLinuxPins);
		System.out.println("Pure Java I2c not currently supported on Pi");
	}

	public void i2cSetDataLow() {
		//			GrovePiGPIO.configDigitalOutput(GrovePiPinManager.I2C_DATA);
		//			GrovePiPinManager.writeValue(GrovePiPinManager.I2C_DATA, GrovePiPinManager.I2C_LOW, GrovePiGPIO.gpioLinuxPins);
		System.out.println("Pure Java I2c not currently supported on Pi");
	}

	public void i2cSetDataHigh() {
		//			GrovePiGPIO.configDigitalOutput(GrovePiPinManager.I2C_DATA);
		//			GrovePiPinManager.writeValue(GrovePiPinManager.I2C_DATA, GrovePiPinManager.I2C_HIGH, GrovePiGPIO.gpioLinuxPins);
		System.out.println("Pure Java I2c not currently supported on Pi");
	}

	public int i2cReadData() {
		System.out.println("Pure Java I2c not currently supported on Pi");
		//return digitalRead(GrovePiPinManager.I2C_DATA);
		return -1;
	}
	public boolean i2cReadDataBool() { return false; } //TODO:

	public int i2cReadClock() {
		System.out.println("Pure Java I2c not currently supported on Pi");
		//return digitalRead(GrovePiPinManager.I2C_CLOCK);
		return -1;
	}

	static void findDup(HardConnection[] base, int baseLimit, HardConnection[] items, boolean mapAnalogs) {
		int i = items.length;
		while (--i>=0) {
			int j = baseLimit;
			while (--j>=0) {
				//TODO: Will probably have undesired side effects.
				//                if (mapAnalogs ? base[j].connection == GrovePiConstants.ANALOG_CONNECTOR_TO_PIN[items[i].connection] :  base[j]==items[i]) {
				if (mapAnalogs ? false : base[j] == items[i]) {
					throw new UnsupportedOperationException("Connector "+items[i]+" is assigned more than once.");
				}
			}
		}     
	}


	//		public HardConnection[] buildUsedLines() {
	//
	//			
	//			HardConnection[] result = new HardConnection[digitalInputs.length+
	//			                                         encoderInputs.length+
	//			                                         digitalOutputs.length+
	//			                                         pwmOutputs.length+
	//			                                         analogInputs.length+
	//			                                         (configI2C?2:0)];
	//
	//			int pos = 0;
	//			System.arraycopy(digitalInputs, 0, result, pos, digitalInputs.length);
	//			pos+=digitalInputs.length;
	//
	//			if (0!=(encoderInputs.length&0x1)) {
	//				throw new UnsupportedOperationException("Rotary encoder requires two neighboring digital inputs.");
	//			}
	//			findDup(result,pos,encoderInputs, false);
	//			System.arraycopy(encoderInputs, 0, result, pos, encoderInputs.length);
	//			pos+=encoderInputs.length;
	//
	//			findDup(result,pos,digitalOutputs, false);
	//			System.arraycopy(digitalOutputs, 0, result, pos, digitalOutputs.length);
	//			pos+=digitalOutputs.length;
	//
	//			findDup(result,pos,pwmOutputs, false);
	//			System.arraycopy(pwmOutputs, 0, result, pos, pwmOutputs.length);
	//			pos+=pwmOutputs.length;
	//
	//			if (configI2C) {
	//				findDup(result,pos,GrovePiConstants.i2cPins, false);
	//				System.arraycopy(GrovePiConstants.i2cPins, 0, result, pos, GrovePiConstants.i2cPins.length);
	//				pos+=GrovePiConstants.i2cPins.length;
	//			}
	//
	//			return result;
	//		}

	public HardConnection[] buildUsedLines() {

		HardConnection[] result = new HardConnection[digitalInputs.length+
		                                             multiBitInputs.length+
		                                             digitalOutputs.length+
		                                             pwmOutputs.length+
		                                             analogInputs.length+
		                                             (configI2C?2:0)];

		int pos = 0;
		System.arraycopy(digitalInputs, 0, result, pos, digitalInputs.length);
		pos+=digitalInputs.length;

		if (0!=(multiBitInputs.length&0x1)) {
			throw new UnsupportedOperationException("Rotary encoder requires two neighboring digital inputs.");
		}
		findDup(result,pos,multiBitInputs, false);
		System.arraycopy(multiBitInputs, 0, result, pos, multiBitInputs.length);
		pos+=multiBitInputs.length;

		findDup(result,pos,digitalOutputs, false);
		System.arraycopy(digitalOutputs, 0, result, pos, digitalOutputs.length);
		pos+=digitalOutputs.length;

		findDup(result,pos,pwmOutputs, false);
		System.arraycopy(pwmOutputs, 0, result, pos, pwmOutputs.length);
		pos+=pwmOutputs.length;

		if (configI2C) {
			findDup(result,pos,GrovePiConstants.i2cPins, false);
			System.arraycopy(GrovePiConstants.i2cPins, 0, result, pos, GrovePiConstants.i2cPins.length);
			pos+=GrovePiConstants.i2cPins.length;
		}

		return result;
	}
}

