package net.bluemind.dav.server.proto;

public final class ProtocolFactory<Q, R> implements IProtocolFactory<Q, R> {

	private final IDavProtocol<Q, R> proto;
	private final String addr;

	public ProtocolFactory(IDavProtocol<Q, R> proto) {
		this.proto = proto;
		this.addr = proto.getClass().getSimpleName().toLowerCase() + ".executor";
	}

	@Override
	public IDavProtocol<Q, R> getProtocol() {
		return proto;
	}

	@Override
	public String getExecutorAddress() {
		return addr;
	}

}
