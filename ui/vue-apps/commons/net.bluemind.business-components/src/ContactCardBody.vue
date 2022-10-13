<template>
    <div class="contact-card-body ml-3">
        <div v-if="emails.length" class="d-flex">
            <bm-icon icon="user-enveloppe" variant="secondary" />
            <div class="d-flex flex-column ml-4">
                <span v-for="email in emails" :key="email.address" class="mb-3">
                    <strong>
                        <a :href="`mailto://${email.address}`">{{ email.address }}</a>
                    </strong>
                </span>
            </div>
        </div>
        <div v-if="phones.length" class="d-flex mt-4">
            <bm-icon icon="phone" variant="secondary" />
            <div class="d-flex flex-column ml-4">
                <span v-for="phone in phones" :key="phone.number" class="mb-3">
                    <strong>{{ phone.number }}</strong>
                    <span class="ml-2 text-neutral">{{ phone.type }}</span>
                </span>
            </div>
        </div>
        <div v-if="locations.length" class="d-flex mt-4">
            <bm-icon icon="world" variant="secondary" />
            <div class="d-flex flex-column ml-4">
                <div v-for="(location, index) in locations" :key="index" class="mb-3">
                    <div v-for="(line, i) in location.lines" :key="i">
                        <strong>{{ line }}</strong>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { formatAddress } from "localized-address-format";
import { BmIcon } from "@bluemind/styleguide";
import l10n from "./l10n";

export default {
    name: "ContactCardBody",
    components: { BmIcon },
    componentI18N: { messages: l10n },
    props: {
        contact: {
            type: Object,
            required: true
        }
    },
    computed: {
        emails() {
            return (
                this.contact?.value?.communications?.emails?.map(email => ({
                    address: email.value,
                    type: this.typeName(email.parameters)
                })) || []
            );
        },
        phones() {
            return (
                this.contact?.value?.communications?.tels?.map(phone => ({
                    number: phone.value,
                    type: this.typeName(phone.parameters)
                })) || []
            );
        },
        locations() {
            return (
                this.contact?.value?.deliveryAddressing?.map(({ address }) => ({
                    lines: [
                        ...formatAddress({
                            postalCountry: "FR", // missing address.countryCode
                            administrativeArea: address.region,
                            locality: address.locality,
                            // dependentLocality: '',
                            postalCode: address.postalCode,
                            sortingCode: address.postOfficeBox,
                            // organization: "Example Org.",
                            // name: this.displayName,
                            addressLines: [address.streetAddress, address.extentedAddress].filter(Boolean)
                        }),
                        address.countryName
                    ].filter(Boolean),
                    type: this.typeName(address.parameters)
                })) || []
            );
        }
    },
    methods: {
        typeName(parameters) {
            const typeValue = parameters?.find(p => p.label === "TYPE")?.value;
            return typeValue && this.$te(`contact.info.type.${typeValue}`)
                ? this.$t(`contact.info.type.${typeValue}`)
                : undefined;
        }
    }
};
</script>
