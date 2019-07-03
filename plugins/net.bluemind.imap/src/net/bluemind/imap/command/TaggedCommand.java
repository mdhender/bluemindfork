package net.bluemind.imap.command;

import java.util.List;

import net.bluemind.imap.TaggedResult;
import net.bluemind.imap.impl.IMAPResponse;

/**
 * Use this to execute command we don't natively support in unit tests
 * 
 */
public class TaggedCommand extends SimpleCommand<TaggedResult> {

	public TaggedCommand(String cmd) {
		super(cmd);
		data = new TaggedResult();
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data.setOk(isOk(rs));
		String[] output = new String[rs.size()];
		for (int i = 0; i < output.length; i++) {
			output[i] = rs.get(i).getPayload();
		}
		data.setOutput(output);
	}

}
