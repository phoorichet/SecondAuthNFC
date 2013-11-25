package edu.cmu.sv.secondauthnfc;

import java.util.List;

import org.ndeftools.Message;
import org.ndeftools.Record;
import org.ndeftools.externaltype.AndroidApplicationRecord;
import org.ndeftools.wellknown.TextRecord;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Vibrator;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback {

	public static String TAG = MainActivity.class.getName();
	private static final int MESSAGE_SENT = 1;
	
	protected NfcAdapter nfcAdapter;
	protected PendingIntent nfcPendingIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// initialize NFC
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Register Android Beam callback
		nfcAdapter.setNdefPushMessageCallback(this, this);
		// Register callback to listen for message-sent success
		nfcAdapter.setOnNdefPushCompleteCallback(this, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "onResume");

		super.onResume();

		enableForegroundMode();
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent");

		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {			
			TextView textView = (TextView) findViewById(R.id.title);

			textView.setText("Hello NFC!");

			Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (messages != null) {

				Log.d(TAG, "Found " + messages.length + " NDEF messages"); // is almost always just one

				vibrate(); // signal found messages :-)

				// parse to records
				for (int i = 0; i < messages.length; i++) {
					try {
						List<Record> records = new Message((NdefMessage)messages[i]);
						
						Log.d(TAG, "Found " + records.size() + " records in message " + i);
						
						for(int k = 0; k < records.size(); k++) {
							Log.d(TAG, " Record #" + k + " is of class " + records.get(k).getClass().getSimpleName());
							Record record = records.get(k);
		                    if(record instanceof AndroidApplicationRecord) {
								AndroidApplicationRecord aar = (AndroidApplicationRecord)record;
								Log.d(TAG, "Package is " + record.getNdefRecord());
							}
		
						}
					} catch (Exception e) {
						Log.e(TAG, "Problem parsing message", e);
					}

 				}
			}
		} else {
			// ignore
		}
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");

		super.onPause();

		disableForegroundMode();
	}
	
	public void enableForegroundMode() {
		Log.d(TAG, "enableForegroundMode");

		IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED); // filter for all
		IntentFilter[] writeTagFilters = new IntentFilter[] {tagDetected};
		nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, writeTagFilters, null);
	}

	public void disableForegroundMode() {
		Log.d(TAG, "disableForegroundMode");

		nfcAdapter.disableForegroundDispatch(this);
	}
	
	NdefMessage	[] getNdefMessages(Intent intent) {
	    // Parse the intent
	    NdefMessage[] msgs = null;
	    String action = intent.getAction();
	    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
	        || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
	        Parcelable[] rawMsgs = 
	            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
	        if (rawMsgs != null) {
	            msgs = new NdefMessage[rawMsgs.length];
	            for (int i = 0; i < rawMsgs.length; i++) {
	                msgs[i] = (NdefMessage) rawMsgs[i];
	            }
	        } else {
	            // Unknown tag type
	            byte[] empty = new byte[] {};
	            NdefRecord record = 
	                new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
	            NdefMessage msg = new NdefMessage(new NdefRecord[] {
	                record
	            });
	            msgs = new NdefMessage[] {
	                msg
	            };
	        }
	    } else {
	        Log.d(TAG, "Unknown intent.");
	        finish();
	    }
	    return msgs;
	}

	@Override
	public void onNdefPushComplete(NfcEvent arg0) {
		Log.d(TAG, "onNdefPushComplete");
		
		// A handler is needed to send messages to the activity when this
		// callback occurs, because it happens from a binder thread
		mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
		
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent arg0) {
		Log.d(TAG, "createNdefMessage");

		// create record to be pushed
		TextRecord record = new TextRecord("This is my text record");

		// encode one or more record to NdefMessage
		return new NdefMessage(record.getNdefRecord());
	}	
	
	/** This handler receives a message from onNdefPushComplete */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MESSAGE_SENT:
				Toast.makeText(getApplicationContext(), "Message beamed!", Toast.LENGTH_LONG).show();
				break;
			}
		}
	};	
	
	private void vibrate() {
		Log.d(TAG, "vibrate");
		
		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE) ;
		vibe.vibrate(500);
	}

}
