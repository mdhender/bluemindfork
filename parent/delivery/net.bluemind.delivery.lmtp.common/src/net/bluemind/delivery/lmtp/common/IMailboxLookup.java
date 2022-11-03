package net.bluemind.delivery.lmtp.common;

public interface IMailboxLookup {
	ResolvedBox lookupEmail(String recipient);
}
