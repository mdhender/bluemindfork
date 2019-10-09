package net.bluemind.backend.mail.replica.service.internal;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.backend.cyrus.partitions.CyrusBoxes;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.ICyrusValidation;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;

public class CyrusValidationService implements ICyrusValidation {
	private static final Logger logger = LoggerFactory.getLogger(CyrusValidationService.class);

	private static final String DEFAULT_PARTITION = "default";

	private final BmContext ctx;

	public CyrusValidationService(BmContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public boolean prevalidate(String mailbox, String partition) {
		// bm-master__devenv_blue/devenv.blue!user.leslie => accept
		// (null)/devenv.blue!user.leslie.Sent => accept
		// (null)/devenv.blue!user.titi => reject

		logger.info("Prevalidate p: {} mbox: {}", partition, mailbox);
		if (Strings.isNullOrEmpty(mailbox)) {
			return false;
		}

		ReplicatedBox box = CyrusBoxes.forCyrusMailbox(mailbox);
		if (box == null) {
			return false;
		}

		if (!box.mailboxRoot) {
			// null partition is fine for non-root folders
			return true;
		}

		// mailbox root
		String cleanPart = Optional.ofNullable(partition).orElse(DEFAULT_PARTITION);
		if (DEFAULT_PARTITION.equals(cleanPart)) {
			return false;
		} else {
			return validatePartition(partition, box.partition.replace('_', '.')) && validateName(box);
		}
	}

	private boolean validateName(ReplicatedBox box) {
		IMailboxes mboxApi = ctx.provider().instance(IMailboxes.class, box.partition.replace('_', '.'));
		Optional<ItemValue<Mailbox>> foundBox = Optional.ofNullable(mboxApi.byName(box.local.replace('^', '.')));
		return foundBox.isPresent();
	}

	private boolean validatePartition(String partition, String boxDomain) {
		CyrusPartition parsed = CyrusPartition.forName(partition);
		Set<String> backendUids = Topology.get().nodes().stream().filter(ivs -> ivs.value.tags.contains("mail/imap"))
				.map(ivs -> ivs.uid).collect(Collectors.toSet());
		return backendUids.contains(parsed.serverUid) && parsed.domainUid.equals(boxDomain);
	}

}
