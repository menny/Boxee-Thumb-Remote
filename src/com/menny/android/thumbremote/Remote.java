package com.menny.android.thumbremote;

public interface Remote {

	public interface ErrorHandler {
		public void ShowError(int id, boolean longDelay);
		public void ShowError(String s, boolean longDelay);
	}

	/**
	 * Tell boxee to change the volume. This involves doing a getVolume request,
	 * then a setVolume request.
	 * 
	 * @param percent
	 *            percent increase/decrease in volume
	 */
	public abstract void changeVolume(final int percent);

	/**
	 * Return the HTTP request prefix for sending boxee a command.
	 * 
	 * @return URL to send to boxee, up to but not including the boxee command
	 */
	public abstract String getRequestPrefix();

	public abstract String getRequestString(String command);

	public abstract void up();

	public abstract void down();

	public abstract void left();

	public abstract void right();

	public abstract void back();

	public abstract void select();

	public abstract void sendBackspace();

	public abstract void keypress(int unicode);

	public abstract void flipPlayPause();

	public abstract void stop();

	public abstract void seek(double pct);

	public abstract void setServer(Server server);

	public abstract void setRequireWifi(boolean requireWifi);

	public abstract boolean hasServers();

}