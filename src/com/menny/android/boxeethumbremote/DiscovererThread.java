/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.boxeethumbremote;

import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Code for dealing with Boxee server discovery. This class tries to send a
 * broadcast UDP packet over your wifi network to discover the boxee service.
 */

public class DiscovererThread extends Thread {
	private static final String TAG = "Discoverer";

	private static final String REMOTE_KEY = "b0xeeRem0tE!";
	private static final int DISCOVERY_PORT = 2562;
	private static final int TIMEOUT_MS = 750;
	private Receiver mReceiver;

	// TODO: Vary the challenge, or it's not much of a challenge :)
	private static final String mChallenge = "myvoice";
	private WifiManager mWifi;
	private boolean mListening = true;

	interface Receiver {
		/**
		 * Process the list of discovered servers. This is always called once
		 * after a short timeout.
		 * 
		 * @param servers
		 *            list of discovered servers, null on error
		 */
		void addAnnouncedServers(ArrayList<BoxeeServer> servers);
	}

	DiscovererThread(Context context, Receiver receiver) {
		mWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mReceiver = receiver;
	}

	public void run() {
		ArrayList<BoxeeServer> servers = null;
		try {
			DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT);
			socket.setBroadcast(true);
			socket.setSoTimeout(TIMEOUT_MS);

			sendDiscoveryRequest(socket);
			servers = listenForResponses(socket);
			socket.close();
		} catch (IOException e) {
			servers = new ArrayList<BoxeeServer>(); // use an empty one
			Log.e(TAG, "Could not send discovery request", e);
		}
		finally
		{
			mReceiver.addAnnouncedServers(servers);
			mListening = false;
		}
	}

	/**
	 * Send a broadcast UDP packet containing a request for boxee services to
	 * announce themselves.
	 * 
	 * @throws IOException
	 */
	private void sendDiscoveryRequest(DatagramSocket socket) throws IOException {
		String formatString = "<bdp1 cmd=\"discover\" application=\"iphone_remote\" challenge=\"%s\" signature=\"%s\"/>";
		String signature = getSignature(mChallenge);
		String data = String.format(formatString, mChallenge, signature);
		Log.d(TAG, "Sending data " + data);
		DatagramPacket packet = new DatagramPacket(data.getBytes(), data
				.length(), getBroadcastAddress(), DISCOVERY_PORT);
		socket.send(packet);
	}

	/**
	 * Calculate the broadcast IP we need to send the packet along. If we send
	 * it to 255.255.255.255, it never gets sent. I guess this has something to
	 * do with the mobile network not wanting to do broadcast.
	 */
	public InetAddress getBroadcastAddress() throws IOException {
		DhcpInfo dhcp = mWifi.getDhcpInfo();
		if (dhcp == null) {
			Log.d(TAG, "Could not get dhcp info");
			return null;
		}

		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);

		return InetAddress.getByAddress(quads);
	}

	/**
	 * Listen on socket for responses, timing out after TIMEOUT_MS
	 * 
	 * @param socket
	 *            socket on which the announcement request was sent
	 * @return list of discovered servers, never null
	 * @throws IOException
	 */
	private ArrayList<BoxeeServer> listenForResponses(DatagramSocket socket)
			throws IOException {
		long start = System.currentTimeMillis();
		byte[] buf = new byte[1024];
		ArrayList<BoxeeServer> servers = new ArrayList<BoxeeServer>();

		// Loop and try to receive responses until the timeout elapses. We'll
		// get
		// back the packet we just sent out, which isn't terribly helpful, but
		// we'll
		// discard it in parseResponse because the cmd is wrong.
		try {
			while (true) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				String s = new String(packet.getData(), 0, packet.getLength());
				Log.d(TAG, "Packet received after "+ (System.currentTimeMillis() - start) + " " + s);
				BoxeeServer server = parseResponse(s,
						((InetSocketAddress) packet.getSocketAddress())
								.getAddress());
				if (server != null)
					servers.add(server);
			}
		} catch (SocketTimeoutException e) {
			Log.d(TAG, "Receive timed out");
		}
		return servers;
	}

	/**
	 * Parses an xml response. Expected response:
	 * 
	 * <?xml version="1.0"?> <BDP1 cmd="found" application="boxee"
	 * version="<version>" name="<host name>" response="<randomdigits>"
	 * httpPort="<port_nubmer>" httpAuthRequired="<true|false>"
	 * signature="<MD5HEX(randomdigits+shared_key)>"/>
	 */
	private BoxeeServer parseResponse(String response, InetAddress address) {

		HashMap<String, String> values = parseBDP1Xml(response);
		if (values == null)
			return null;

		// Validate "response" and "signature"
		String challenge_response = values.get("response");
		String signature = values.get("signature");
		if (challenge_response != null && signature != null) {
			String legit_response = getSignature(challenge_response)
					.toLowerCase();
			if (!legit_response.equals(signature.toLowerCase())) {
				Log.e(TAG, "Signature verification failed " + legit_response
						+ " vs " + signature);
				return null;
			}
		}

		// Validate "cmd"
		String cmd = values.get("cmd");
		if (cmd == null || !cmd.equals("found")) {
			// We'll get the discovery packet we sent out, where cmd="discovery"
			Log.e(TAG, "Bad cmd " + response);
			return null;
		}

		// Validate "application"
		String app = values.get("application");
		if (app == null || !app.equals("boxee")) {
			Log.e(TAG, "Bad app " + app);
			return null;
		}

		BoxeeServer server = new BoxeeServer(values, address);
		Log.d(TAG, "Discovered server " + server);
		return server;
	}

	private HashMap<String, String> parseBDP1Xml(String xml) {
		final HashMap<String, String> values = new HashMap<String, String>();

		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser xpp = factory.newPullParser();
			xpp.setInput(new StringReader(xml));

			int eventType = xpp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {

				if (eventType == XmlPullParser.START_TAG
						&& xpp.getName().equals("BDP1")) {

					for (int i = 0; i < xpp.getAttributeCount(); i++) {
						String key = xpp.getAttributeName(i);
						String value = xpp.getAttributeValue(i);
						values.put(key, value);
					}
				}

				eventType = xpp.next();
			}
		} catch (XmlPullParserException e) {
			Log.e(TAG, "xml error: " + e);
			return null;
		} catch (IOException e) {
			Log.e(TAG, "IOException: " + e);
			return null;
		}

		return values;
	}

	/**
	 * Calculate the signature we need to send with the request. It is a string
	 * containing the hex md5sum of the challenge and REMOTE_KEY.
	 * 
	 * @return signature string
	 */
	private String getSignature(String challenge) {
		MessageDigest digest;
		byte[] md5sum = null;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(challenge.getBytes());
			digest.update(REMOTE_KEY.getBytes());
			md5sum = digest.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		StringBuffer hexString = new StringBuffer();
		for (int k = 0; k < md5sum.length; ++k) {
			String s = Integer.toHexString((int) md5sum[k] & 0xFF);
			if (s.length() == 1)
				hexString.append('0');
			hexString.append(s);
		}
		return hexString.toString();
	}

	public boolean isLookingForServers() {
		return mListening;
	}
}
