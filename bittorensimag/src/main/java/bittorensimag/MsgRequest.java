package bittorensimag;

public class MsgRequest extends MsgHave{
	private int beginOffset;
	private int pieceLength;
	
	public MsgRequest(int msgLength, int msgType, int index, int beginOffset, int pieceLength) {
		super(msgLength, msgType, index);
		this.beginOffset = beginOffset;
		this.pieceLength = pieceLength;
	}

	public int getBeginOffset() {
		return beginOffset;
	}

	public void setBeginOffset(int beginOffset) {
		this.beginOffset = beginOffset;
	}

	public int getPieceLength() {
		return pieceLength;
	}

	public void setPieceLength(int pieceLength) {
		this.pieceLength = pieceLength;
	}
	
	
	
}
