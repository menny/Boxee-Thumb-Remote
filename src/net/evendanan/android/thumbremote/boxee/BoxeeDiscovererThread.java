/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.evendanan.android.thumbremote.boxee;

import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import net.evendanan.android.thumbremote.ServerAddress;

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

public class BoxeeDiscovererThread extends Thread {
	private static final String TAG = "Discoverer";

	private static final String REMOTE_KEY = "b0xeeRem0tE!";
	private static final int DISCOVERY_PORT = 2562;
	private static final int TIMEOUT_MS = 750;
	private Receiver mReceiver;

	// TODO: Vary the challenge, or it's not much of a challenge :)
	private static final String mChallenge = "myvoice";
	private WifiManager mWifi;
	private boolean mListening = true;

	private DatagramSocket mSocket;

	public interface Receiver {
		/**
		 * Process the list of discovered servers. This is always called once
		 * after a short timeout.
		 * 
		 * @param servers
		 *            list of discovered servers, null on error
		 */
		void addAnnouncedServers(ArrayList<ServerAddress> servers);
	}

	public BoxeeDiscovererThread(Context context, Receiver receiver) {
		mWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mReceiver = receiver;
	}
	
	public void setReceiver(Receiver r)
	{
		mReceiver = r;
		if (mReceiver == null && mSocket != null) mSocket.close();
	}

	public void run() {
		ArrayList<ServerAddress> servers = null;
		mSocket = null;
		try {
			mSocket = new DatagramSocket(DISCOVERY_PORT);
			//we could have two or more discoverers at the same time.
			mSocket.setReuseAddress(true);
			mSocket.setBroadcast(true);
			mSocket.setSoTimeout(TIMEOUT_MS);

			sendDiscoveryRequest(mSocket);
			servers = listenForResponses(mSocket);
		} catch (IOException e) {
			servers = new ArrayList<ServerAddress>(); // use an empty one
			Log.e(TAG, "Could not send discovery request", e);
		}
		finally
		{
			Receiver r = mReceiver;
			if (r != null)
				r.addAnnouncedServers(servers);
			mListening = false;
			if (mSocket != null) mSocket.close();
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
	private ArrayList<ServerAddress> listenForResponses(DatagramSocket socket)
			throws IOException {
		long start = System.currentTimeMillis();
		byte[] buf = new byte[10240];
		ArrayList<ServerAddress> servers = new ArrayList<ServerAddress>();

		// Loop and try to receive responses until the timeout elapses. We'll
		// get
		// back the packet we just sent out, which isn't terribly helpful, but
		// we'll
		// discard it in parseResponse because the cmd is wrong.
		try {
			while (mReceiver != null) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				Log.d(TAG, "Waiting for discovery response...");
				socket.receive(packet);
				String s = new String(packet.getData(), packet.getOffset(), packet.getLength());
				Log.d(TAG, "Packet received after "+ (System.currentTimeMillis() - start) + " " + s);
				InetAddress sourceAddress = packet.getAddress();
				Log.d(TAG, "Parsing response...");
				ServerAddress server = parseResponse(s, sourceAddress
						/*((InetSocketAddress) packet.getSocketAddress())
								.getAddress()*/);
				if (server != null)
					servers.add(server);
			}
		} catch (SocketTimeoutException e) {
			Log.w(TAG, "Receive timed out");
		}
		catch(Exception e)
		{
			Log.w(TAG, "Failed to create a Server object from discovery response! Error: "+e.getMessage());
			e.printStackTrace();
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
	private ServerAddress parseResponse(String response, InetAddress address) {

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

		ServerAddress server = new ServerAddress("Boxee", 
				values.get("version"), values.get("name"),
				"true".equalsIgnoreCase(values.get("httpAuthRequired")),
				address, Integer.parseInt(values.get("httpPort")));
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
						Log.d(TAG, String.format("BDP1 k: '%s', v: '%s'", key, value));
						values.put(key, value);
					}
					
					return values;
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

	public boolean isDiscoverying() {
		return mListening && mReceiver != null;
	}
}
