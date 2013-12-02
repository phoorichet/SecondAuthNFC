package edu.cmu.sv.secondauthnfc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
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

	public static final String TAG = MainActivity.class.getName();
	public static final String CORP_NFC_AUTH_SERVER_URL = "http://tfa-corp.herokuapp.com/users/secondauth.json";
	
	private static final int MESSAGE_SENT = 1;

	protected NfcAdapter nfcAdapter;
	protected PendingIntent nfcPendingIntent;
	
	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginNFCTask mAuthNFCTask = null;
	private TimerNFCTask timerNFCTask = null;
	
	private String mPasscode = null;
	
	private TextView mLoginNFCStatusMessageView;
	private TextView nfcReaderTimer;
	
	public static final Integer CORP_ID = 1;
	
	private Intent nfcIntent = null;
	
	private CountDownTimer mCountDownTimer = null;
	
	private String mEmail = "";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Intent intent = getIntent();
	    mEmail = intent.getStringExtra(LoginActivity.CORP_EMAIL);

		// initialize NFC
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

//		// Register Android Beam callback
//		nfcAdapter.setNdefPushMessageCallback(this, this);
//		// Register callback to listen for message-sent success
//		nfcAdapter.setOnNdefPushCompleteCallback(this, this);
		
		mLoginNFCStatusMessageView = (TextView)findViewById(R.id.nfc_status_message);
		nfcReaderTimer = (TextView)findViewById(R.id.NfcReaderTimer);
		
		timerNFCTask = new TimerNFCTask();
		timerNFCTask.execute((Void) null);
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
			TextView textView = (TextView) findViewById(R.id.nfc_status_message);

			// Reset message count
//			TextView countNFCMessage = (TextView) findViewById(R.id.countNFCMessages);
//			countNFCMessage.setText("Found 0 NDEF messages");

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
//				countNFCMessage.setText("Found " + messages.length + " NDEF messages");
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
						
						
						// Get record #1
						Integer recordNumber = 1;
						Record passcodeRecord = records.get(recordNumber);
						if (passcodeRecord instanceof TextRecord){
							mPasscode = new String(((TextRecord) passcodeRecord).getText());
							if(mPasscode != null){
								// Send to the server for second auth
								 mAuthNFCTask = new UserLoginNFCTask();
								 mAuthNFCTask.execute((Void) null);
								 nfcIntent = intent;
							}else{
								mLoginNFCStatusMessageView.setError(getString(R.string.nfc_passcode_error_message));
							}
						}else{
							mLoginNFCStatusMessageView.setError(getString(R.string.nfc_record_error_message));
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
//		Message composedMessage = composeMessage("p4j3PESac2fAq+CUnzCw3Q==$ifsAKGFq6AXfoTSqLOjpmRZVT9+pFNvU6L0g8126QuUT3hD14tyCVttdpi1v\n00F3");
//		NdefMessage composedMessageNdefMessage = composedMessage.getNdefMessage();
//
//		if (write(composedMessageNdefMessage, intent)) {
//			Log.d(TAG, "Write success!");
//
//			TextView textView = (TextView) findViewById(R.id.nfc_status_message);
//			textView.setText("Write success!");
//		} else {
//			Log.d(TAG, "Write failure!");
//
//			TextView textView = (TextView) findViewById(R.id.nfc_status_message);
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
		

		return message;
	}

	@Override
	public void onNdefPushComplete(NfcEvent arg0) {
		Log.d(TAG, "onNdefPushComplete");
		
		runOnUiThread(new Runnable() {
            public void run() {
                TextView textView = (TextView) findViewById(R.id.nfc_status_message);
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
	
	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthNFCTask != null) {
			return;
		}

		boolean cancel = false;
		
		// Check for a valid passcode
		if (mPasscode == null){
			cancel = true;
		}else{
			
		}

		

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.

		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginNFCStatusMessageView.setText(R.string.login_nfc_progress_signing_in);
//			showProgress(true);
			mAuthNFCTask = new UserLoginNFCTask();
			mAuthNFCTask.execute((Void) null);
		}
	}
	
	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginNFCTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {
			
			Hashtable<String, String> hash = new Hashtable<String, String>();
			hash.put("otp", mPasscode);
			hash.put("corp_id", CORP_ID+"");
			hash.put("email", mEmail);
			Log.d(TAG, "==> Sending OPT: " + mPasscode);
			HttpPostRequest postRequest = new HttpPostRequest();
			String content = postRequest.getContent(CORP_NFC_AUTH_SERVER_URL, hash);
			
			

			// TODO: register the new account here.
			return content;
		}

		@Override
		protected void onPostExecute(final String content) {
			mAuthNFCTask = null;
//			showProgress(false);
			
			int statusCode = 0;
			String passcode = null;
			boolean writeOK = false;
			JSONObject resultJSON = null;
			
			if(content != null){
				Log.i(TAG, content);
				try {
					resultJSON = new JSONObject(content);
					statusCode = resultJSON.getInt("result");
					
					// Valid response
					if (statusCode == 1 && nfcIntent!= null){
						// Write back to NFC
						
						passcode = resultJSON.getString("otp");
						Message composedMessage = composeMessage(passcode);
						NdefMessage composedMessageNdefMessage = composedMessage.getNdefMessage();
				
						if (write(composedMessageNdefMessage, nfcIntent)) {
							Log.d(TAG, "Write success!");
				
							TextView textView = (TextView) findViewById(R.id.nfc_status_message);
							textView.setText("Write success!");
							writeOK = true;
						} else {
							Log.d(TAG, "Write failure!");
				
							TextView textView = (TextView) findViewById(R.id.nfc_status_message);
							textView.setText("Write failure!");
						}
						
						Log.d(TAG, "  ==> GOT passcode: "+passcode);
						
//						Intent launchSecondAuth = new Intent(getApplicationContext(), MainActivity.class);
//						startActivity(launchSecondAuth);
						
					}else{
						passcode = resultJSON.getString("message");
						mLoginNFCStatusMessageView.setText(passcode);
					}	
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					
				}
			}
			
			

			if (writeOK) {
				// StopTimer
				if(mCountDownTimer != null){
					mCountDownTimer.cancel();
				}
				
				// Launch Corp App
				Intent lauchCorpIntent = new Intent(getApplicationContext(), CorpActivity.class);
				startActivity(lauchCorpIntent);
				finish();
			} else {
				
			}
		}

		@Override
		protected void onCancelled() {
			mAuthNFCTask = null;
//			showProgress(false);
		}
	}
	
	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class TimerNFCTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean finished) {
			mAuthNFCTask = null;
////			showProgress(false);
//
			if (finished != null) {
//				finish();
			} else {
//				mLoginNFCStatusMessageView.setError(getString(R.string.nfc_auth_error_message));
			}
		}

		@Override
		protected void onCancelled() {
//			mAuthNFCTask = null;
////			showProgress(false);
			if(mCountDownTimer != null){
				mCountDownTimer.cancel();
			}
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mCountDownTimer = new CountDownTimer(15000, 1000) {

			     public void onTick(long millisUntilFinished) {
			    	 nfcReaderTimer.setText("seconds remaining: " + millisUntilFinished / 1000);
			     }

			     public void onFinish() {
			    	 nfcReaderTimer.setText("Reading time up!");
			    	 
			    	 Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
			    	 startActivity(intent);
			    	 finish();
			     }
			  }.start();
		}
			
	}

}
