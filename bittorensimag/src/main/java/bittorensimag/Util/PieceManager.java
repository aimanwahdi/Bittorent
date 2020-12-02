package bittorensimag.Util;

import java.util.ArrayList;
import java.util.HashMap;

public class PieceManager {
	private HashMap<Integer, ArrayList<String>> pieceMap; //Clé : Piece Index, Valeur : List of peers that contains this piece
	private ArrayList<Boolean> downloaded;
	private ArrayList<Boolean> requestSent;
	private int numOfPiece;
	
	
	public PieceManager(HashMap<Integer, ArrayList<String>> pieceMap, ArrayList<Boolean> downloaded, ArrayList<Boolean> requestSent, int numOfPiece) {
		super();
		this.pieceMap = pieceMap;
		this.downloaded = downloaded;
		this.requestSent = requestSent;
		this.numOfPiece = numOfPiece;
	}
	
	public void requestSent(int pieceRequested) {
		requestSent.set(pieceRequested, true);
	}
	
	public int nextPieceToRequest(int pieceReceived, String currentPeer) {
		downloaded.set(pieceReceived, true);
		int nextPiece = 0;
		while ((nextPiece < numOfPiece) && (requestSent.get(nextPiece) == true || !pieceMap.get(nextPiece).contains(currentPeer)) ) {
			nextPiece += 1;
		}
		if(nextPiece == numOfPiece) { //if all the request has been sent, return -1 to tell that it is finished
			return -1;
		} else {
			return nextPiece;
		}
		
		
	}
	
	

}
