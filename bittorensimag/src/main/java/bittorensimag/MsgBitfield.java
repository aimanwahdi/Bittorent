package bittorensimag;

public class MsgBitfield extends Msg{
	private byte[] bitfieldDATA = new byte[2];

	public MsgBitfield(int msgLength, int msgType, byte[] bitfieldDATA) {
		super(msgLength, msgType);
		this.bitfieldDATA = bitfieldDATA;
	}

	public byte[] getBitfieldDATA() {
		return bitfieldDATA;
	}

	public void setBitfieldDATA(byte[] bitfieldDATA) {
		this.bitfieldDATA = bitfieldDATA;
	}
	
	
}
