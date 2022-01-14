package net.bluemind.signature.commons.action;

public class VariableDecorators {
	public static IVariableDecorator newLineToBr() {
		return (key, value) -> {
			return value.replace("\r\n", "<br/>").replace("\n", "<br/>");
		};
	}
}
