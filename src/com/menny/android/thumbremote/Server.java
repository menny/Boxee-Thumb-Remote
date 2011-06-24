/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.thumbremote;

import java.net.InetAddress;

/**
 * Holds information about a Boxee server which announced itself in response to
 * a discovery request.
 */
public class Server {
	private String mType;
	private String mName;
	private boolean mAuthRequired;
	private int mPort;
	private InetAddress mAddr;

	public Server(String type, String name, boolean authRequired, InetAddress address, int port) {
		mAddr = address;
		mPort = port;
		mType = type;
		mName = name;
		mAuthRequired = authRequired;
	}

	public boolean valid() {
		return mPort > 0 && mAddr != null;
	}

	public String type() {
		return mType;
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
