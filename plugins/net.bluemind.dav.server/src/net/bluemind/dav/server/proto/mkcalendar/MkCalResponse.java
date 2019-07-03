package net.bluemind.dav.server.proto.mkcalendar;

public class MkCalResponse {

	private final String path;

	public MkCalResponse(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

}
