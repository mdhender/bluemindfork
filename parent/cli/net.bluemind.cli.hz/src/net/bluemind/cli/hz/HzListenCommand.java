/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.hz;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.hornetq.client.Consumer;
import net.bluemind.hornetq.client.MQ;

@Command(name = "listen", description = "Listen to message(s) on a topic")
public class HzListenCommand extends AbstractHzOperation {

	public static class Reg extends HzReg {

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return HzListenCommand.class;
		}

	}

	@Arguments(required = true, description = "hazelcast topic")
	public String topic;

	@Override
	protected void connectedOperation() {
		Consumer reg = MQ.registerConsumer(topic, msg -> {
			ctx.info(msg.toJson().encodePrettily());
		});

		Runtime.getRuntime().addShutdownHook(new Thread("clean-up") {
			@Override
			public void run() {
				reg.close();
				ctx.info("Closing consumer on " + topic);
			}
		});

		synchronized (this) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
