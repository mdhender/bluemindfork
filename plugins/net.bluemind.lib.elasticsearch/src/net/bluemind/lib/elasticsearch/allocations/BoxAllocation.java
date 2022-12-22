package net.bluemind.lib.elasticsearch.allocations;

public class BoxAllocation {
	public final String sourceIndex;
	public final String targetIndex;
	public final String mbox;

	public BoxAllocation(String sourceIndex, String targetIndex, String mbox) {
		this.sourceIndex = sourceIndex;
		this.targetIndex = targetIndex;
		this.mbox = mbox;
	}
}