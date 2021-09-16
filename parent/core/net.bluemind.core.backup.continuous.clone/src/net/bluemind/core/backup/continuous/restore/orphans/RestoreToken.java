package net.bluemind.core.backup.continuous.restore.orphans;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.CoreTok;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreToken {

	private static final Logger logger = LoggerFactory.getLogger(RestoreToken.class);

	public RestoreToken() {
	}

	public String restore(IServerTaskMonitor monitor, List<DataElement> maybeTok) {
		ValueReader<ItemValue<CoreTok>> scReader = JsonUtils.reader(new TypeReference<ItemValue<CoreTok>>() {
		});

		Optional<CoreTok> lastTok = maybeTok.stream()
				.filter(de -> de.key.valueClass.equals(CoreTok.class.getCanonicalName()))
				.map(de -> scReader.read(new String(de.payload)).value).collect(Collectors.maxBy((tok1, tok2) -> 1));
		monitor.log("Got core.tok " + lastTok);
		return lastTok.map(c -> c.key).orElse(null);
	}

}
