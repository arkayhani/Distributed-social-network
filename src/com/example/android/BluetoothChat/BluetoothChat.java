/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.LayoutInflater.Factory;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;
	private static boolean ensure = true;
	private static boolean run = true;
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static String mode = "multi";
	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	private static final int REQUEST_Discovareble = 4;
	public timerthread timer;
	// Layout Views
	private TextView mTitle;
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;
	private static boolean autosendallmessages = true;
	private static boolean autostartmessaging = true;
	private static boolean autoconnecttodevices = true;
	private static boolean automovetonextdevice = true;
	private static boolean autoresumemessaging = true;
	private static boolean myturnwrite = false;
	private static boolean myturnread = false;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	private String msendaddr = null;
	private String oldmessage = "";
	private String newmessage = "";
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;
	SQLiteDatabase db;

	private class timerthread extends Thread {
		public int maxtime = 20;
		public int time = 0;
		public Boolean flag = true;
		public Boolean pause = false;
		public void settimer() {
			Random generator = new Random();
			maxtime = 60 + generator.nextInt(20);
			Log.d("time", String.valueOf(maxtime));
		}

		public void cleantimer() {
			// Random generator = new Random();
			Log.d("time", "clear");
			time = 0;
		}
		public void pause() {
			pause=true;
		}
		public void reume() {
			// Random generator = new Random();
			pause=false;
		}
		public timerthread() {
			// TODO Auto-generated constructor stub
			settimer();
			cleantimer();
			flag = true;
			pause=false;

		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			flag = false;
			super.destroy();

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			// super.run();
			while (flag) {
				while (time < maxtime) {
					time++;
					if (!flag) {
						break;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				// do
				if (!flag) {
					break;
				} else {
					mode = "multi";
					ensureDiscoverable();
					Intent serverIntent = null;
					serverIntent = new Intent(BluetoothChat.this, DeviceListActivity.class);
					startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
					settimer();
					cleantimer();
				}
			}

		}

		// r
	}

	public static String reverseIt(String source) {
		int i, len = source.length();
		StringBuilder dest = new StringBuilder(len);

		for (i = (len - 1); i >= 0; i--) {
			dest.append(source.charAt(i));
		}

		return dest.toString();
	}

	public String code(String in, String key) {
		String reverse = key;
		int j = reverse.length() - 1;
		StringBuilder dest = new StringBuilder(in.length());
		for (int i = 0; i < in.length(); i++) {
			if (j < 0)
				j = reverse.length() - 1;
			char m = (char) (in.charAt(i) + (char) (reverse.charAt(j) - 'a'));
			dest.append(m);
			j--;
		}
		return dest.toString();
	}

	public String decode(String in, String key) {
		String reverse = key;
		int j = reverse.length() - 1;
		StringBuilder dest = new StringBuilder(in.length());
		for (int i = 0; i < in.length(); i++) {
			if (j < 0)
				j = reverse.length() - 1;
			char m = (char) (in.charAt(i) - (char) (reverse.charAt(j) - 'a'));
			dest.append(m);
			j--;
		}
		return dest.toString();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		run = true;
		ensure = true;
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");
		Intent load = new Intent(BluetoothChat.this, loadactivty.class);
		startActivity(load);
		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText("Bluetooth Network");
		mTitle = (TextView) findViewById(R.id.title_right_text);
		/// define DataBase
		final String path = Environment.getDataDirectory() + "/data/" + getPackageName() + "/mobile.db";
		db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.CREATE_IF_NECESSARY);
		// define tabels
		create_tabels();

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		if (timer != null) {
			// timer.stop();
			timer.flag=false;
			timer = null;
		}
		timer = new timerthread();
		timer.start();
	}

	/////////////////////////////////////////// DataBase
	/////////////////////////////////////////// Functions///////////////////////////////////////////////////////////////////////
	public void create_tabels() {

		droptabeldata();
		droptabeldevices();
		droptabelmessages();
		String sql = "CREATE TABLE IF NOT EXISTS data (";
		sql += "id TEXT,";
		sql += "message TEXT, ";
		sql += "sentto TEXT, ";
		sql += " status Text);";
		db.execSQL(sql);
		Toast.makeText(getApplicationContext(), "TABELOK", 50000).show();
		sql = "CREATE TABLE IF NOT EXISTS messages (";
		sql += "id TEXT,";
		sql += "message TEXT); ";
		db.execSQL(sql);
		Toast.makeText(getApplicationContext(), "TABEL2OK", 50000).show();
		sql = "CREATE TABLE IF NOT EXISTS devices (";
		sql += "id TEXT,";
		sql += "status TEXT); ";
		db.execSQL(sql);
		Toast.makeText(getApplicationContext(), "TABEL3OK", 50000).show();
	}

	public void create_tabel_devices() {
		droptabeldevices();
		String sql = "CREATE TABLE IF NOT EXISTS devices (";
		sql += "id TEXT,";
		sql += "status TEXT); ";
		db.execSQL(sql);
		Toast.makeText(getApplicationContext(), "TABEL3OK", 50000).show();
	}

	public void inserttodata(String id, String message, String sentto, String status) {

		if (!id_if_exist_data(id, sentto)) {
			String sql = "INSERT INTO data (id ,message,sentto,status) VALUES (?,?,?,?);";
			db.execSQL(sql, new String[] { id, message, sentto, status });
			Log.d("check", "INSERT INTO data (id ,message,sentto,status) VALUES (?,?,?,?);");
			Log.d("check", id + message + sentto + status);
		}
		Toast.makeText(getApplicationContext(), "ADDED", 5000).show();

	}

	public void inserttodevices(String id, String status) {
		if (!id_if_exist_devices(id)) {
			String sql = "INSERT INTO devices (id ,status) VALUES (?,?);";
			db.execSQL(sql, new String[] { id, status });
			Log.d("check", "INSERT INTO devices (id ,status) VALUES (?,?);");
			Log.d("check", id + status);
		}
		Toast.makeText(getApplicationContext(), "ADDED", 5000).show();

	}

	public void inserttomessages(String id, String message) {

		if (!id_if_exist_messages(id)) {
			String sql = "INSERT INTO messages (id ,message) VALUES (?,?);";
			db.execSQL(sql, new String[] { id, message });
			Log.d("check", "INSERT INTO messages (id ,message) VALUES (?,?);");
			Log.d("check", id + message);

		}
		Toast.makeText(getApplicationContext(), "ADDED", 5000).show();

	}

	public Boolean idvto_if_existdata(String id, String sentto) {
		String sql = "SELECT * FROM data WHERE id=? and sentto=?";
		Cursor cr = db.rawQuery(sql, new String[] { id, sentto });
		Log.d("check", "SELECT * FROM data WHERE id=? and sentto=?");
		Log.d("check", id + sentto);
		Toast.makeText(getApplicationContext(), "idvto_if_existdata", 5000).show();
		if (cr.getCount() == 0) {
			cr.close();
			return false;
		}
		cr.close();
		return true;

	}

	public void change_id_status_devices(String id, String status) {
		String sql = "UPDATE devices set status='" + status + "' WHERE id='" + id + "' ";
		db.execSQL(sql);

		// Log.d("check", id+ sentto);
		Log.d("message", "change_id_status_devices");
		Log.d("check", "UPDATE devices set status='" + status + "' WHERE id='" + id + "' ");

	}

	public String first_status_devices(String status) {
		String sql = "SELECT * FROM devices WHERE status=? ";
		Cursor cr = db.rawQuery(sql, new String[] { status });
		Log.d("message", "first_status_devices");

		if (cr.getCount() != 0)

		{
			cr.moveToFirst();
			// Log.d("check", "cr.getcount " + cr.getCount());
			int colid = cr.getColumnIndex("id");
			// Log.d("check", "colid " + colid);
			String result = cr.getString(colid);
			Log.d("check", "result	" + result);
			cr.close();
			return result;

		}
		Log.d("check", "after getcount not done");
		cr.close();
		return "";
	}

	public String first_sento_messages(String sentto) {
		String sql = "SELECT * FROM messages";
		Cursor cr = db.rawQuery(sql, null);
		Log.d("message", "first_sento_messages");
		cr.moveToFirst();
		// Log.d("check", "cr.moveToFirst();");
		if (cr.getCount() != 0)
			do {

				String sql2 = "SELECT * FROM data WHERE id=? and sentto=?";
				// Log.d("check", "cr.getColumnIndex(id)" );
				// Log.d("check", "cr.getString(cr.getColumnIndex(id))" +
				// cr.getString(cr.getColumnIndex("id")));
				Cursor cr2 = db.rawQuery(sql2, new String[] { cr.getString(cr.getColumnIndex("id")), sentto });
				// Log.d("check", "cr2.getCount() " + cr2.getCount());
				if (cr2.getCount() == 0) {
					cr2.moveToFirst();
					// Log.d("check", "cr2.moveToFirst();");
					String result = cr.getString(cr.getColumnIndex("id"));
					cr.close();
					cr2.close();
					return result;
				}
			} while (cr.moveToNext());
		// if(cr.getCount()!=0)
		// return cr.getString(cr.getColumnIndex("id"));
		cr.close();
		return "";
	}

	public void change_first_status_devices(String from, String to) {
		// String sql = "UPDATE devices set status='" + to + "' WHERE '" + from
		// + "'=status ";
		String sql = "SELECT * FROM devices WHERE ?=status ";
		Cursor cr = db.rawQuery(sql, new String[] { from });
		if (cr.getCount() > 0) {
			cr.moveToFirst();
			String result = cr.getString(cr.getColumnIndex("id"));
			change_id_status_devices(result, to);
			cr.close();
			Log.d("message", "change_first_status_devices" + from + to);
		}

	}

	public void change_first_status_data(String from, String to) {
		// String sql = "UPDATE data set status='" + to + "' WHERE '" + from +
		// "'=status ";
		// db.execSQL(sql);
		// Log.d("message", "change_first_status_devices" + from + to);
		String sql = "SELECT * FROM data WHERE ?=status ";
		Cursor cr = db.rawQuery(sql, new String[] { from });
		if (cr.getCount() > 0) {
			cr.moveToFirst();
			String resultid = cr.getString(cr.getColumnIndex("id"));
			String resultsentto = cr.getString(cr.getColumnIndex("sentto"));
			/**************************************/////////////
			// change_id_status_data(result, to);
			cr.close();
			String sql2 = "UPDATE data set status='" + to + "' WHERE '" + resultid + "'=id  and '" + resultsentto
					+ "'=sentto ";
			db.execSQL(sql2);
			Log.d("check", resultid + "'=id '" + resultsentto + "'=sentto ");
			Log.d("check", "change_first_status_data" + from + to);
		}

	}

	public Boolean id_if_exist_devices(String id) {
		String sql = "SELECT * FROM devices WHERE id=? ";
		Cursor cr = db.rawQuery(sql, new String[] { id });
		Log.d("chack", "id_if_exist_devices");
		Log.d("chack", "SELECT * FROM devices WHERE id=? ");
		Log.d("chack", id);

		if (cr.getCount() == 0) {
			cr.close();
			return false;
		}
		cr.close();
		return true;
	}

	public Boolean id_if_exist_data(String id, String sentto) {
		String sql = "SELECT * FROM data WHERE id=? and sentto=?";
		Cursor cr = db.rawQuery(sql, new String[] { id, sentto });
		Log.d("check", "id_if_exist_data");
		Log.d("check", sql + id + "num:" + cr.getCount());
		if (cr.getCount() == 0) {
			cr.close();
			return false;
		}
		cr.close();
		return true;
	}

	public Boolean id_if_exist_messages(String id) {
		String sql = "SELECT * FROM messages WHERE id=? ";
		Cursor cr = db.rawQuery(sql, new String[] { id });
		Log.d("check", "id_if_exist_messages");
		Log.d("check", sql + id);
		if (cr.getCount() == 0) {
			cr.close();
			return false;
		}
		cr.close();
		return true;
	}

	public String id_message_messages(String id) {
		String sql = "SELECT * FROM messages WHERE id=? ";
		Cursor cr = db.rawQuery(sql, new String[] { id });
		Log.d("check", "id_message_messages");
		Log.d("check", sql + id);
		if (id_if_exist_messages(id)) {
			cr.moveToFirst();
			String result = cr.getString(cr.getColumnIndex("message"));
			cr.close();
			return result;
		}
		cr.close();
		return "";

	}

	public int id_count_messages() {
		String sql = "SELECT * FROM messages";
		Cursor cr = db.rawQuery(sql, null);
		Log.d("message", "id_count_messages" + sql);
		int result = cr.getCount();
		cr.close();
		return result;
	}

	public int id_count_devices() {
		String sql = "SELECT * FROM devices";
		Cursor cr = db.rawQuery(sql, null);
		Log.d("message", "id_count_devices" + sql);
		int result = cr.getCount();
		cr.close();
		return result;
	}

	public void droptabeldevices() {
		String sql = "drop table if exists devices";
		db.execSQL(sql);
		Log.d("message", "droptabeldevices" + sql);

	}

	public void droptabelmessages() {
		String sql = "drop table if exists messages";
		db.execSQL(sql);
		Log.d("message", "droptabelmessages" + sql);

	}

	public void droptabeldata() {
		String sql = "drop table if exists data";
		db.execSQL(sql);
		Log.d("message", "droptabeldata" + sql);

	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
		} else if (!mBluetoothAdapter.isEnabled()) {
			// Intent enableIntent = new
			// Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			// startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			ensureDiscoverable();
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");
		// ensureDiscoverable();

		// Log.d("new", "salam");
		// Log.d("new", code("salam"));
		// Log.d("new", decode(code("salam")));
		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		ensureDiscoverable();
		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);
		mConversationView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// Toast.makeText(BluetoothChat.this, "drgrg", 80000).show();
				Intent s = new Intent(BluetoothChat.this, Detail_actvity.class);
				startActivityForResult(s, 403);

				return false;
			}
		});

		// Initialize the compose field with a listener for the return key
		// mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		// mOutEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the send button with a listener that for click events
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				TextView view = (TextView) findViewById(R.id.edit_text_out);
				String message = view.getText().toString();
				view.setText("");
				if (mode.equals("singel"))
					mConversationArrayAdapter.add("Me:  " + message + "(private message)");
				else
					mConversationArrayAdapter.add("Me:  " + message);
				AddMessage(message);
			}
		});
		/*
		 * ((Button) findViewById(R.id.btndis)).setOnClickListener(new
		 * OnClickListener() {
		 * 
		 * @Override public void onClick(View arg0) { connectDevice2();
		 * 
		 * } }); ((Button) findViewById(R.id.btnstr)).setOnClickListener(new
		 * OnClickListener() {
		 * 
		 * @Override public void onClick(View arg0) { // TODO Auto-generated
		 * method stub sendm();
		 * 
		 * } });
		 */
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (timer != null) {
			//Toast.makeText(BluetoothChat.this, "asghal", 8000).show();
			timer.flag = false;
			timer = null;
		}
		run = false;
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
		db.close();

	}

	private void ensureDiscoverable() {
		if (ensure) {
			ensure = false;
			if (D)
				Log.d(TAG, "ensure discoverable");

			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
				// Otherwise, setup the chat session
			} else {
				if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
					Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
					startActivityForResult(discoverableIntent, REQUEST_Discovareble);
				} else {
					ensure = true;
				}
			}
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			if (send.length < 400) {
				message += "*end";
			}
			send = message.getBytes();
			while (send.length < 400) {
				message += "u";
				send = message.getBytes();
			}
			while (send.length > 1024) {
				message = message.substring(0, message.length() - 7);
				message += " ...";
				send = message.getBytes();
			}

			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			mOutEditText.setText(mOutStringBuffer);
		}
	}

	private void sendm() {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d("check", "sendm starts");
		String msgid = first_sento_messages(first_status_devices("connected"));
		Log.d("check", "sendm id	" + msgid);
		if (!msgid.equals("")) {
			String message = id_message_messages(msgid);
			Log.d("check", "sendm message	" + message);
			inserttodata(msgid, message, first_status_devices("connected"), "sending");
			// Check that there's actually something to send
			if (message.length() > 0) {
				// Get the message bytes and tell the BluetoothChatService to
				// write
				message += "*end";
				byte[] send = message.getBytes();
				mChatService.write(send);
				Log.d("check", "sendm write");
				// Reset out string buffer to zero and clear the edit text field
				mOutStringBuffer.setLength(0);
			}
		} else {
			Toast.makeText(getApplicationContext(), "send not found", 5000).show();
			if (automovetonextdevice) {
				{
					if (myturnwrite) {
						myturnwrite = false;
						String message = "newtur";
						Log.d("check", "myturnwrite+sendmnotfound->myturnwrite=false" + message);
						if (message.length() > 0) {
							// Get the message bytes and tell the
							// BluetoothChatService to
							// write
							byte[] send = message.getBytes();
							mChatService.write(send);
							Log.d("check", "sendm write");
							// Reset out string buffer to zero and clear the
							// edit
							// text field

							mOutStringBuffer.setLength(0);
						}
					}
				}

			}
			if (automovetonextdevice) {
				{
					if (myturnread) {
						myturnread = false;
						String message = "finish";
						Log.d("check", "myturnread+sendmnotfound->myturnread=false" + message);

						if (message.length() > 0) {
							// Get the message bytes and tell the
							// BluetoothChatService to
							// write
							byte[] send = message.getBytes();
							mChatService.write(send);
							Log.d("check", "sendm write");
							// Reset out string buffer to zero and clear the
							// edit
							// text field
							mOutStringBuffer.setLength(0);
						}
					}
				}

			}
		}
	}

	private void AddMessage(String message) {
		Log.d("check", mode);
		if (!mode.equals("singel")) {
			String s = mBluetoothAdapter.getAddress() + ";" + id_count_messages();

			if (id_if_exist_messages(s) == false)
				inserttomessages(s, s + "*" + mBluetoothAdapter.getName() + "*" + message);

			Log.d("message", "AddMessage	" + s + "*" + mBluetoothAdapter.getName() + "*" + message);
		} else {

			// Attempt to connect to the device
			// mTitle.setText("pr);
			String s = mBluetoothAdapter.getAddress() + ";" + id_count_messages();

			if (id_if_exist_messages(s) == false)
				inserttomessages(s, s + "*&" + code(msendaddr, msendaddr) + "&"
						+ code(mBluetoothAdapter.getName() + "$" + message, msendaddr));

			Log.d("message", "AddMessage	" + s + "*&" + msendaddr + "&" + message);
			// Log.d("message", "AddMessage " + s + "*&" + msendaddr + "&" +
			// code(message));
			// Log.d("message", "AddMessage " + s + "*&" + msendaddr + "&" +
			// decode(code(message)));

		}

	}

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				// sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (timer!=null) {
				timer.pause();
			}
			if (run)
				switch (msg.what) {
				case MESSAGE_STATE_CHANGE:
					if (timer != null)
						timer.cleantimer();
					if (D)
						Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
					switch (msg.arg1) {
					case BluetoothChatService.STATE_CONNECTED:
						mTitle.setText(R.string.title_connected_to);
						mTitle.append(mConnectedDeviceName);
						// mConversationArrayAdapter.clear();
						change_first_status_devices("connecting", "connected");
						if (autostartmessaging && myturnwrite) {
							Log.d("chech", "stateconnect&myturnwrite->sendm");
							sendm();
						}
						break;
					case BluetoothChatService.STATE_CONNECTING:
						mTitle.setText(R.string.title_connecting);
						change_first_status_devices("new", "connecting");

						break;
					case BluetoothChatService.STATE_Stop:

						mTitle.setText(R.string.title_connecting);
						change_first_status_devices("connected", "stop");
						change_first_status_devices("connecting", "failed");
						if (automovetonextdevice && myturnwrite) {
							Log.d("chech", "statestop&myturnwrite->connectdevice2");
							connectDevice2();
						}

						break;
					case BluetoothChatService.STATE_LISTEN:
					case BluetoothChatService.STATE_NONE:
						mTitle.setText(R.string.title_not_connected);
						break;
					}
					break;
				case MESSAGE_WRITE:
					if (timer != null)
						timer.cleantimer();
					mode = "multi";
					Log.d("check", "MESSAGE_WRIE");
					if (myturnread || myturnwrite) {
						Log.d("check", "MESSAGE_WRIE after myturnread || myturnwrite");
						byte[] writeBuf = (byte[]) msg.obj;
						// construct a string from the buffer
						String writeMessage = new String(writeBuf);
						// mConversationArrayAdapter.add("Me: " + writeMessage);
						change_first_status_data("sending", "sent");

						if (autosendallmessages) {
							Log.d("chech", "writemessage->snedm()");
							sendm();
						}
					}

					break;
				case MESSAGE_READ:
					if (timer != null)
						timer.cleantimer();
					mode = "multi";
					byte[] readBuf = (byte[]) msg.obj;
					// construct a string from the valid bytes in the buffer
					String readMessage = new String(readBuf, 0, msg.arg1);
					// Log.d("check", "read message:" +
					// "new"+":"+"new".hashCode());
					// Log.d("check", "read message:" +
					// readMessage+":"+readMessage.hashCode()+readMessage.compareTo("new")+(readMessage.equals("new")));
					if (readMessage.contains("*end")) {
						readMessage = readMessage.split("\\*end")[0];
					}
					if (readMessage.equals("newtur")) {
						Log.d("chech", "messageread&new->myturnread=true");
						myturnread = true;
						sendm();
					} else if (readMessage.equals("finish")) {
						Log.d("chech", "messageread&fin->myturnwrite=true&mchatservice.start");
						myturnwrite = true;
						mChatService.start();
					}

					else {
						if (!id_if_exist_messages(readMessage.split(Pattern.quote("*"))[0])) {

							Log.d("check", "read message   " + readMessage);
							if (readMessage.split(Pattern.quote("*"))[1].charAt(0) == '&'
									&& (decode(readMessage.split(Pattern.quote("&"))[1], mBluetoothAdapter.getAddress())
											.equals(mBluetoothAdapter.getAddress()))) {
								mConversationArrayAdapter.add(// readMessage.split(Pattern.quote(";"))[0]
																// + ": "
										decode(readMessage.split(Pattern.quote("&"))[2], mBluetoothAdapter.getAddress())
												.split(Pattern.quote("$"))[0]
												+ ":  "
												+ decode(readMessage.split(Pattern.quote("&"))[2],
														mBluetoothAdapter.getAddress()).split(Pattern.quote("$"))[1]
												+ "(private message)");

							} else {
								if (readMessage.split(Pattern.quote("*"))[1].charAt(0) != '&')
									mConversationArrayAdapter.add(readMessage.split(Pattern.quote("*"))[1] + ":  "
											+ readMessage.split(Pattern.quote("*"))[2]);
								inserttomessages(readMessage.split(Pattern.quote("*"))[0], readMessage);
							}

							inserttodata(readMessage.split(Pattern.quote("*"))[0], readMessage,
									first_status_devices("connected"), "sent");

						}

					}

					// Log.d("chech", "message is"+readMessage);
					//
					// if (readMessage.equals("newtur")) {
					// oldmessage = "";
					// newmessage = "";
					// Log.d("chech", "newt");
					//
					// Log.d("chech", "messageread&new->myturnread=true");
					// myturnread = true;
					// sendm();
					// } else if (readMessage.equals("finish")) {
					// oldmessage = "";
					// newmessage = "";
					// Log.d("chech",
					// "messageread&fin->myturnwrite=true&mchatservice.start");
					// Log.d("chech", "fin");
					// myturnwrite = true;
					// mChatService.start();
					// }
					//
					// else {
					//
					// if (readMessage.contains("newtur")) {
					// oldmessage +=
					// readMessage.split(Pattern.quote("newtur"))[0];
					// Log.d("chech", "contnewt");
					// newmessage = "newtur";
					// } else if (readMessage.contains("finish")) {
					// oldmessage +=
					// readMessage.split(Pattern.quote("finish"))[0];
					// Log.d("chech", "contfin");
					// newmessage = "finish";
					// } else {
					// oldmessage += readMessage;
					// }
					// Log.d("chech", "old is"+oldmessage);
					// if (oldmessage.contains("*end"))
					// {
					// readMessage = oldmessage.split("\\*end")[0];
					// Log.d("chech", "endend");
					// if (oldmessage.split("\\*end").length > 1) {
					// Log.d("chech", "forend");
					// String temp = "";
					// for (int i = 1; i < oldmessage.split("\\*end").length;
					// i++) {
					// temp += oldmessage.split("\\*end")[i];
					// }
					// oldmessage = temp;
					// } else {
					// oldmessage = "";
					//
					// }
					// Log.d("chech", "read is"+readMessage);
					// if
					// (!id_if_exist_messages(readMessage.split(Pattern.quote("*"))[0]))
					// {
					//
					// Log.d("check", "read message " + readMessage);
					// if (readMessage.split(Pattern.quote("*"))[1].charAt(0) ==
					// '&'
					// && (decode(readMessage.split(Pattern.quote("&"))[1],
					// mBluetoothAdapter.getAddress())
					// .equals(mBluetoothAdapter.getAddress()))) {
					// mConversationArrayAdapter.add(readMessage.split(Pattern.quote(";"))[0]
					// + ": "
					// + decode(readMessage.split(Pattern.quote("&"))[2],
					// mBluetoothAdapter.getAddress())
					// + "(private message)");
					//
					// } else {
					// if (readMessage.split(Pattern.quote("*"))[1].charAt(0) !=
					// '&')
					// mConversationArrayAdapter.add(readMessage.split(Pattern.quote(";"))[0]
					// + ": "
					// + readMessage.split(Pattern.quote("*"))[1]);
					// inserttomessages(readMessage.split(Pattern.quote("*"))[0],
					// readMessage);
					// }
					//
					// inserttodata(readMessage.split(Pattern.quote("*"))[0],
					// readMessage,
					// first_status_devices("connected"), "sent");
					//
					// }
					// }
					// if (newmessage.equals("newtur")) {
					// newmessage = "";
					// Log.d("chech", "messageread&new->myturnread=true");
					// myturnread = true;
					// sendm();
					// }
					// if (newmessage.equals("finish")) {
					// newmessage = "";
					// Log.d("chech",
					// "messageread&fin->myturnwrite=true&mchatservice.start");
					// myturnwrite = true;
					// mChatService.start();
					// }
					//
					// }

					break;
				case MESSAGE_DEVICE_NAME:
					if (timer != null)
						timer.cleantimer();

					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					if (id_if_exist_devices(mConnectedDeviceName) == false) {
						inserttodevices(mConnectedDeviceName, "connected");
					} else {
						change_id_status_devices(mConnectedDeviceName, "connected");
					}

					Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT)
							.show();
					break;
				case MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
					break;
				}
			if (timer!=null) {
				timer.reume();
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (timer != null)
		{
			timer.cleantimer();
		
			timer.reume();
		}
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK && mode.equals("singel")) {
				// if(data!=null &&
				// data.hasExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS) &&
				// mode=="singel" )
				/*
				 * autosendallmessages = false; autostartmessaging = false;
				 * autoconnecttodevices = false; automovetonextdevice = false;
				 * autoresumemessaging = false; myturnwrite = true;
				 * 
				 * // data. Log.d("c", mode);
				 */
				mode = "singel";
				msendaddr = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				mTitle.setText("secure to : " + msendaddr);
				// Get the BLuetoothDevice object

				// Attempt to connect to the device
				// connectDevice(data, false);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			mode = "multi";
			if (resultCode == Activity.RESULT_OK && mode.equals("multi")) {
				autosendallmessages = true;
				autostartmessaging = true;
				autoconnecttodevices = true;
				automovetonextdevice = true;
				autoresumemessaging = true;
				getdevicesname(data, false);
			}
			break;
		case REQUEST_Discovareble:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				Log.d(TAG, "discobable bt");
				ensure = true;
			}
			break;
		case REQUEST_ENABLE_BT:

			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				Log.d(TAG, "eenable bt");
				ensure = true;
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		if (data.hasExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS)) {
			String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

			// Get the BLuetoothDevice object
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			// Attempt to connect to the device
			mChatService.connect(device, secure);
		}
	}

	private void connectDevice2() {
		Log.d("check", "before info");
		String info = first_status_devices("new");
		Log.d("check", "after info");
		if (!info.equals("")) {

			Log.d("check", "info	" + info);

			// Get the device MAC address
			String address = info.split(Pattern.quote(";"))[0];
			Log.d("check", "adress	" + address);
			// Get the BLuetoothDevice object
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			// Attempt to connect to the device
			mChatService.connect(device, false);
		} else {
			Log.d("er", "no new found");
		}
	}

	private void getdevicesname(Intent data, boolean secure) {

		ArrayList<String> s = data.getStringArrayListExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		if (autoresumemessaging) {
			// change_first_status_devices("stop", "new");
			create_tabel_devices();
		}
		for (String string : s) {
			if (id_if_exist_devices(string) == false)
				inserttodevices(string, "new");

		}

		if (autoconnecttodevices) {
			Log.d("chech", "getdevicesname->myturnwrite=true&conectdevice2");
			myturnwrite = true;
			connectDevice2();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (timer != null)
		{
			timer.cleantimer();
	
			timer.pause();
		}
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.secure_connect_scan:
			mode = "singel";
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.insecure_connect_scan:
			mode = "multi";
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

}
