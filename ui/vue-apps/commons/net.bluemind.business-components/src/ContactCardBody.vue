<template>
    <div class="contact-card-body ml-3">
        <div v-if="emails.length" class="d-flex">
            <bm-icon icon="user-enveloppe" variant="secondary" />
            <ol class="d-flex flex-column p-0 ml-4">
                <li v-for="email in emails" :key="email.address" class="d-flex">
                    <strong>
                        <slot name="email" :email="email.address">
                            {{ email.address }}
                        </slot>
                    </strong>
                    <bm-icon-button-copy :text="email.address" size="sm" class="ml-4" />
                </li>
            </ol>
        </div>
        <div v-if="phones.length" class="d-flex mt-4">
            <bm-icon icon="phone" variant="secondary" />
            <ol class="d-flex flex-column p-0 ml-4">
                <li v-for="phone in phones" :key="phone.number" class="d-flex" :text="phone.number">
                    <a :href="`tel:${phone.number.replace(/\s+/g, '')}`">
                        <strong>{{ phone.number }}</strong>
                    </a>
                    <span class="ml-2 text-neutral">{{ phone.type }}</span>
                    <bm-icon-button-copy :text="phone.number" size="sm" class="ml-4" />
                </li>
            </ol>
        </div>
        <div v-if="locations.length" class="d-flex mt-4">
            <bm-icon icon="world" variant="secondary" />
            <ol class="d-flex flex-column p-0 ml-4">
                <li v-for="(location, index) in locations" :key="index" class="d-flex">
                    <div>
                        <div v-for="(line, i) in location.lines" :key="i">
                            <strong>{{ line }}</strong>
                        </div>
                    </div>
                    <bm-icon-button-copy :text="location.lines.join(EOL)" size="sm" class="ml-4" />
                </li>
            </ol>
        </div>
    </div>
</template>

<script>
import { formatAddress } from "localized-address-format";
import { BmIcon, BmIconButtonCopy } from "@bluemind/ui-components";

import l10n from "./l10n";

export default {
    name: "ContactCardBody",
    components: { BmIcon, BmIconButtonCopy },
    componentI18N: { messages: l10n },
    props: {
        contact: {
            type: Object,
            required: true
        }
    },
    data() {
        return { EOL: navigator.userAgent.indexOf("Win") !== -1 ? "\r\n" : "\n" };
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

<style lang="scss">
.contact-card-body {
    ol {
        li {
            list-style-type: none;
            .bm-icon-button-copy:not(.active) {
                visibility: hidden;
            }
            &:hover .bm-icon-button-copy {
                visibility: visible;
            }
        }
    }
}
</style>
