package bittorensimag;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;


public class Msg {
	private int msgLength;
	private String msgType;
    public static final Map<Integer, String> messagesNames;
    static {
        Map<Integer, String> aMap = new HashMap<Integer, String>();
        aMap.put(0, "Choke");
        aMap.put(1, "Unchoke");
        aMap.put(2, "Interested");
        aMap.put(3, "Not interested");
        aMap.put(4, "Have");
        aMap.put(5, "Bitfield");
        aMap.put(6, "Request");
        aMap.put(7, "Piece");
        aMap.put(8, "Cancel");

        messagesNames = Collections.unmodifiableMap(aMap);
    }
	
    public Msg(int msgLength,String msgType) {
    	this.msgLength=msgLength;
    	this.msgType=msgType;
    }
    
	public int getMsgLength() {
		return msgLength;
	}
	public void setMsgLength(int msgLength) {
		this.msgLength = msgLength;
	}
	public String getMsgType() {
		return msgType;
	}
	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}
	
	@Override
	public String toString() {
		return "Msg [getClass()=" + getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString()
				+ "]";
	}
	
	
}
