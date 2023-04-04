package net.bluemind.directory.hollow.datamodel.producer;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.hollow.datamodel.producer.Value.ByteArrayValue;
import net.bluemind.directory.hollow.datamodel.producer.Value.ListValue;
import net.bluemind.directory.hollow.datamodel.producer.Value.StringValue;
import net.bluemind.directory.hollow.datamodel.utils.Pem;
import net.bluemind.utils.CertificateUtils;

public abstract class ContactInfosSerializer extends DirEntrySerializer {

	protected ContactInfosSerializer(ItemValue<DirEntry> dirEntry, String domainUid) {
		super(dirEntry, domainUid);
	}

	protected abstract VCard contactInfos();

	@Override
	public Value get(Property property) {
		switch (property) {
		case Surname:
			return new StringValue(contactInfos().identification.name.familyNames);
		case GivenName:
			return new StringValue(contactInfos().identification.name.givenNames);
		case Title:
			return new StringValue(contactInfos().organizational.title);
		case DepartmentName:
			return new StringValue(contactInfos().organizational.org.department);
		case CompanyName:
			return new StringValue(contactInfos().organizational.org.company);
		case Assistant:
			return new StringValue(contactInfos().related.assistant);
		case StreetAddress:
			return contactInfos().deliveryAddressing.stream().findFirst()
					.map(deliveryAddressing -> (Value) (new StringValue(deliveryAddressing.address.streetAddress)))
					.orElse(Value.NULL);
		case postOfficeBox:
			return contactInfos().deliveryAddressing.stream().findFirst()
					.map(deliveryAddressing -> (Value) (new StringValue(deliveryAddressing.address.postOfficeBox)))
					.orElse(Value.NULL);
		case Locality:
			return contactInfos().deliveryAddressing.stream().findFirst()
					.map(deliveryAddressing -> (Value) (new StringValue(deliveryAddressing.address.locality)))
					.orElse(Value.NULL);
		case StateOrProvince:
			return contactInfos().deliveryAddressing.stream().findFirst()
					.map(deliveryAddressing -> (Value) (new StringValue(deliveryAddressing.address.region)))
					.orElse(Value.NULL);
		case PostalCode:
			return contactInfos().deliveryAddressing.stream().findFirst()
					.map(deliveryAddressing -> (Value) (new StringValue(deliveryAddressing.address.postalCode)))
					.orElse(Value.NULL);
		case Country:
			return contactInfos().deliveryAddressing.stream().findFirst()
					.map(deliveryAddressing -> (Value) (new StringValue(deliveryAddressing.address.countryName)))
					.orElse(Value.NULL);
		case BusinessTelephoneNumber:
			return new StringValue(getPhoneNumber("voice", "work"));
		case HomeTelephoneNumber:
			return new StringValue(getPhoneNumber("voice", "home"));
		case MobileTelephoneNumber:
			return new StringValue(getPhoneNumber("voice", "cell"));
		case PagerTelephoneNumber:
			return Value.NULL;
		case PrimaryFaxNumber:
			return new StringValue(getPhoneNumber("fax", "work"));
		case AssistantTelephoneNumber:
			return Value.NULL;
		case UserX509Certificate:
			return new ListValue(contactInfos().security.keys.stream()
					.filter(key -> key.parameters.stream()
							.anyMatch(parameter -> "MEDIATYPE".equalsIgnoreCase(parameter.label)
									&& "application/pkcs7-mime".equalsIgnoreCase(parameter.value))) //
					.map(this::certToValue) //
					.filter(v -> v != Value.NULL) //
					.toList());
		case AddressBookX509Certificate:
			return new ListValue(contactInfos().security.keys.stream().map(key -> new Pem(key.value).toDer() //
					.map(der -> (Value) (new ByteArrayValue(der))).orElse(Value.NULL)).filter(v -> v != Value.NULL) //
					.toList());
		default:
			return super.get(property);
		}
	}

	private Value certToValue(VCard.Security.Key key) {
		return CertificateUtils.pkcs7PemToDer(key.value).map(pkcs7 -> (Value) (new ByteArrayValue(pkcs7)))
				.orElseGet(() -> new Pem(key.value).toPcks7().map(pcks7 -> (Value) (new ByteArrayValue(pcks7)))
						.orElse(Value.NULL));
	}

	private String getPhoneNumber(String type, String classifier) {
		String bestChoice = null;
		for (Tel tel : contactInfos().communications.tels) {
			if (tel.containsValues("TYPE", classifier, type)) {
				return tel.value;
			} else if (tel.containsValues("TYPE", type)) {
				bestChoice = tel.value;
			}
		}
		return null != bestChoice ? bestChoice : null;
	}

}
