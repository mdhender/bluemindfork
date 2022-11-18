package net.bluemind.systemcheck.collect;

import java.io.FileInputStream;
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;

import net.bluemind.core.rest.IServiceProvider;

public class MemoryCollector implements IDataCollector {
	public void collect(IServiceProvider provider, Map<String, String> collected) throws Exception {
		readMemory(collected);
	}

	private void readMemory(Map<String, String> ret) {
		try {
			FileInputStream fis = new FileInputStream("/proc/meminfo");
			byte[] data = ByteStreams.toByteArray(fis);
			fis.close();
			Iterable<String> lines = Splitter.on('\n').split(new String(data));
			for (String l : lines) {
				if (l.startsWith("MemTotal:")) {
					String number = l.replace("MemTotal:", "").replace("kB", "").trim();
					int megabytes = Integer.parseInt(number) / 1024;
					ret.put("mem.mb", "" + megabytes);
				}
			}
		} catch (Exception e) {
			ret.put("mem.mb", "0");
		}
	}
}
