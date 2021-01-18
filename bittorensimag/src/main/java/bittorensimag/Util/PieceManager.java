package bittorensimag.Util;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class PieceManager {
	private HashMap<Integer, ArrayList<Socket>> pieceMap; //Clé : Piece Index, Valeur : List of peers that contains this piece
	private ArrayList<Boolean> downloaded;
	private ArrayList<Boolean> requestSent;
	private ArrayList<Integer> needed;
	private int numOfPiece;

//	public PieceManager(HashMap<Integer, ArrayList<String>> pieceMap, ArrayList<Boolean> downloaded, ArrayList<Boolean> requestSent, int numOfPiece) {
	public PieceManager(int numOfPiece) {
		super();
		this.pieceMap = new HashMap<Integer, ArrayList<Socket>>();
		this.downloaded = new ArrayList<Boolean>() ;
		this.requestSent = new ArrayList<Boolean>();
		this.numOfPiece = numOfPiece;
		this.needed = new ArrayList<Integer>();
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

	public void pieceNeeded(int pieceNeeded) {
		needed.add(pieceNeeded);
	}

	public int nextPieceToRequest(Socket currentPeer) {
		boolean peerHasPieceWeNeed = false;
		int nextPiece = -1;
		for (int i = 0; i < needed.size(); i++) {
			nextPiece = needed.get(i);
			// if request has already been sent or peer does not have nextPiece
			if (pieceMap.containsKey(nextPiece)) {
				if (requestSent.get(nextPiece) || !pieceMap.get(nextPiece).contains(currentPeer)) {
					continue;
				}
				else {
					peerHasPieceWeNeed = true;
					break;
				}
			}
		}
		if (!peerHasPieceWeNeed) { // if peer has no piece we need, return -1
			return -1;
		} else {
			return nextPiece;
		}
	}

	public void initNeededPiecesList(int numberOfPieces) {
		for (int i = 0; i < numberOfPieces; i++) {
			needed.add(i);
		}
	}

	public void pieceNoMoreNeeded(int piece) {
		needed.remove(Integer.valueOf(piece));
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

	public void setPieceMap(HashMap<Integer, ArrayList<Socket>> newMap) {
		this.pieceMap = newMap;
	}

	public void sortPiecesNeeded() {
		Set<Integer> sortedPieces = pieceMap.keySet();
		List<Integer> sortedList = new ArrayList<Integer>();
		sortedList.addAll(sortedPieces);
		Collections.sort(needed, Comparator.comparing(item -> sortedList.indexOf(item)));
	}

	public ArrayList<Integer> getPieceNeeded() {
		return needed;
	}

	public ArrayList<Boolean> getDownloaded() {
		return downloaded;
	}
}
