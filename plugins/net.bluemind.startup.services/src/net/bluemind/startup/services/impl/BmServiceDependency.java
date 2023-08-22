package net.bluemind.startup.services.impl;

import java.util.Dictionary;

import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BmServiceDependency implements ServiceDependency {

	private final BundleContext bundleContext;
	private final ServiceDependency wrapped;

	public BmServiceDependency(BundleContext bundleContext, ServiceDependency wrapped) {
		this.bundleContext = bundleContext;
		this.wrapped = wrapped;
	}

	@Override
	public boolean isRequired() {
		return wrapped.isRequired();
	}

	@Override
	public boolean isAvailable() {
		return wrapped.isAvailable();
	}

	@Override
	public boolean isAutoConfig() {
		return wrapped.isAutoConfig();
	}

	@Override
	public String getAutoConfigName() {
		return wrapped.getAutoConfigName();
	}

	@Override
	public boolean isPropagated() {
		return wrapped.isPropagated();
	}

	@Override
	public <K, V> Dictionary<K, V> getProperties() {
		return wrapped.getProperties();
	}

	@Override
	public String getName() {
		return wrapped.getName();
	}

	@Override
	public String getSimpleName() {
		return wrapped.getSimpleName();
	}

	@Override
	public String getFilter() {
		return wrapped.getFilter();
	}

	@Override
	public String getType() {
		return wrapped.getType();
	}

	@Override
	public int getState() {
		return wrapped.getState();
	}

	@Override
	public ServiceDependency setCallbacks(String add, String remove) {
		return wrapped.setCallbacks(add, remove);
	}

	@Override
	public ServiceDependency setCallbacks(String add, String change, String remove) {
		return wrapped.setCallbacks(add, change, remove);
	}

	@Override
	public ServiceDependency setCallbacks(String add, String change, String remove, String swap) {
		return wrapped.setCallbacks(add, change, remove, swap);
	}

	@Override
	public ServiceDependency setCallbacks(Object instance, String add, String remove) {
		return wrapped.setCallbacks(instance, add, remove);
	}

	@Override
	public ServiceDependency setCallbacks(Object instance, String add, String change, String remove) {
		return wrapped.setCallbacks(instance, add, change, remove);
	}

	@Override
	public ServiceDependency setCallbacks(Object instance, String added, String changed, String removed,
			String swapped) {
		return wrapped.setCallbacks(instance, added, changed, removed, swapped);
	}

	@Override
	public ServiceDependency setRequired(boolean required) {
		return wrapped.setRequired(required);
	}

	@Override
	public ServiceDependency setAutoConfig(boolean autoConfig) {
		return wrapped.setAutoConfig(autoConfig);
	}

	@Override
	public ServiceDependency setAutoConfig(String instanceName) {
		return wrapped.setAutoConfig(instanceName);
	}

	@Override
	public ServiceDependency setService(Class<?> serviceName) {
		ServiceDependenciesLookup.startBundleImplementing(bundleContext, serviceName);
		return wrapped.setService(serviceName);
	}

	@Override
	public ServiceDependency setService(Class<?> serviceName, String serviceFilter) {
		ServiceDependenciesLookup.startBundleImplementing(bundleContext, serviceName);
		return wrapped.setService(serviceName, serviceFilter);
	}

	@Override
	public ServiceDependency setService(String serviceFilter) {
		return wrapped.setService(serviceFilter);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public ServiceDependency setService(Class<?> serviceName, ServiceReference serviceReference) {
		ServiceDependenciesLookup.startBundleImplementing(bundleContext, serviceName);
		return wrapped.setService(serviceName, serviceReference);
	}

	@Override
	public ServiceDependency setDefaultImplementation(Object implementation) {
		return wrapped.setDefaultImplementation(implementation);
	}

	@Override
	public ServiceDependency setPropagate(boolean propagate) {
		return wrapped.setPropagate(propagate);
	}

	@Override
	public ServiceDependency setPropagate(Object instance, String method) {
		return wrapped.setPropagate(instance, method);
	}

	@Override
	public ServiceDependency setDebug(String debugKey) {
		return wrapped.setDebug(debugKey);
	}

	@Override
	public ServiceDependency setDereference(boolean dereferenceServiceInternally) {
		return wrapped.setDereference(dereferenceServiceInternally);
	}

}
