package net.bluemind.signature.commons.action;

public class VariableDecorators {

	public static IVariableDecorator newLineToBr() {
		return new IVariableDecorator() {
			@Override
			public String decorate(String key, String value) {
				return value.replaceAll("(\r\n|\n)", "<br/>");
			}
		};
	}

}
