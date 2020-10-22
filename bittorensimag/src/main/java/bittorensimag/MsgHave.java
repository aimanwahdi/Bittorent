package bittorensimag;

public class MsgHave extends Msg{
	private int index;

	public MsgHave(int msgLength, int msgType, int index) {
		super(msgLength, msgType);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
	
}
