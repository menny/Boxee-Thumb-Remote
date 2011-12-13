/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.evendanan.android.thumbremote.ui;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import net.evendanan.android.thumbremote.R;
import net.evendanan.android.thumbremote.RemoteApplication;
import net.evendanan.android.thumbremote.ServerAddress;
import net.evendanan.android.thumbremote.boxee.BoxeeConnector;
import net.evendanan.android.thumbremote.boxee.BoxeeDiscovererThread;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

// See:
// http://android.git.kernel.org/?p=platform/packages/apps/Settings.git;a=blob;f=src/com/android/settings/wifi/WifiSettings.java;h=cac77e3251fde365f3e32463f4c44712fd0b1944;hb=HEAD

/**
 * Handles preference storage for BoxeeRemote.
 */
public class SettingsActivity extends PreferenceActivity implements
		BoxeeDiscovererThread.Receiver,
		OnPreferenceClickListener, 
		OnSharedPreferenceChangeListener {
	
	private final Preference.OnPreferenceChangeListener numberCheckListener = new Preference.OnPreferenceChangeListener() {

	    @Override
	    public boolean onPreferenceChange(Preference preference, Object newValue) {
	        //Check that the string is an integer.
	        return numberCheck(newValue);
	    }
	    
		private boolean numberCheck(Object newValue) {
		    if( !newValue.toString().equals("")  &&  newValue.toString().matches("\\d*") ) {
		        return true;
		    }
		    else {
		        Toast.makeText(SettingsActivity.this.getApplicationContext(), getResources().getString(R.string.is_an_invalid_number, newValue), Toast.LENGTH_SHORT).show();
		        return false;
		    }
		}
	};
	/**
	 * private constants
	 */
	private static final int DIALOG_CUSTOM = 1;

	private HashMap<String, ServerAddress> mServers;
	private PreferenceScreen mServersScreen;
	private Preference mCustomServerPreference;

	public SettingsActivity() {
		mServers = new HashMap<String, ServerAddress>();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.layout.preferences);

		mServersScreen = (PreferenceScreen) getPreferenceScreen().findPreference(getText(R.string.settings_key_servers_screen));
		
		mCustomServerPreference = new Preference(this);
		mCustomServerPreference.setTitle(getText(R.string.custom_server));
		mCustomServerPreference.setOrder(1000);
		mCustomServerPreference.setOnPreferenceClickListener(this);
		mServersScreen.addPreference(mCustomServerPreference);

		getPreferenceScreen().findPreference(getText(R.string.settings_key_network_timeout_key)).setOnPreferenceChangeListener(numberCheckListener);
	}

	@Override
	protected void onPause() {
		RemoteApplication.getConfig().unlisten(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		RemoteApplication.getConfig().listen(this);
		setServerNameInSummary();
	}

	public void setServerNameInSummary() {
		mServersScreen.setSummary(RemoteApplication.getConfig().getServerName());
		if (RemoteApplication.getConfig().isManuallySetServer())
			mCustomServerPreference.setSummary(RemoteApplication.getConfig().getServerName());
		else
			mCustomServerPreference.setSummary("");
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id != DIALOG_CUSTOM)
			return null;

		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.custom_host, (ViewGroup) findViewById(R.id.layoutCustomHost));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						String address = ((TextView) layout.findViewById(R.id.textAddress)).getText().toString();
						String portText = ((TextView) layout.findViewById(R.id.textPort)).getText().toString();
						
						setCustomServer(address, portText);
						
						dialog.dismiss();
					}
				});

		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setNeutralButton(R.string.clear_custom_server, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						setCustomServer("", "8800");
						
						dialog.dismiss();
					}
				});
		builder.setTitle(R.string.custom_server);
		builder.setIcon(R.drawable.app_icon);

		return builder.create();
	}
	
	protected void setCustomServer(String address, String portText) {
		if (TextUtils.isEmpty(address))
		{
			RemoteApplication.getConfig().putServer("",  "", "", 8800, "", false, false);
		}
		else
		{
			int port = !TextUtils.isEmpty(portText) && TextUtils.isDigitsOnly(portText)? Integer.parseInt(portText) : 8800;
		
			RemoteApplication.getConfig().putServer(BoxeeConnector.BOXEE_SERVER_TYPE, 
					BoxeeConnector.BOXEE_SERVER_VERSION_OLD, address, port, "custom", false, true);
		}
		
		setServerNameInSummary();
	}

	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
		if (id != DIALOG_CUSTOM)
			return;
		//"getHost()" has a possible netword call. Need to be done out of the UI thread 
		new AsyncTask<Void, Void, String>()
		{
			@Override
			protected String doInBackground(Void... params) {
				try
				{
					InetAddress hostname = RemoteApplication.getConfig().getHost();
					return hostname.getHostName();
				}
				catch(Exception e)
				{
					e.printStackTrace();
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(String hostname) {
				super.onPostExecute(hostname);
				
				if (hostname != null) 
					((TextView) dialog.findViewById(R.id.textAddress)).setText(hostname);
				else
					((TextView) dialog.findViewById(R.id.textAddress)).setText("");
				
				((TextView) dialog.findViewById(R.id.textPort)).setText(new Integer(RemoteApplication.getConfig().getPort()).toString());
				
			}
		}.execute();
	}

	@Override
	public void addAnnouncedServers(ArrayList<ServerAddress> servers) {
		mServersScreen.removeAll();
		mServersScreen.addPreference(mCustomServerPreference);
		//and now the one I discovered
		for (ServerAddress server : servers) {
			final String serverNameKey = server.name()+"@"+server.address().getHostName();
			Preference preference = new Preference(this);
			preference.setOrder(mServers.size());
			preference.setTitle(serverNameKey);
			preference.setOnPreferenceClickListener(this);
			mServersScreen.addPreference(preference);
			mServers.put(serverNameKey, server);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		ServerAddress server = mServers.get(preference.getTitle());

		if (server == null) {
			showDialog(DIALOG_CUSTOM);
			return true;
		}

		RemoteApplication.getConfig().putServer(server, false);
		mServersScreen.getDialog().dismiss();

		setServerNameInSummary();
		
		return true;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(RemoteApplication.getConfig().SERVER_NAME_KEY)) {
			String value = RemoteApplication.getConfig().getServerName();
			Toast.makeText(this.getApplicationContext(), "New server "+value, Toast.LENGTH_SHORT);
			setServerNameInSummary();
		}
	}

}
