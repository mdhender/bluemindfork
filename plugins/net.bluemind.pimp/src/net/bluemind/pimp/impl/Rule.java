package net.bluemind.pimp.impl;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "product", "defaultHeap", "defaultDirect", "sparePercent" })
public class Rule {

	@JsonProperty("product")
	private String product;

	@JsonProperty("defaultHeap")
	private int defaultHeap;

	@JsonProperty("defaultDirect")
	private int defaultDirect;

	@JsonProperty("directCap")
	private int directCap = 512;

	@JsonProperty("sparePercent")
	private int sparePercent;

	@JsonProperty("cpusBoost")
	private int cpusBoost = 0;

	@JsonProperty("optional")
	private boolean optional = false;

	private Map<String, Object> additionalProperties = new HashMap<>();

	@JsonProperty("product")
	public String getProduct() {
		return product;
	}

	@JsonProperty("product")
	public void setProduct(String product) {
		this.product = product;
	}

	@JsonProperty("defaultHeap")
	public int getDefaultHeap() {
		return defaultHeap;
	}

	@JsonProperty("defaultHeap")
	public void setDefaultHeap(int defaultHeap) {
		this.defaultHeap = defaultHeap;
	}

	@JsonProperty("defaultDirect")
	public int getDefaultDirect() {
		return defaultDirect;
	}

	@JsonProperty("defaultDirect")
	public void setDefaultDirect(int defaultDirect) {
		this.defaultDirect = defaultDirect;
	}

	@JsonProperty("sparePercent")
	public int getSparePercent() {
		return sparePercent;
	}

	@JsonProperty("sparePercent")
	public void setSparePercent(int sparePercent) {
		this.sparePercent = sparePercent;
	}

	/**
	 * We will allocate
	 * <code>cpusBoost * availableProcessors * threadStackSize</code> extra memory
	 * the each process.
	 * 
	 * The default value is zero
	 * 
	 * @return the multiplier that will be user
	 */
	@JsonProperty("cpusBoost")
	public int getCpusBoost() {
		return cpusBoost;
	}

	/**
	 * We will allocate
	 * <code>cpusBoost * availableProcessors * threadStackSize extra</code> memory
	 * the each process.
	 * 
	 * The default value is zero
	 * 
	 * @param cpusBoost the multiplier to use
	 */
	@JsonProperty("cpusBoost")
	public void setCpusBoost(int cpusBoost) {
		this.cpusBoost = cpusBoost;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	/**
	 * Max direct memory (MB) to allocate, defaults to 512m
	 * 
	 * @return
	 */
	public int getDirectCap() {
		return directCap;
	}

	public void setDirectCap(int directCap) {
		this.directCap = directCap;
	}

}
