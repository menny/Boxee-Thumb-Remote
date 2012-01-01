package net.evendanan.android.thumbremote.network;

import java.io.IOException;
import java.net.URISyntaxException;

interface HttpBlocking {
	Response fetch(String url) throws IOException, URISyntaxException;
	void close();
}
