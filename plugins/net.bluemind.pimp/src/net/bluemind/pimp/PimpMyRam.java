package net.bluemind.pimp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import net.bluemind.pimp.impl.Rule;
import net.bluemind.pimp.impl.RulesBuilder;

public class PimpMyRam implements IApplication {
	private static final Logger logger = LoggerFactory.getLogger(PimpMyRam.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		pimpSysCtl();

		Rule[] rules = loadRules();
		printMemoryAllocation(rules);
		long totalMemMB = getTotalSystemMemory();
		int spareMb = configureSpareMemory(rules, totalMemMB);
		configureProductMemory(rules, spareMb);

		pimpPostgresql(totalMemMB);

		System.exit(0);
		return IApplication.EXIT_OK;
	}

	private void pimpSysCtl() {
		try (InputStream in = PimpMyRam.class.getClassLoader().getResourceAsStream("data/sysctl/bm.conf")) {
			Files.write(ByteStreams.toByteArray(in), new File("/etc/sysctl.d/01-bluemind.conf"));

			int ret = SystemHelper.cmd("sysctl --system");
			if (ret != 0) {
				logger.warn("Loading sysctl ending with error code {}", ret);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void pimpPostgresql(long totalMemMB) {
		// this would be better if we include a file in the package
		boolean isShard = new File("/usr/share/doc/bm-mailbox-role/").isDirectory();
		if (totalMemMB > 63000) {
			writePg(isShard ? "mem.shard.64g" : "mem.64g");
		} else if (totalMemMB > 47000) {
			writePg(isShard ? "mem.shard.48g" : "mem.48g");
		} else if (totalMemMB > 31000) {
			writePg(isShard ? "mem.shard.32g" : "mem.32g");
		} else if (totalMemMB > 15000) {
			writePg(isShard ? "mem.shard.16g" : "mem.16g");
		} else {
			writePg("mem.default");
		}
	}

	private void writePg(String tplName) {
		try (InputStream in = PimpMyRam.class.getClassLoader().getResourceAsStream("data/pg/" + tplName);
				ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			bos.write(
					"# DO NOT MODIFY\n# OVERWRITTEN BY bm-pimp\n# use postgresql.conf.local for specific configuration\n"
							.getBytes());
			bos.write(ByteStreams.toByteArray(in));

			Files.write(bos.toByteArray(), new File("/etc/postgresql/16/main/postgresql.conf.pimp"));
			logger.info("PostgreSQL memory configured ({})", tplName);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private Rule[] loadRules() {
		return new RulesBuilder().build();
	}

	private void configureProductMemory(Rule[] rules, int spareMb) throws IOException {
		File parent = new File("/etc/bm/default");
		parent.mkdirs();
		int sparePercentAlloc = 0;
		for (Rule r : rules) {
			sparePercentAlloc += r.getSparePercent();
			if (!productEnabled(r.getProduct())) {
				logger.info("{} {}is not installed or disabled, not configuring.", r.getProduct(),
						r.isOptional() ? "(optional) " : "");
				continue;
			}
			File f = confPath(r);
			int fromSpare = spareMb / 100 * r.getSparePercent();
			// our stack size is at 256k so we divide by 4 to get MB
			int cpuBoostMb = r.getCpusBoost() * Runtime.getRuntime().availableProcessors() / 4;
			if (cpuBoostMb > 0) {
				logger.info("CPU boost is {}MB", cpuBoostMb);
			}
			int memMb = r.getDefaultHeap() + fromSpare + cpuBoostMb;
			int dmemMb = Math.min(r.getDirectCap(), r.getDefaultDirect() + fromSpare);
			String content = "MEM=" + memMb + "\nDMEM=" + dmemMb + "\n";

			logger.info("  * {} gets +{}MB for a total of {}MB", r.getProduct(), fromSpare, memMb);
			Files.write(content.getBytes(), f);

			// also write to the old location
			File oldDir = new File("/etc/" + r.getProduct() + "/");
			if (oldDir.mkdirs()) {
				File oldFile = new File(oldDir, "mem_conf.ini");
				if (!oldFile.exists()) {
					Files.write(content.getBytes(), oldFile);
				}
			}
		}
		logger.info("Spare percent allocation is set at {}% in rules.json", sparePercentAlloc);
	}

	private int configureSpareMemory(Rule[] rules, long totalMemMB) {
		// give 40% of memory above 6GB to our JVMs
		int spareMb = (int) ((totalMemMB - 6144) * 0.40);
		logger.info("{}MB initial spare.", spareMb);
		int reallocated = 0;
		for (Rule r : rules) {
			if (!productEnabled(r.getProduct()) && !r.isOptional()) {
				int realloc = r.getDefaultHeap() + ((int) (spareMb * r.getSparePercent() / 100.0));
				logger.info("Adding {}MB to spare because {} is missing or disabled", realloc, r.getProduct());
				reallocated += realloc;
			}
		}
		logger.info("{}MB Reallocated because of missing components", reallocated);
		spareMb += reallocated;
		if (spareMb > 0) {
			logger.info("{}MB will be distributed between JVMs", spareMb);
		} else {
			logger.warn("No spare memory to distribute to JVMs ({})", spareMb);
			System.exit(0);
		}
		return spareMb;
	}

	private long getTotalSystemMemory()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
		Method totalMem = osMxBean.getClass().getMethod("getTotalPhysicalMemorySize");
		long totalMemMB = 4096;
		if (totalMem != null) {
			totalMem.setAccessible(true);
			totalMemMB = ((Long) totalMem.invoke(osMxBean)) / 1024 / 1024;
			logger.info("Total from JMX: {}MB", totalMemMB);
		} else {
			logger.error("Cannot figure out physical memory size");
			System.exit(1);
		}
		return totalMemMB;
	}

	private void printMemoryAllocation(Rule[] rules) {
		int totalPercent = 0;
		int totalDefaultMb = 0;
		for (Rule r : rules) {
			totalPercent += r.getSparePercent();
			totalDefaultMb += r.getDefaultHeap();
		}
		logger.info("{}MB is allocated for all heaps.", totalDefaultMb);
		validateTotalMemoryPercentage(totalPercent);
		logger.info("{}% of spare memory will be allocated to java components", totalPercent);
	}

	private void validateTotalMemoryPercentage(int totalPercent) {
		if (totalPercent > 100) {
			logger.error("You cannot distribute more than 100% of spare memory, total is {}%", totalPercent);
			System.exit(1);
		}
	}

	private File confPath(Rule r) {
		return new File("/etc/bm/default/" + r.getProduct() + ".ini");
	}

	@Override
	public void stop() {
		// ok
	}

	private boolean productEnabled(String productName) {
		File productDir = new File("/usr/share/" + productName);
		return productDir.exists() && !new File("/etc/bm/" + productName + ".disabled").exists();
	}
}
