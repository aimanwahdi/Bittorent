package bittorensimag.Messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import bittorensimag.MessageCoder.*;

// Messages with length = 1
public class Simple extends Msg {
    private static final Logger LOG = Logger.getLogger(Simple.class);

    protected int msgType;

    public final static int LENGTH = 1;
    
    public final static int CHOKE = 0;
    public final static int UNCHOKE = 1;
    public final static int INTERESTED = 2;
    public final static int NOTINTERESTED = 3;

    public Simple(int msgType) {
        super(LENGTH, msgType);
        this.msgType = super.getMsgType();
    }

    public int getMsgLength() {
        return super.getMsgLength();
    }

    public int getMsgType() {
        return super.getMsgType();
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    @Override
    public String toString() {
        return "Msg [getClass()=" + getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString()
                + "]";
    }

    public static void sendMessage(int msgType, OutputStream out) throws IOException {
        MsgCoderToWire coderToWire = new MsgCoderToWire();
        Simple msg = new Simple(msgType);
        coderToWire.frameMsg(coderToWire.toWire(msg), out);
        LOG.debug("Message " + Msg.messagesNames.get(msgType) + " sent");
    }
}
