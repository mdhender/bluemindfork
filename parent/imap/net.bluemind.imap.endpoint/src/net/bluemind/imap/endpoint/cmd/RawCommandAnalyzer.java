/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.common.vertx.contextlogging.ContextualData;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.SessionState;

public class RawCommandAnalyzer {

	private static final Logger logger = LoggerFactory.getLogger(RawCommandAnalyzer.class);

	public AnalyzedCommand analyze(ImapContext ctx, RawImapCommand raw) {
		if (ctx != null && ctx.state() == SessionState.IN_AUTH) {
			if (logger.isInfoEnabled()) {
				logger.info("{} raw.cmd: {}", ctx.state(), raw.cmd());
			}
			String mech = ContextualData.get("mech");
			switch (mech) {
			case "PLAIN":
				return new AuthenticatePlainCommand(raw);
			default:
				logger.warn("authentication mech {} is not supported.", mech);
				ctx.state(SessionState.NOT_AUTHENTICATED);
				return null;
			}
		}

		String cmd = raw.cmd().toLowerCase();
		if (cmd.isBlank() || cmd.isEmpty()) {
			return null;
		}
		char base = cmd.charAt(0);
		switch (base) {
		case 'a':
			if (cmd.startsWith("append ")) {
				return new AppendCommand(raw);
			} else if (cmd.startsWith("authenticate ")) {
				return new AuthenticateCommand(raw);
			}
			return null;
		case 'c':
			if (cmd.equals("capability")) {
				return new CapabilityCommand(raw);
			} else if (cmd.startsWith("create ")) {
				return new CreateCommand(raw);
			} else if (cmd.equals("check")) {
				return new NoopCommand(raw);
			} else if (cmd.equals("close")) {
				return new CloseCommand(raw);
			}
			return null;
		case 'd':
			if (cmd.equals("done")) {
				return new DoneCommand(raw);
			} else if (cmd.startsWith("delete ")) {
				return new DeleteCommand(raw);
			}
			return null;
		case 'e':
			if (cmd.startsWith("examine ")) {
				return new ExamineCommand(raw);
			} else if (cmd.startsWith("expunge")) {
				return new ExpungeCommand(raw);
			}
			return null;
		case 'f':
			if (cmd.startsWith("fetch ")) {
				return new FetchCommand(raw);
			}
			return null;
		case 'g':
			if (cmd.startsWith("getquotaroot ")) {
				return new GetQuotaRootCommand(raw);
			} else if (cmd.startsWith("getacl ")) {
				return new GetAclCommand(raw);
			}
			return null;
		case 'i':
			if (cmd.startsWith("id ")) {
				return new IdCommand(raw);
			} else if (cmd.equals("idle")) {
				return new IdleCommand(raw);
			}
			return null;
		case 'l':
			if (cmd.startsWith("login ")) {
				return new LoginCommand(raw);
			} else if (cmd.startsWith("list ")) {
				return new ListCommand(raw);
			} else if (cmd.startsWith("lsub ")) {
				return new LsubCommand(raw);
			} else if (cmd.equals("logout")) {
				return new LogoutCommand(raw);
			}
			return null;
		case 'm':
			if (cmd.startsWith("myrights ")) {
				return new MyRightsCommand(raw);
			}
			return null;
		case 'n':
			if (cmd.equals("noop")) {
				return new NoopCommand(raw);
			} else if (cmd.equals("namespace")) {
				return new NamespaceCommand(raw);
			}
			return null;
		case 'r':
			if (cmd.startsWith("rename ")) {
				return new RenameCommand(raw);
			}
			return null;
		case 's':
			if (cmd.startsWith("select ")) {
				return new SelectCommand(raw);
			} else if (cmd.startsWith("status ")) {
				return new StatusCommand(raw);
			} else if (cmd.startsWith("store ")) {
				return new StoreCommand(raw);
			} else if (cmd.startsWith("subscribe ")) {
				return new SubscribeCommand(raw);
			} else if (cmd.startsWith("search ")) {
				return new SearchCommand(raw);
			}
			return null;
		case 'u':
			if (cmd.startsWith("uid fetch ")) {
				return new UidFetchCommand(raw);
			} else if (cmd.startsWith("uid store ")) {
				return new UidStoreCommand(raw);
			} else if (cmd.startsWith("uid copy ")) {
				return new UidCopyCommand(raw);
			} else if (cmd.startsWith("uid search ")) {
				return new UidSearchCommand(raw);
			} else if (cmd.startsWith("uid expunge ")) {
				return new UidExpungeCommand(raw);
			} else if (cmd.startsWith("unsubscribe ")) {
				return new UnsubscribeCommand(raw);
			}
			return null;
		case 'x':
			if (cmd.startsWith("xlist ")) {
				return new XListCommand(raw);
			}
			return null;
		default:
			return null;
		}

	}

}
