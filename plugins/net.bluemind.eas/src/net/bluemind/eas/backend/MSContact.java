/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.backend;

import java.util.Date;
import java.util.List;

import net.bluemind.eas.dto.type.ItemDataType;

public class MSContact implements IApplicationData {

	public String assistantName;
	public String assistantPhoneNumber;
	public String assistnamePhoneNumber;
	public String business2PhoneNumber;
	public String businessAddressCity;
	public String businessPhoneNumber;
	public String webPage;
	public String businessAddressCountry;
	public String department;
	public String email1Address;
	public String email2Address;
	public String email3Address;
	public String businessFaxNumber;
	public String fileAs;
	public String firstName;
	public String middleName;
	public String homeAddressCity;
	public String homeAddressCountry;
	public String homeFaxNumber;
	public String homePhoneNumber;
	public String home2PhoneNumber;
	public String homeAddressPostalCode;
	public String homeAddressState;
	public String homeAddressStreet;
	public String mobilePhoneNumber;
	public String suffix;
	public String companyName;
	public String otherAddressCity;
	public String otherAddressCountry;
	public String carPhoneNumber;
	public String otherAddressPostalCode;
	public String otherAddressState;
	public String otherAddressStreet;
	public String pagerNumber;
	public String title;
	public String businessPostalCode;
	public String lastName;
	public String spouse;
	public String businessState;
	public String businessStreet;
	public String jobTitle;
	public String yomiFirstName;
	public String yomiLastName;
	public String yomiCompanyName;
	public String officeLocation;
	public String radioPhoneNumber;
	public String picture;
	public String data;

	public Date anniversary;
	public Date birthday;

	public List<String> categories;
	public List<String> children;

	// Contacts2

	public String customerId;
	public String governmentId;
	public String iMAddress;
	public String iMAddress2;
	public String iMAddress3;
	public String managerName;
	public String companyMainPhone;
	public String accountName;
	public String nickName;
	public String mMS;

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	public String getAssistantName() {
		return assistantName;
	}

	public void setAssistantName(String assistantName) {
		this.assistantName = assistantName;
	}

	public String getAssistantPhoneNumber() {
		return assistantPhoneNumber;
	}

	public void setAssistantPhoneNumber(String assistantPhoneNumber) {
		this.assistantPhoneNumber = assistantPhoneNumber;
	}

	public String getAssistnamePhoneNumber() {
		return assistnamePhoneNumber;
	}

	public void setAssistnamePhoneNumber(String assistnamePhoneNumber) {
		this.assistnamePhoneNumber = assistnamePhoneNumber;
	}

	public String getBusiness2PhoneNumber() {
		return business2PhoneNumber;
	}

	public void setBusiness2PhoneNumber(String business2PhoneNumber) {
		this.business2PhoneNumber = business2PhoneNumber;
	}

	public String getBusinessAddressCity() {
		return businessAddressCity;
	}

	public void setBusinessAddressCity(String businessAddressCity) {
		this.businessAddressCity = businessAddressCity;
	}

	public String getBusinessPhoneNumber() {
		return businessPhoneNumber;
	}

	public void setBusinessPhoneNumber(String businessPhoneNumber) {
		this.businessPhoneNumber = businessPhoneNumber;
	}

	public String getWebPage() {
		return webPage;
	}

	public void setWebPage(String webPage) {
		this.webPage = webPage;
	}

	public String getBusinessAddressCountry() {
		return businessAddressCountry;
	}

