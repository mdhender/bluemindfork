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
package net.bluemind.imap.endpoint.exec;

import org.slf4j.helpers.MessageFormatter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.NamespaceCommand;
import net.bluemind.imap.endpoint.driver.NamespaceInfos;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.lib.vertx.Result;

public class NamespaceProcessor implements CommandProcessor<NamespaceCommand> {

	private static final String NS_RESP = """
			* NAMESPACE (("" "/")) (("{}" "/")) (("{}" "/"))\r
			{} OK Completed\r
			""";

	/**
	 * <code>* NAMESPACE (("" "/")) (("Autres utilisateurs/" "/")) (("Dossiers partag&AOk-s/" "/"))</code>
	 */
	@Override
	public void operation(NamespaceCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		NamespaceInfos infos = ctx.mailbox().namespaces();
		ctx.write(format(NS_RESP, UTF7Converter.encode(infos.otherUsers()),
				UTF7Converter.encode(infos.sharedMailboxes()), command.raw().tag()));
		completed.handle(Result.success());
	}

	private String format(String pattern, Object... args) {
		return MessageFormatter.arrayFormat(pattern, args).getMessage();
	}

	@Override
	public Class<NamespaceCommand> handledType() {
		return NamespaceCommand.class;
	}

}
