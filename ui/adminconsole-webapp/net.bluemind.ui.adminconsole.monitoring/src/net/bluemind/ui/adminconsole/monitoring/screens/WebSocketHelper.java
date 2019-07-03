package net.bluemind.ui.adminconsole.monitoring.screens;

public class WebSocketHelper {
	public static native WebSocket create(String url)/*-{
		return new WebSocket(url);
	}-*/;
}