	public void setBusinessAddressCountry(String businessAddressCountry) {
		this.businessAddressCountry = businessAddressCountry;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getEmail1Address() {
		return email1Address;
	}

	public void setEmail1Address(String email1Address) {
		this.email1Address = email1Address;
	}

	public String getEmail2Address() {
		return email2Address;
	}

	public void setEmail2Address(String email2Address) {
		this.email2Address = email2Address;
	}

	public String getEmail3Address() {
		return email3Address;
	}

	public void setEmail3Address(String email3Address) {
		this.email3Address = email3Address;
	}

	public String getBusinessFaxNumber() {
		return businessFaxNumber;
	}

	public void setBusinessFaxNumber(String businessFaxNumber) {
		this.businessFaxNumber = businessFaxNumber;
	}

	public String getFileAs() {
		return fileAs;
	}

	public void setFileAs(String fileAs) {
		this.fileAs = fileAs;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getHomeAddressCity() {
		return homeAddressCity;
	}

	public void setHomeAddressCity(String homeAddressCity) {
		this.homeAddressCity = homeAddressCity;
	}

	public String getHomeAddressCountry() {
		return homeAddressCountry;
	}

	public void setHomeAddressCountry(String homeAddressCountry) {
		this.homeAddressCountry = homeAddressCountry;
	}

	public String getHomeFaxNumber() {
		return homeFaxNumber;
	}

	public void setHomeFaxNumber(String homeFaxNumber) {
		this.homeFaxNumber = homeFaxNumber;
	}

	public String getHomePhoneNumber() {
		return homePhoneNumber;
	}

	public void setHomePhoneNumber(String homePhoneNumber) {
		this.homePhoneNumber = homePhoneNumber;
	}

	public String getHome2PhoneNumber() {
		return home2PhoneNumber;
	}

	public void setHome2PhoneNumber(String home2PhoneNumber) {
		this.home2PhoneNumber = home2PhoneNumber;
	}

	public String getHomeAddressPostalCode() {
		return homeAddressPostalCode;
	}

	public void setHomeAddressPostalCode(String homeAddressPostalCode) {
		this.homeAddressPostalCode = homeAddressPostalCode;
	}

	public String getHomeAddressState() {
		return homeAddressState;
	}

	public void setHomeAddressState(String homeAddressState) {
		this.homeAddressState = homeAddressState;
	}

	public String getHomeAddressStreet() {
		return homeAddressStreet;
	}

	public void setHomeAddressStreet(String homeAddressStreet) {
		this.homeAddressStreet = homeAddressStreet;
	}

	public String getMobilePhoneNumber() {
		return mobilePhoneNumber;
	}

	public void setMobilePhoneNumber(String mobilePhoneNumber) {
		this.mobilePhoneNumber = mobilePhoneNumber;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getOtherAddressCity() {
		return otherAddressCity;
	}

	public void setOtherAddressCity(String otherAddressCity) {
		this.otherAddressCity = otherAddressCity;
	}

	public String getOtherAddressCountry() {
		return otherAddressCountry;
	}

	public void setOtherAddressCountry(String otherAddressCountry) {
		this.otherAddressCountry = otherAddressCountry;
	}

	public String getCarPhoneNumber() {
		return carPhoneNumber;
	}

	public void setCarPhoneNumber(String carPhoneNumber) {
		this.carPhoneNumber = carPhoneNumber;
	}

	public String getOtherAddressPostalCode() {
		return otherAddressPostalCode;
	}

	public void setOtherAddressPostalCode(String otherAddressPostalCode) {
		this.otherAddressPostalCode = otherAddressPostalCode;
	}

	public String getOtherAddressState() {
		return otherAddressState;
	}

	public void setOtherAddressState(String otherAddressState) {
		this.otherAddressState = otherAddressState;
	}

	public String getOtherAddressStreet() {
		return otherAddressStreet;
	}

	public void setOtherAddressStreet(String otherAddressStreet) {
		this.otherAddressStreet = otherAddressStreet;
	}

	public String getPagerNumber() {
		return pagerNumber;
	}

	public void setPagerNumber(String pagerNumber) {
		this.pagerNumber = pagerNumber;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBusinessPostalCode() {
		return businessPostalCode;
	}

	public void setBusinessPostalCode(String businessPostalCode) {
		this.businessPostalCode = businessPostalCode;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getSpouse() {
		return spouse;
	}

	public void setSpouse(String spouse) {
		this.spouse = spouse;
	}

	public String getBusinessState() {
		return businessState;
	}

	public void setBusinessState(String businessState) {
		this.businessState = businessState;
	}

	public String getBusinessStreet() {
		return businessStreet;
	}

	public void setBusinessStreet(String businessStreet) {
		this.businessStreet = businessStreet;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public String getYomiFirstName() {
		return yomiFirstName;
	}

	public void setYomiFirstName(String yomiFirstName) {
		this.yomiFirstName = yomiFirstName;
	}

	public String getYomiLastName() {
		return yomiLastName;
	}

	public void setYomiLastName(String yomiLastName) {
		this.yomiLastName = yomiLastName;
	}

	public String getYomiCompanyName() {
		return yomiCompanyName;
	}

	public void setYomiCompanyName(String yomiCompanyName) {
		this.yomiCompanyName = yomiCompanyName;
	}

	public String getOfficeLocation() {
		return officeLocation;
	}

	public void setOfficeLocation(String officeLocation) {
		this.officeLocation = officeLocation;
	}

	public String getRadioPhoneNumber() {
		return radioPhoneNumber;
	}

	public void setRadioPhoneNumber(String radioPhoneNumber) {
		this.radioPhoneNumber = radioPhoneNumber;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getGovernmentId() {
		return governmentId;
	}

	public void setGovernmentId(String governmentId) {
		this.governmentId = governmentId;
	}

	public String getIMAddress() {
		return iMAddress;
	}

	public void setIMAddress(String address) {
		iMAddress = address;
	}

	public String getIMAddress2() {
		return iMAddress2;
	}

	public void setIMAddress2(String address2) {
		iMAddress2 = address2;
	}

	public String getIMAddress3() {
		return iMAddress3;
	}

	public void setIMAddress3(String address3) {
		iMAddress3 = address3;
	}

	public String getManagerName() {
		return managerName;
	}

	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}

	public String getCompanyMainPhone() {
		return companyMainPhone;
	}

	public void setCompanyMainPhone(String companyMainPhone) {
		this.companyMainPhone = companyMainPhone;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getMMS() {
		return mMS;
	}

	public void setMMS(String mms) {
		mMS = mms;
	}

	@Override
	public ItemDataType getType() {
		return ItemDataType.CONTACTS;
	}

	public Date getAnniversary() {
		return anniversary;
	}

	public void setAnniversary(Date anniversary) {
		this.anniversary = anniversary;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
