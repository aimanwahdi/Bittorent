package bittorensimag;

public class MsgPiece extends Msg{
	private int pieceIndex;
	private int beginOffset;
	private byte[] data;
	
	public MsgPiece(int msgLength, int msgType, int pieceIndex, int beginOffset, byte[] data) {
		super(msgLength, msgType);
		this.pieceIndex = pieceIndex;
		this.beginOffset = beginOffset;
		this.data = data;
	}

	public int getPieceIndex() {
		return pieceIndex;
	}

	public void setPieceIndex(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}

	public int getBeginOffset() {
		return beginOffset;
	}

	public void setBeginOffset(int beginOffset) {
		this.beginOffset = beginOffset;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	
}
