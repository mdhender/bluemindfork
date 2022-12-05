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
package net.bluemind.cli.node;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.server.api.Server;

public class CpuFlagStatusProvider implements IStatusProvider {

	@Override
	public void report(CliContext ctx, ItemValue<Server> srv, INodeClient nc) {
		CompletableFuture<Void> comp = new CompletableFuture<>();
		nc.asyncExecute(ExecRequest.anonymous("cat /proc/cpuinfo"), new ProcessHandler() {

			boolean seenFlags = false;
			Splitter flagsSplit = Splitter.on(' ');

			@Override
			public void starting(String taskRef) {
				// yeah
			}

			/**
			 * 
			 * <code>
			 * flags		: fpu vme de pse tsc msr pae mce cx8 apic sep mtrr \
			 * pge mca cmov pat pse36 clflush mmx fxsr sse sse2 ss syscall nx pdpe1gb rdtscp lm constant_tsc arch_perfmon nopl xtopology \
			 * tsc_reliable nonstop_tsc cpuid tsc_known_freq pni pclmulqdq ssse3 fma cx16 pcid sse4_1 sse4_2 \
			 * x2apic movbe popcnt tsc_deadline_timer aes xsave avx f16c rdrand hypervisor lahf_lm abm 3dnowprefetch \
			 * cpuid_fault invpcid_single pti ssbd ibrs ibpb stibp fsgsbase \
			 * tsc_adjust bmi1 avx2 smep bmi2 invpcid rdseed adx smap clflushopt xsaveopt \
			 * xsavec xsaves arat md_clear flush_l1d arch_capabilities
			 * </code>
			 * 
			 *
			 */
			@Override
			public void log(String l, boolean isContinued) {
				if (l != null && l.startsWith("flags") && !seenFlags) {
					int idx = l.indexOf(": ");
					if (idx > 0) {
						String justFlags = l.substring(idx + 2);
						Set<String> flagsSet = flagsSplit.splitToStream(justFlags).collect(Collectors.toSet());
						warnOnInsufficientCpuCapabilities(ctx, srv, flagsSet);
						seenFlags = true;
					}
				}

			}

			@Override
			public void completed(int exitCode) {
				comp.complete(null);
			}
		});
		comp.join();
	}

	private void warnOnInsufficientCpuCapabilities(CliContext ctx, ItemValue<Server> srv, Set<String> flagsSet) {
		if (!flagsSet.contains("aes")) {
			// java ssl has intrinsics to use that (eg. for node ssl connections)
			ctx.warn("[{}] CPU is missing AES flag and will provide poor SSL performance", srv.value.address());
			if (flagsSet.contains("hypervisor")) {
				ctx.warn("[{}] If your virtualization host can expose better CPU flags, performance will improve",
						srv.value.address());
			}
		}

	}

}
