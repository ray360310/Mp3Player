package com.taipeitecheerh.mp3player;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
 
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class Mp3Player_main extends Activity {
	
	private String FILE_PREFIX = "/storage/sdcard0/";
	private static final String[] FILE_PREFIXES = { "/storage/sdcard0/", "/mnt/sdcard/Music/" }; 
	
	private static final String TAG = "Mp3Player_bluetooth";
    private static final boolean D = true;
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    private int mState = WAITING_FOR_LIBRARY;
    // States for the progression of peer connection
    public static final int WAITING_FOR_LIBRARY 	= 0;
    public static final int WAITING_FOR_ACTION 		= 1;
    public static final int DOWNLOADING				= 2;
    public static final int UPLOADING				= 3;
    
    private int mDownloadState = DOWNLOADING_FILE_INFO;
    // States for downloading
    public static final int DOWNLOADING_FILE_INFO	= 0;
    public static final int DOWNLOADING_FILE_CONTENT= 1;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE 	= 0;
	private static final int REQUEST_ENABLE_BT 				= 1;
    // 0 as input to discovery makes it always discoverable
 	private static final int DISCOVERY_DURATION = 15;
    private String mConnectedDeviceName = null;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private Bluetoothservice bluetoothservice = null;
    
	WakeLock wakeLock;
    private static final String[] EXTENSIONS = { ".mp3", ".mid", ".wav", ".ogg", ".mp4" }; //Playable Extensions
    private List<String> pathaddresslist = new ArrayList<String>();
    List<String> trackNames; //Playable Track Titles
    //AssetManager assets; //Assets (Compiled with APK)
    File path; //directory where music is loaded from on SD Card
    //File path2; //directory where album artwork is loaded from on SD Card
    Music track; //currently loaded track
    private Utilities utils;
    private ImageButton imgbtn_previous,imgbtn_pype,imgbtn_next;
    private SeekBar songprogressbar;
    private TextView tvw_songname,tvw_songdetails,tvw_nowplaytime,tvw_endplaytime;
    private String pathstring,songname,songsinger,songalbum;
    private String[] songlist,pathlist,listtransfer;
    private ArrayAdapter<String> musiclistArrayAdapter;
    private StringBuilder mStringLibrary;
    private P2PFile mFile;
    private Handler prohandler = new Handler();
    Random random; //used for shuffle
    boolean shuffle; //is shuffle mode on?
    boolean isTuning; //is user currently jammin out, if so automatically start playing the next track
    int currentTrack; //index of current track selected
    int type,songtempnumber,songtempduration; //0 for loading from assets, 1 for loading from SD card
    Cursor c;
    Uri pathuri;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    setVolumeControlStream(AudioManager.STREAM_MUSIC);
	    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyMediaPlayer");
	    setContentView(R.layout.activity_mp3_player_main);
		if (mBluetoothAdapter == null){
			Toast.makeText(this, "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		// Initialize the BluetoothChatService to perform bluetooth connections
        bluetoothservice = Bluetoothservice.createService(this, mHandler);
        bluetoothservice.registerHandler(mHandler);
	    pathstring = "/storage/sdcard0/music";
	    //put bluetooth ceck here
		type = 1; 
	    initialize(0);
	    
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if (!mBluetoothAdapter.isEnabled()){
			Intent mIntentOpenBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(mIntentOpenBT,REQUEST_ENABLE_BT);
			//if(Debug) Log.d(ACTIVITY_SERVICE, "!mBluetoothAdapter.isEnabled()"); 
		} else {
            if (bluetoothservice == null) setupChat();
        }
	}

	private void initialize(int type){
		tvw_songname = (TextView)findViewById(R.id.textView_songname);
		tvw_songname.setSelected(true);
		tvw_songdetails = (TextView)findViewById(R.id.textView_songdetail);
		tvw_nowplaytime = (TextView)findViewById(R.id.textView_nowplaytime);
		tvw_endplaytime = (TextView)findViewById(R.id.textView_endplaytime);
		imgbtn_previous = (ImageButton)findViewById(R.id.imageButton01);
		imgbtn_previous.setOnClickListener(imgbtn_previous_listener);
		imgbtn_pype = (ImageButton)findViewById(R.id.imageButton02);
		imgbtn_pype.setOnClickListener(imgbtn_pype_listener);
		imgbtn_next = (ImageButton)findViewById(R.id.imageButton03);
		imgbtn_next.setOnClickListener(imgbtn_next_listener);
		songprogressbar = (SeekBar) findViewById(R.id.seekBar_timefocus);
		songprogressbar.setOnSeekBarChangeListener(seekbar_changelistener);
		getdatapath(pathstring);
		locallibrary();
		trackNames = new ArrayList<String>();
	    currentTrack = 0;
	    shuffle = false;
	    isTuning = false;
	    random = new Random();
	    this.type = type;
	    addTracks(songlist);//have to check
	    loadTrack();
	    tvw_songname.setText(songlist[0].substring(0,songlist[0].length()-4));
	    ensureDiscoverable();
	}
	
	private void bluetoothinitialize(){
		Log.d("BLUETOOTH", "Bluetooth rebuild");
		bluetoothservice.stop();
		bluetoothservice.start();
	}
	
	@Override
	public void onResume(){
	    super.onResume();
	    wakeLock.acquire();
	    if (bluetoothservice !=null){
	    	if (bluetoothservice.getState() == Bluetoothservice.STATE_NONE) {
	              bluetoothservice.start();
	            }
	    }
	}
	 
	@Override
	public void onPause(){
	    super.onPause();
	    if (wakeLock.isHeld()){
	    	wakeLock.release();
	    }
	    if(track != null){
	        if(track.isPlaying()){
	            track.pause();
	            isTuning = false;
	        }
	        if(isFinishing()){
	            track.dispose();
	            finish();
	        }
	    } else{
	        if(isFinishing()){
	            finish();
	        }
	    }
	}
	
	private void setupChat(){
		bluetoothservice = new Bluetoothservice(this,mHandler);
		mOutStringBuffer = new StringBuffer("");
	}
	
	private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
            startActivity(discoverableIntent);
        }
    }
	private void connectDevice(String address, boolean secure) {
        // Go to the connection activity
        //Intent connectIntent = new Intent(this, DownloadUploadActivity.class);
        //connectIntent.putExtra(DownloadUploadActivity.ADDRESS, address);
        //startActivity(connectIntent);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                //connectDevice(data, true);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "not_enabled_leaving", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
	private void getdatapath(String datapathstring){//to get local music location path
		path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
		File getdirfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath());
		pathuri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		File[] files = getdirfile.listFiles();
		pathlist = new String[files.length];
		pathaddresslist.clear();
		for (File file : files){
			pathaddresslist.add(file.getPath());
		}
		pathaddresslist.toArray(pathlist);
		songlist = getdirfile.list();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		createMenu(menu);
	    return true;
	}
	
	private void createMenu(Menu menu){
	    MenuItem miLooping = menu.add(0, 0, 0, "Looping");{
	        miLooping.setIcon(R.drawable.looping);
	    	//need to fix the picture
	    }
	    MenuItem miShuffle = menu.add(0, 1, 1, "Shuffle");{
	        miShuffle.setIcon(R.drawable.shuffle);
	    	//need to fix the picture
	    }
	    MenuItem miStop = menu.add(0, 2, 2, "Stop");{
	        miStop.setIcon(R.drawable.stop);
	    }
	    MenuItem miSource = menu.add(0, 3, 3, "Source");{
	        miSource.setIcon(R.drawable.source);
	    }
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
	    switch(item.getItemId()){
	    case 0:
	        //Set Looping
	        synchronized(this){
	            if(track.isLooping()){
	                track.setLooping(false);
	                Toast.makeText(getBaseContext(), "Playing Tracks Sequentially", Toast.LENGTH_SHORT).show();
	            } else{
	                track.setLooping(true);
	                Toast.makeText(getBaseContext(), "Looping " + trackNames.get(currentTrack), Toast.LENGTH_SHORT).show();
	            }
	        }
	        return true;
	    case 1:
	        //Set Shuffle
	        synchronized(this){
	            if(shuffle){
	                //setShuffle(false);
	            } else{
	                //setShuffle(true);
	            }
	        }
	        return true;
	    case 2:
	        //Stop Music
	        synchronized(this){
	            track.switchTracks();
	        }
	        return true;
	    case 3:
	        //Change Source from Assets to SD Card and vice versa
	        synchronized(this){
	            type++;
	            if(type > 1){
	                type = 0;
	            }
	        }
	    default:
	        return false;
	    }
	}
    
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case Bluetoothservice.STATE_CONNECTED:
                	setTitle(getString(R.string.title_connected_to, mConnectedDeviceName));
                	//connectDevice(mConnectedDeviceName, true);
                	sendLibraryToPeer();//send musiclist to control user
                    break;
                case Bluetoothservice.STATE_CONNECTING:
                    setTitle("Connecting");
                    //connectDevice(mConnectedDeviceName, true);
                    break;
                case Bluetoothservice.STATE_LISTEN:
                case Bluetoothservice.STATE_NONE:
                	setTitle("Not connected");
                    break;
                }
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage;
                readMessage = new String(readBuf, 0, msg.arg1);
        		//String[] files = tokens[1].split(":::");
                if (readMessage.equals("dct")==true){
                	bluetoothinitialize();
                }
                if(readMessage.equals("pre")==true){
        			previous();
        		}else if(readMessage.equals("nxt")==true){
        			next();
        		}else if(readMessage.equals("pay")==true){
        			isTuning = false;
        			playpause();
        		}else if(readMessage.equals("pue")==true){
        			isTuning = true;
        			playpause();
        		}else{
        			if(readMessage.startsWith("sng<>")){
        				String[] tokens = readMessage.split("<>");
        				currentTrack = Integer.valueOf(tokens[1]);
        				musiclistclick();
        			}else if(readMessage.startsWith("cdn<>")){
        				String[] tokens = readMessage.split("<>");
        				songtempduration = Integer.valueOf(tokens[1]);
        				remoteprogressbar();
        			}
        		}
        		//bluetoothservice.setPeerFiles(files);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                connectDevice(mConnectedDeviceName, true);
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    /**
	 * Send peeer a library composed of all files in the apps local dir.
	 */
	private void sendLibraryToPeer() {//send musiclist to control user
		sendCommand();
    	//Log.d(TAG, "Sent library: " + mStringLibrary.toString());
	}
	
	private void locallibrary(){
		mStringLibrary = new StringBuilder();
        if (mStringLibrary.length() > 0)
        	mStringLibrary.delete(0, mStringLibrary.length());
        mStringLibrary.append("<>");
		for (int i=0;i<=songlist.length-1;i++){
			mStringLibrary.append(songlist[i]);
			if (i != songlist.length-1){
				mStringLibrary.append(":::");
			}
		}
		mStringLibrary.append("<>");
	}
	
	private void sendCommand() {//sned command
        // Check that we're actually connected before trying anything
        if (bluetoothservice.getState() != Bluetoothservice.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothservice.write(mStringLibrary.toString().getBytes());
    }
	private void senddetail(String command) {//sned command
        // Check that we're actually connected before trying anything
        if (bluetoothservice.getState() != Bluetoothservice.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
     
     // Check that there's actually something to send
        if (command.length() > 0) {
        	// Reset out string buffer to zero and clear the edit text field
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = command.getBytes();
            bluetoothservice.write(send);
        }
    }
    private void setTrack(int direction){
        if(direction == 0){
            currentTrack--;
            if(currentTrack < 0){
                currentTrack = trackNames.size()-1;
            }
        } else if(direction == 1){
            currentTrack++;
            if(currentTrack > trackNames.size()-1){
                currentTrack = 0;
            }
        }
        if(shuffle){
            int temp = random.nextInt(trackNames.size());
            while(true){
                if(temp != currentTrack){
                    currentTrack = temp;
                    break;
                }
                temp++;
                if(temp > trackNames.size()-1){
                    temp = 0;
                }
            }
        }
    }
    
    //Adds the playable files to the trackNames List
  	private void addTracks(String[] temp){
  		if(temp != null){
  			for(int i = 0; i < temp.length; i++){
  				//Only accept files that have one of the extensions in the EXTENSIONS array
  				if(trackChecker(temp[i])){
  					trackNames.add(temp[i]);
  				}
  			}
  			Toast.makeText(getBaseContext(), "Loaded " + Integer.toString(trackNames.size()) + " Tracks", Toast.LENGTH_SHORT).show();
  		}
  	}
  	
  	//Checks to make sure that the track to be loaded has a correct extenson
  	private boolean trackChecker(String trackToTest){
  		for(int j = 0; j < EXTENSIONS.length; j++){
  			if(trackToTest.contains(EXTENSIONS[j])){
  				return true;
  			}
  		}
  		return false;
  	}
    
    @SuppressLint("ShowToast")
	private void loadTrack(){
        if(track != null){
            track.dispose();
        }
        if(trackNames.size() > 0){
        	track = loadMusic();
        	String dispStr = "";
        	String src = "";
        	int totalcount,index;
        	Context ctx = Mp3Player_main.this;
    	    ContentResolver resolver = ctx.getContentResolver();
        	c = resolver.query(pathuri,null,null,null,null);
        	totalcount = c.getCount();
        	c.moveToFirst();
        	for(int i =0;i<totalcount;i++){
        		index = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        		src = c.getString(index);
        		if (src.equals(songlist[currentTrack].substring(0,songlist[currentTrack].length()-4))){
        			index = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        			src = c.getString(index);
        			dispStr = dispStr + src + " | ";
        			index = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
        			src = c.getString(index);
        			dispStr = dispStr + src;
        			tvw_songdetails.setText(dispStr);
        			dispStr = "<=>" + dispStr + "<=>";
        			//senddetail(dispStr);
        			index = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
        			int length = c.getInt(index);
        			dispStr = "<->" + Integer.toString(length) + "<->";
        			senddetail(dispStr);
        			Toast.makeText(getApplicationContext(), dispStr, Toast.LENGTH_SHORT);
        			length = length/1000;
        			int sec = length%60;
        			length = length-sec;
        			int min = length/60;
        			if (sec < 10)
        				dispStr = min +":0"+ sec;
        			else   
        				dispStr =min +":"+ sec;
        			tvw_endplaytime.setText(dispStr);
        		}
        		if (i == totalcount -1){
        			c.close();
        		}else{
        			c.moveToNext();
        		}
        	}
        }
    }
    //loads a Music instance using either a built in asset or an external resource
	private Music loadMusic(){
    	try{
			FileInputStream fis = new FileInputStream(new File(path, trackNames.get(currentTrack)));
            FileDescriptor fileDescriptor = fis.getFD();
            return new Music(fileDescriptor);
        } catch(IOException e){
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Error Loading " + trackNames.get(currentTrack), Toast.LENGTH_SHORT).show();
        }
		return null;
    }
    //Plays the Track
    private void playTrack(){
        if(isTuning && track != null){
            track.play();
            songprogressbar.setProgress(0);
            songprogressbar.setMax(100);
            updateprogressbar();
            //Toast.makeText(getBaseContext(), "Playing " + trackNames.get(currentTrack).substring(0, trackNames.get(currentTrack).length()-4), Toast.LENGTH_SHORT).show();
        }
    }
    public void remoteprogressbar(){
    	songprogressbar.setProgress(songtempduration);
		int totalDuration = track.getsongduration();
		int currentDuration = 0;
		totalDuration = (int) (totalDuration / 1000);
		currentDuration = (int) ((((double)songtempduration) / 100) * totalDuration);
		int currentPosition = currentDuration*1000;
        // forward or backward to certain seconds
        track.seekto(currentPosition);
    }
    public void updateprogressbar(){
    	prohandler.postDelayed(mupdatetimetask, 100);
    }
    /**
     * Background Runnable thread
     * */
    private Runnable mupdatetimetask = new Runnable() {
           public void run() {
               int totalDuration = track.getsongduration();
               int currentDuration = track.getnowduration();
               //Log.i("nowcurrnettime", Integer.toString(currentDuration));
               String dispStr = "";
               int sec = (int) ((currentDuration % (1000*60*60)) % (1000*60) / 1000);
               int min = (int)(currentDuration % (1000*60*60)) / (1000*60);
               if (sec < 10)
            	   dispStr = min +":0"+ sec;
               else   
            	   dispStr =min +":"+ sec;
               // Displaying time completed playing
               tvw_nowplaytime.setText(dispStr);
 
               // Updating progress bar
               Double percentage = (double) 0;
               long currentSeconds = (int) (currentDuration / 1000);
               long totalSeconds = (int) (totalDuration / 1000);
               percentage =(((double)currentSeconds)/totalSeconds)*100;
               int progress = percentage.intValue();
               //Log.d("Progress", ""+progress);
               songprogressbar.setProgress(progress);
               // Running this thread after 100 milliseconds
              prohandler.postDelayed(this, 100);
           }
        };
        
 
    private OnItemClickListener musiclistClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long arg3) {
			currentTrack = position;
			musiclistclick();
        }
    };
    
    private void musiclistclick()
	{
    	track.pause();
    	tvw_songname.setText(songlist[currentTrack].substring(0,songlist[currentTrack].length() - 4));
		loadTrack();
		isTuning = true;
		imgbtn_pype.setImageResource(R.drawable.pause);
		playTrack();
		
	}
    
   private SeekBar.OnSeekBarChangeListener seekbar_changelistener = new SeekBar.OnSeekBarChangeListener() {
	
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		prohandler.removeCallbacks(mupdatetimetask);
		int totalDuration = track.getsongduration();
		int currentDuration = 0;
        totalDuration = (int) (totalDuration / 1000);
        currentDuration = (int) ((((double)seekBar.getProgress()) / 100) * totalDuration);
        int currentPosition = currentDuration*1000;
 
        // forward or backward to certain seconds
        track.seekto(currentPosition);
 
        // update timer progress again
        updateprogressbar();
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		prohandler.removeCallbacks(mupdatetimetask);
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		
	}
};
    
	private View.OnClickListener imgbtn_previous_listener = new View.OnClickListener() {//disconnect function
		@Override
		public void onClick(View v) {//test OK
			// TODO Auto-generated method stub
			previous();
		}
	};
	private View.OnClickListener imgbtn_pype_listener = new View.OnClickListener() {//disconnect function
		@Override
		public void onClick(View v) {//test OK
			// TODO Auto-generated method stub
			playpause();
		}
	};
	private View.OnClickListener imgbtn_next_listener = new View.OnClickListener() {//disconnect function
		@Override
		public void onClick(View v) {//test OK
			// TODO Auto-generated method stub
			next();
		}
	};
	private void previous()
	{
		setTrack(0);
		tvw_songname.setText(songlist[currentTrack].substring(0,songlist[currentTrack].length() - 4));
		imgbtn_pype.setImageResource(R.drawable.pause);
	    loadTrack();
	    isTuning = true;
	    playTrack();
	    tvw_songname.setSelected(true);
	}
	private void playpause()
	{
		synchronized(this){
			if(isTuning){
				isTuning = false;
	            imgbtn_pype.setImageResource(R.drawable.play);
	            track.pause();
			} else{
	            isTuning = true;
	            tvw_songname.setText(songlist[currentTrack].substring(0,songlist[currentTrack].length() - 4));
	            imgbtn_pype.setImageResource(R.drawable.pause);
	             playTrack();
	        }
			tvw_songname.setSelected(true);
	    }
	}
	private void next()
	{
		setTrack(1);
		tvw_songname.setText(songlist[currentTrack].substring(0,songlist[currentTrack].length() - 4));
		imgbtn_pype.setImageResource(R.drawable.pause);
	    loadTrack();
	    isTuning = true;
	    playTrack();
	    tvw_songname.setSelected(true);
	}
	
	/**
     * Abstraction for a peer and user file that will be transferred to the peer
     * or written by the user.
     * @author mtrathjen08
     *
     */
    public class P2PFile {
    	private String mFileName;
    	private long mFileSize;
    	private int mBytesWritten;
    	private FileOutputStream mFileOutStream;
    	private FileInputStream mFileInStream;
    	
    	private static final int BLOCK_SIZE = 4096;
    	
    	/**
    	 * Create a file for transfer to peer.
    	 * @param fileName
    	 */
    	public P2PFile(String fileName) {
    		mFileName = fileName;
    		mBytesWritten = 0;
    		try {
    			File mFile = new File(FILE_PREFIX + mFileName);
    			mFileInStream = new FileInputStream(mFile);
    			mFileSize = mFile.length();
				Log.d(TAG, "Opened file "+ mFileName);
			} catch (FileNotFoundException e) {
				Log.d(TAG, "Failed to open file "+ mFileName);
			}
    		
    	}
    	
    	/**
    	 * Create a local user file.
    	 * @param fileName 
    	 * @param fileSize
    	 */
    	public P2PFile(String fileName, int fileSize) {
    		mFileName = fileName;
    		mFileSize = fileSize;
    		mBytesWritten = 0;
    		try {
    			File mFile = new File(FILE_PREFIX + mFileName);
    			mFile.createNewFile();
    			mFileOutStream = new FileOutputStream(mFile);
				if (D) Log.d(TAG, "Created file "+ mFileName);
			} catch (FileNotFoundException e) {
				Log.d(TAG, "Failed to create file "+ mFileName);
			} catch (IOException e) {
				Log.d(TAG, "Failed to create file "+ mFileName);
			}
    	}
    	
    	/**
    	 * Write len bytes from readBuf starting at index start, to the underlying
    	 * file.
    	 * @param readBuf Bytes of data to write.
    	 * @param start Starting index of the data buffer.
    	 * @param len Number of bytes to write.
    	 */
    	public synchronized void writeToFile(byte[] readBuf, int start, int len) {
    		if (mFileOutStream != null && mState == DOWNLOADING) {
				try {
					mFileOutStream.write(readBuf, 0, len);
					mBytesWritten += len;
					
					if (D) Log.d(TAG, "Wrote " + mBytesWritten + " bytes");
					
					if (mBytesWritten == mFileSize) {
						mState = WAITING_FOR_ACTION;
						mDownloadState = DOWNLOADING_FILE_INFO;
						mFileOutStream.close();
						Toast.makeText(getApplicationContext(), "Finished writing file.",
	                            Toast.LENGTH_LONG).show();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
    	}
    	
    	/**
    	 * Send the underlying file to the peer.
    	 */
    	public void transferFile() {
    		// Send head with file name and size
    		StringBuilder header = new StringBuilder();
    		header.append("<>"+mFileName+":::"+mFileSize+"<>");
    		bluetoothservice.write(header.toString().getBytes());
    		
    		// Read in the requested file and send to peer
    		while (mBytesWritten != mFileSize) {
    			try {
    				byte[] b = new byte[BLOCK_SIZE];
					int read = mFileInStream.read(b, 0, BLOCK_SIZE);
					bluetoothservice.write(b, 0, read);
					mBytesWritten += read;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		Toast.makeText(getApplicationContext(), "Finished transferring file.",
                    Toast.LENGTH_LONG).show();
    	}
    }
	
	
}
