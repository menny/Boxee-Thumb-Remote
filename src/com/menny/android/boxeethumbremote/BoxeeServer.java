package com.menny.android.boxeethumbremote;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import android.content.Intent;

/**
 * Holds information about a Boxee server which announced itself in response to
 * a discovery request.
 */
class BoxeeServer {
	private String mVersion;
	private String mName;
	private boolean mAuthRequired;
	private int mPort;
	private InetAddress mAddr;

	/**
	 * Tries to create a BoxeeServer from arguments passed in an intent.
	 * 
	 * @param data
	 */
	public BoxeeServer(Intent data) {
		mName = data.getStringExtra(Settings.SERVER_NAME_KEY);
		mPort = data.getIntExtra(Settings.PORT_KEY, Remote.BAD_PORT);
		mAuthRequired = data.getBooleanExtra(Settings.AUTH_REQUIRED_KEY, false);

		try {
			mAddr = InetAddress.getByName(data
					.getStringExtra(Settings.HOST_KEY));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Tries to create a BoxeeServer from arguments created from Boxee
	 * autodiscovery algorithm.
	 * 
	 * @param attributes
	 * @param address
	 */
	public BoxeeServer(HashMap<String, String> attributes, InetAddress address) {
		mAddr = address;
		mVersion = attributes.get("version");
		mName = attributes.get("name");
		try {
			mPort = Integer.parseInt(attributes.get("httpPort"));
		} catch (NumberFormatException e) {
			mPort = Remote.BAD_PORT;
		}

		String auth = attributes.get("httpAuthRequired");
		mAuthRequired = auth != null && auth.equals("true");
	}

	/**
	 * Creates a BoxeeServer given specific parameters.
	 * @param name
	 * @param address
	 * @param port
	 * @param authRequired
	 */
	public BoxeeServer(String name, String address, int port,
			boolean authRequired) {
		mName = name;
		mPort = port;
		mAuthRequired = authRequired;

		try {
			mAddr = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean valid() {
		return mPort != Remote.BAD_PORT && mAddr != null;
	}

	public String version() {
		return mVersion;
	}

	public String name() {
		return mName;
	}

	public boolean authRequired() {
		return mAuthRequired;
	}

	public int port() {
		return mPort;
	}

	public InetAddress address() {
		return mAddr;
	}

	public String toString() {
		return String.format("%s at %s:%d %s", mName, mAddr.getHostAddress(),
				mPort, valid() ? "" : "(broken?)");
	}
}
