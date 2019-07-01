package net.bluemind.dav.server.proto;

public interface IProtocolFactory<Q, R> {

	IDavProtocol<Q, R> getProtocol();

	String getExecutorAddress();

}
