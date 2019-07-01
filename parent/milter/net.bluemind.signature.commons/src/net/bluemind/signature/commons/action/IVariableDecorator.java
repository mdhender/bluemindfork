package net.bluemind.signature.commons.action;

@FunctionalInterface
public interface IVariableDecorator {

	public String decorate(String key, String value);
	
	
}
