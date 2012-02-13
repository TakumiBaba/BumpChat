package com.bumptech.bumpchat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.bumpapi.BumpAPI;
import com.bumptech.bumpapi.BumpAPIListener;
import com.bumptech.bumpapi.BumpConnection;
import com.bumptech.bumpapi.BumpDisconnectReason;
import com.bumptech.bumpapi.BumpConnectFailedReason;
import com.bumptech.bumpchat.R;

public class BumpChat extends Activity implements BumpAPIListener {
	private String chat;
	private BumpConnection conn;
	private Button connect, send;
	private TextView message, chatView;

	private final Handler handler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		message = (TextView) findViewById(R.id.message);
		chatView = (TextView) findViewById(R.id.chat);
		connect = (Button) findViewById(R.id.connect_button);
		send = (Button) findViewById(R.id.send);
		send.setEnabled(false);

		chat = "";
	}

	public void onConnectButton(View v) {
		Intent bump = new Intent(this, BumpAPI.class);
		bump.putExtra(BumpAPI.EXTRA_USER_NAME, "Bump API User");
		bump.putExtra(BumpAPI.EXTRA_API_KEY, "dfe9601b8a184a73b98cba690c24b67b");
		startActivityForResult(bump, 0);
	}

	@Override
	public void onStop() {
		if (conn != null)
			conn.disconnect();

		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			conn = (BumpConnection) data.getParcelableExtra(BumpAPI.EXTRA_CONNECTION);
			conn.setListener(this, handler);
			updateChat("--- Successfully connected to " + conn.getOtherUserName()
			    + " ---");
			connect.setEnabled(false);
			send.setEnabled(true);
		} else {
			BumpConnectFailedReason reason = (BumpConnectFailedReason)data.getSerializableExtra(BumpAPI.EXTRA_REASON);
			updateChat("--- Failed to connect (" + reason.toString() + ")---");
		}
	}

	public void sendChat(View v) {
		String messageText = message.getText().toString();
		message.setText("");
		updateChat("You said: " + messageText);
		try {
			conn.send(messageText.getBytes("UTF-8"));
		} catch (Exception e) {
		}
	}

	private void updateChat(String s) {
		chat += s + "\n";

		chatView.setText(chat.subSequence(0, chat.length()));
	}

	@Override
	public void bumpDisconnect(BumpDisconnectReason reason) {
		switch (reason) {
			case END_OTHER_USER_QUIT:
				updateChat("--- " + conn.getOtherUserName() + " QUIT ---");
				break;
			case END_OTHER_USER_LOST:
				updateChat("--- " + conn.getOtherUserName() + " LOST ---");
				break;
		}

		connect.setEnabled(true);
		send.setEnabled(false);
	}

	@Override
	public void bumpDataReceived(byte[] chunk) {
		try {
			String data = new String(chunk, "UTF-8");
			updateChat(conn.getOtherUserName() + " said: " + data);
		} catch (Exception e) {
			Log.e("Bump Chat", "Failed to parse incoming data");
			e.printStackTrace();
		}
	}
}
