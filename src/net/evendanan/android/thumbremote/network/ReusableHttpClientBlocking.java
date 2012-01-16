/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.evendanan.android.thumbremote.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import android.text.TextUtils;
import android.util.Log;

/**
 * Wraps Android's HttpClient/HttpGet objects
 */
class ReusableHttpClientBlocking implements HttpBlocking {
	private static final String TAG = "ReusableHttpClientBlocking";
	
	private final DefaultHttpClient mHttpClient;
	private final HttpGet mRequest;
	
	ReusableHttpClientBlocking(int timeout, String user, String password) {
		Log.d(TAG, "Creating a new HTTP client");
		mHttpClient = new DefaultHttpClient();
		if (!TextUtils.isEmpty(password)) {
			CredentialsProvider credProvider = new BasicCredentialsProvider();
		    credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), 
		    		new UsernamePasswordCredentials(user, password));
		    mHttpClient.setCredentialsProvider(credProvider);
		}
		mRequest = new HttpGet();
		mRequest.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, new Integer(timeout));
		mRequest.getParams().setParameter(CoreConnectionPNames.SO_LINGER, new Integer(timeout/2));
		mRequest.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, new Integer(timeout));
	}

	/**
	 * Perform the blocking fetch.
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	public synchronized Response fetch(String url) throws IOException, URISyntaxException {
		if (url == null)
			return new Response(false, 404, null);

		URI uri = new URI(url);
		mRequest.setURI(uri);
		Log.d(TAG, "Fetching " + url);
		
    	HttpResponse httpResponse = mHttpClient.execute(mRequest);
        int responseCode = httpResponse.getStatusLine().getStatusCode();
        String response = "";
        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {

        	InputStream instream = entity.getContent();
            response = convertStreamToString(instream);
        }

        boolean success = responseCode >= 200 && responseCode < 300;
        if (!success)
        {
        	Log.w(TAG, "Got error response code "+responseCode);
        	return new Response(false, responseCode, httpResponse.getStatusLine().getReasonPhrase());
        }
        
        return new Response(true, responseCode, response);
	}
	
	@Override
	public synchronized void close() {
		mHttpClient.getConnectionManager().shutdown();
	}

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        
        String NL = System.getProperty("line.separator");
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(NL);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}