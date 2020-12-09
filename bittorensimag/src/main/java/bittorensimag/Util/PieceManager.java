package bittorensimag.Util;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class PieceManager {
	private HashMap<Integer, ArrayList<Socket>> pieceMap; //Clé : Piece Index, Valeur : List of peers that contains this piece
	private ArrayList<Boolean> downloaded;
	private ArrayList<Boolean> requestSent;
	private int numOfPiece;
	
	
//	public PieceManager(HashMap<Integer, ArrayList<String>> pieceMap, ArrayList<Boolean> downloaded, ArrayList<Boolean> requestSent, int numOfPiece) {
	public PieceManager(int numOfPiece) {
		super();
		this.pieceMap = new HashMap<Integer, ArrayList<Socket>>();
		this.downloaded = new ArrayList<Boolean>() ;
		this.requestSent = new ArrayList<Boolean>();
		this.numOfPiece = numOfPiece;
		// in the beginning we don't have any piece requested or downloaded
		this.initList(this.downloaded,numOfPiece); 
		this.initList(this.requestSent,numOfPiece);
	}
	
	public void requestSent(int pieceRequested) {
		requestSent.set(pieceRequested, true);
	}
	
	public void pieceDownloaded(int pieceReceived) {
		downloaded.set(pieceReceived, true);
	}
	
	public int nextPieceToRequest(Socket currentPeer) {
		int nextPiece = 0;
		while ((nextPiece < numOfPiece) && (requestSent.get(nextPiece) == true || !pieceMap.get(nextPiece).contains(currentPeer) || downloaded.get(nextPiece) == true) ) {
			nextPiece += 1;
		}
		if(nextPiece == numOfPiece) { //if all the request has been sent, return -1 to tell that it is finished
			return -1;
		} else {
			return nextPiece;
		}
	}
	
	public boolean isRequested(int pieceIndex) {
		return requestSent.get(pieceIndex);
	}
	
	public void initList(ArrayList<Boolean> list, int size) {
		for(int i = 0; i < size; i++) {
			list.add(false);
		}
	}

	public HashMap<Integer, ArrayList<Socket>> getPieceMap() {
		return pieceMap;
	}

	public ArrayList<Boolean> getDownloaded() {
		return downloaded;
	}
}
