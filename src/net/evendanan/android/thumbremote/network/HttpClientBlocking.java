///* The following code was written by Menny Even Danan
// * and is released under the APACHE 2.0 license
// * 
// * http://www.apache.org/licenses/LICENSE-2.0
// */
//package net.evendanan.android.thumbremote.network;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.URI;
//import java.net.URISyntaxException;
//
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.auth.AuthScope;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.CredentialsProvider;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.BasicCredentialsProvider;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.params.CoreConnectionPNames;
//
//import android.text.TextUtils;
//import android.util.Log;
//
///**
// * Wraps Android's HttpClient/HttpGet objsts
// */
//class HttpClientBlocking implements HttpBlocking {
//	private static final String TAG = "HttpClientBlocking";
//	private final URI mUrl;
//	private final String mUser;
//	private final String mPassword;
//	private final int mTimeout;
//	
//	private DefaultHttpClient mHttpClient = null;
//	
//	HttpClientBlocking(String url, int timeout, String user, String password) throws URISyntaxException {
//		mUrl = new URI(url);
//		mUser = user;
//		mPassword = password;
//		mTimeout = timeout;
//	}
//
//	/**
//	 * Perform the blocking fetch.
//	 * @throws IOException 
//	 */
//	public synchronized Response fetch() throws IOException {
//		if (mUrl == null)
//			return new Response(false, null);
//
//		Log.d(TAG, "Fetching " + mUrl.toString());
//		HttpGet request = new HttpGet(mUrl);
//		
//		request.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, new Integer(mTimeout));
//    	request.getParams().setParameter(CoreConnectionPNames.SO_LINGER, new Integer(mTimeout/2));
//    	request.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, new Integer(mTimeout));
//    	
//    	if (mHttpClient == null)
//    	{
//    		mHttpClient = new DefaultHttpClient();
//    		addAuthentication(mHttpClient);
//    	}
//    	
//    	InputStream instream = null;
//        try {
//        	HttpResponse httpResponse = mHttpClient.execute(request);
//            int responseCode = httpResponse.getStatusLine().getStatusCode();
//            String response = "";
//            HttpEntity entity = httpResponse.getEntity();
//
//            if (entity != null) {
//
//                instream = entity.getContent();
//                response = convertStreamToString(instream);
//            }
//
//            return new Response(responseCode >= 200 && responseCode < 300, response);
//        } catch (ClientProtocolException e)  {
//        	mHttpClient.getConnectionManager().shutdown();
//        	mHttpClient = null;
//        	e.printStackTrace();
//            throw e;
//        } catch (IOException e) {
//        	mHttpClient.getConnectionManager().shutdown();
//        	mHttpClient = null;
//        	e.printStackTrace();
//            throw e;
//        }/*
//        finally
//        {
//            mHttpClient.getConnectionManager().shutdown();
//        }*/
//	}
//
//    private static String convertStreamToString(InputStream is) {
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        StringBuilder sb = new StringBuilder();
//        
//        String NL = System.getProperty("line.separator");
//        String line = null;
//        try {
//            while ((line = reader.readLine()) != null) {
//                sb.append(line);
//                sb.append(NL);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } /*finally {
//            try {
//                is.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }*/
//        return sb.toString();
//    }
//
//	private void addAuthentication(DefaultHttpClient client) {
//		if (!TextUtils.isEmpty(mPassword)) {
//			CredentialsProvider credProvider = new BasicCredentialsProvider();
//		    credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), 
//		    		new UsernamePasswordCredentials(mUser, mPassword));
//		    client.setCredentialsProvider(credProvider);
//		}
//	}
//}