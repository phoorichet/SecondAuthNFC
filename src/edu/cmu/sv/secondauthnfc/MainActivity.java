package edu.cmu.sv.secondauthnfc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.ndeftools.Message;
import org.ndeftools.Record;
import org.ndeftools.externaltype.AndroidApplicationRecord;
import org.ndeftools.externaltype.ExternalTypeRecord;
import org.ndeftools.externaltype.GenericExternalTypeRecord;
import org.ndeftools.wellknown.TextRecord;

import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
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

public class MainActivity extends Activity implements
		CreateNdefMessageCallback, OnNdefPushCompleteCallback {

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
		nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

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

			// Reset message count
			TextView countNFCMessage = (TextView) findViewById(R.id.countNFCMessages);
			countNFCMessage.setText("Found 0 NDEF messages");

			textView.setText("NFC Tag discovered!!");
			
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			Ndef ndef = Ndef.get(tag);
			Log.d(TAG, "Maximum tag size is " + ndef.getMaxSize());


			Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (messages != null) {
				
				Log.d(TAG, "Found " + messages.length + " NDEF messages"); // is
																			// almost
																			// always
																			// just
																			// one
				countNFCMessage.setText("Found " + messages.length + " NDEF messages");
				vibrate(); // signal found messages :-)

				// parse to records
				for (int i = 0; i < messages.length; i++) {
					try {
						// Convert to higher level message
						List<Record> records = new Message((NdefMessage) messages[i]);

						Log.d(TAG, "Found " + records.size() + " records in message " + i);

						for (int k = 0; k < records.size(); k++) {
							Log.d(TAG, " Record #" + k + " is of class " + records.get(k).getClass().getSimpleName());
							Record record = records.get(k);
							if (record instanceof AndroidApplicationRecord) {
								AndroidApplicationRecord aar = (AndroidApplicationRecord) record;
								Log.d(TAG, "  ==> " + new String(aar.getData(), "UTF-8") );
								
								
							}else if (record instanceof TextRecord){
								Log.d(TAG, record.getNdefRecord().toString());
								Log.d(TAG, "  ==> " + new String(((TextRecord) record).getText()));
							} else {
								Log.d(TAG, "Package is " + record.getNdefRecord().toString());
								
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

//		// Test write NFC tags
//		Message composedMessage = composeMessage("Mother Fucker!!!");
//		NdefMessage composedMessageNdefMessage = composedMessage.getNdefMessage();
//
//		if (write(composedMessageNdefMessage, intent)) {
//			Log.d(TAG, "Write success!");
//
//			TextView textView = (TextView) findViewById(R.id.title);
//			textView.setText("Write success!");
//		} else {
//			Log.d(TAG, "Write failure!");
//
//			TextView textView = (TextView) findViewById(R.id.title);
//			textView.setText("Write failure!");
//		}
	}

	public boolean write(NdefMessage rawMessage, Intent intent) {
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

		Ndef ndef = Ndef.get(tag);
		if (ndef != null) {
			try {
				Log.d(TAG, "Write formatted tag");

				ndef.connect();
				if (!ndef.isWritable()) {
					Log.d(TAG, "Tag is not writeable");

					return false;
				}

				if (ndef.getMaxSize() < rawMessage.toByteArray().length) {
					Log.d(TAG,
							"Tag size is too small, have " + ndef.getMaxSize()
									+ ", need "
									+ rawMessage.toByteArray().length);

					return false;
				}
				ndef.writeNdefMessage(rawMessage);

				return true;
			} catch (Exception e) {
				Log.d(TAG, "Problem writing to tag", e);
			} finally {
				try {
					ndef.close();
				} catch (IOException e) {
					// ignore
				}
			}
		} else {
			Log.d(TAG, "Write to an unformatted tag not implemented");
		}

		return false;
	}

	protected void printTagId(Intent intent) {
		if (intent.hasExtra(NfcAdapter.EXTRA_ID)) {
			byte[] byteArrayExtra = intent
					.getByteArrayExtra(NfcAdapter.EXTRA_ID);

			Log.d(TAG, "Tag id is " + toHexString(byteArrayExtra));
		}
	}

	/**
	 * Converts the byte array to HEX string.
	 * 
	 * @param buffer
	 *            the buffer.
	 * @return the HEX string.
	 */
	public String toHexString(byte[] buffer) {
		StringBuilder sb = new StringBuilder();
		for (byte b : buffer)
			sb.append(String.format("%02x ", b & 0xff));
		return sb.toString().toUpperCase();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");

		super.onPause();

		disableForegroundMode();
	}

	public void enableForegroundMode() {
		Log.d(TAG, "enableForegroundMode");

		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED); // filter for all
		IntentFilter[] writeTagFilters = new IntentFilter[] { tagDetected };
		nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent,
				writeTagFilters, null);
	}

	public void disableForegroundMode() {
		Log.d(TAG, "disableForegroundMode");

		nfcAdapter.disableForegroundDispatch(this);
	}

	public Message composeMessage(String text) {
		Log.d(TAG, "createMessage");

		Message message = new Message(); // ndeftools ndef message

		// add an android application record
		AndroidApplicationRecord aar = new AndroidApplicationRecord("edu.cmu.sv.secondauthnfc");
		message.add(aar);

		// add a text record
		TextRecord record = new TextRecord(text);
		message.add(record);
		message.add(new TextRecord("Lock is here!!"));

		return message;
	}

	@Override
	public void onNdefPushComplete(NfcEvent arg0) {
		Log.d(TAG, "onNdefPushComplete");
		
		runOnUiThread(new Runnable() {
            public void run() {
                TextView textView = (TextView) findViewById(R.id.title);
                textView.setText("Message beamed!");
            }   
        });

		// A handler is needed to send messages to the activity when this
		// callback occurs, because it happens from a binder thread
		mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();

	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		Log.d(TAG, "createNdefMessage");

		Message message = new Message(); // ndeftools ndef message

		// add an android application record
		AndroidApplicationRecord aar = new AndroidApplicationRecord("edu.cmu.sv.secondauthnfc");
		message.add(aar);

		// create external type record to be pushed
		ExternalTypeRecord record = new GenericExternalTypeRecord(
				"com.my.data", "myDataType",
				"This is my magic payload".getBytes(Charset.forName("UTF-8")));
		message.add(record);

		// encode one or more record to NdefMessage
		return message.getNdefMessage();
	}

	/** This handler receives a message from onNdefPushComplete */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MESSAGE_SENT:
				Toast.makeText(getApplicationContext(), "Message beamed!",
						Toast.LENGTH_LONG).show();
				break;
			}
		}
	};

	private void vibrate() {
		Log.d(TAG, "vibrate");

		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibe.vibrate(500);
	}

}
