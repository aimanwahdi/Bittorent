package bittorensimag;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class Simple extends Msg implements MsgCoder {
    private int msgLength;
    private int msgType;

    public Simple(int msgLength, int msgType) {
        super(msgLength, msgType);
    }

    public int getMsgLength() {
        return msgLength;
    }

    public void setMsgLength(int msgLength) {
        this.msgLength = msgLength;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    @Override
    public String toString() {
        return "Msg [getClass()=" + getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString()
                + "]";
    }

    @Override
    public void accept(MsgCoderDispatcher dispatcher) throws IOException {
        dispatcher.toWire(this);

    }

    @Override
    public Msg fromWire(byte[] input) throws IOException {
        ByteArrayInputStream bs = new ByteArrayInputStream(input);
        DataInputStream in = new DataInputStream(bs);

        int length = in.readInt();
        int type = in.readByte();

        return new Msg(length, type);
    }
}
