package com.taipeitecheerh.mp3player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstracts a peer device by storing relevant information.
 * @author mtrathjen08
 *
 */
public class Peerdevice {
	// Device name
	private String mDeviceName;
	// List of peer files
	private List<String> mPeerFiles;
	// List of client files
	private List<String> mClientFiles;
	
	public Peerdevice() {
		mDeviceName = "";
		mPeerFiles = new LinkedList<String>();
		mClientFiles = new LinkedList<String>();
	}
	
	// Get and Set functions for fields.
	
	public void setDeviceName(String name) {
		mDeviceName = name;
	}
	public void setClientFiles(String[] files) {
		for (String s: files)
			mClientFiles.add(s);
	}
	public void setPeerFiles(String[] files) {
		for (String s: files)
			mPeerFiles.add(s);
	}
	public String getDeviceName() {
		return mDeviceName;
	}
	public List<String> getClientFiles() {
		return Collections.unmodifiableList(mClientFiles);
	}
	public List<String> getPeerFiles() {
		return Collections.unmodifiableList(mPeerFiles);
	}
}
