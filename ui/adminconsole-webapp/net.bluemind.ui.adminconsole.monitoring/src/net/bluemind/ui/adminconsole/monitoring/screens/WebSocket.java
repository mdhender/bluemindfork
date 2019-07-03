package net.bluemind.ui.adminconsole.monitoring.screens;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative=true,namespace=JsPackage.GLOBAL, name="WebSocket")
public abstract class WebSocket {

	@JsFunction
	public interface Callback {
		public void notified();
	}
	
	@JsProperty
	public Callback onopen;
	
	@JsProperty
	public Callback onerror; 

	@JsProperty
	public short readyState;

	@JsProperty
	public String url;

	@JsMethod
	public abstract void  close();
	
}
