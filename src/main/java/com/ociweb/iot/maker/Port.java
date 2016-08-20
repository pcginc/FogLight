package com.ociweb.iot.maker;

public enum Port {
	A0(Port.IS_ANALOG, 0),
	A1(Port.IS_ANALOG, 1),
	A2(Port.IS_ANALOG, 2),
	
	D0(Port.IS_DIGITAL, 0),
	D1(Port.IS_DIGITAL, 1),
	D2(Port.IS_DIGITAL, 2),
	D3(Port.IS_DIGITAL, 3),
	D4(Port.IS_DIGITAL, 4),
	D5(Port.IS_DIGITAL, 5),
	D6(Port.IS_DIGITAL, 6),
	D7(Port.IS_DIGITAL, 7),
	D8(Port.IS_DIGITAL, 8);
	
	public byte port;
	public byte mask;
	
	private Port(byte mask, int number) {
		this.port = (byte)number;
		this.mask = mask;
	}
	
	public static final byte IS_ANALOG =  1;
	public static final byte IS_DIGITAL = 2;

	public boolean isAnalog() {
		return 0!=(IS_ANALOG&mask);
	}

	public static Port[] ANALOGS = {A0,A1,A2};
	public static Port[] DIGITALS = {D0,D1,D2,D3,D4,D5,D6,D7,D8};

	
}
