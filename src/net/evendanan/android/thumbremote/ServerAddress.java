/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.evendanan.android.thumbremote;

import java.net.InetAddress;

/**
 * Holds information about a media server which announced itself in response to
 * a discovery request.
 */
public final class ServerAddress {
	private final String mType;
	private final String mVersion;
	private final String mName;
	private final boolean mAuthRequired;
	private final int mPort;
	private final InetAddress mAddr;

	public ServerAddress(String type, String version, String name, boolean authRequired, InetAddress address, int port) {
		mAddr = address;
		mVersion = version;
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
	
	public String version(){
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
