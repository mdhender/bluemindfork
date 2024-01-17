package net.bluemind.eas.validation;

import org.w3c.dom.Document;

public interface IProtocolValidator {

	void checkRequest(double protocolVersion, Document doc) throws ValidationException;

	void checkResponse(double protocolVersion, Document doc) throws ValidationException;

}
