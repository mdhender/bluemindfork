package net.bluemind.eas.http.query.internal;

import net.bluemind.eas.dto.OptionalParams;

public final class Base64OptParams implements OptionalParams {

	private String attachmentName;

	private String collectionId;

	private String collectionName;

	private String itemId;

	private String longId;

	private String parentId;

	private String occurrence;

	private String saveInSent;

	private String acceptMultiPart;

	private String acceptEncoding;

	@Override
	public String attachmentName() {
		return attachmentName;
	}

	@Override
	public String collectionId() {
		return collectionId;
	}

	@Override
	public String collectionName() {
		return collectionName;
	}

	@Override
	public String itemId() {
		return itemId;
	}

	@Override
	public String longId() {
		return longId;
	}

	@Override
	public String parentId() {
		return parentId;
	}

	@Override
	public String occurrence() {
		return occurrence;
	}

	@Override
	public String saveInSent() {
		return saveInSent;
	}

	@Override
	public String acceptMultiPart() {
		return acceptMultiPart;
	}

	@Override
	public String acceptEncoding() {
		return acceptEncoding;
	}

	public void setAttachmentName(String attachmentName) {
		this.attachmentName = attachmentName;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public void setLongId(String longId) {
		this.longId = longId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public void setOccurrence(String occurrence) {
		this.occurrence = occurrence;
	}

	public void setSaveInSent(String saveInSent) {
		this.saveInSent = saveInSent;
	}

	public void setAcceptMultiPart(String acceptMultiPart) {
		this.acceptMultiPart = acceptMultiPart;
	}

	public void setAcceptEncoding(String acceptEncoding) {
		this.acceptEncoding = acceptEncoding;
	}

}
