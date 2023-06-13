<template>
    <div class="contact-search-input flex-fill">
        <contact-input
            class="desktop"
            variant="underline"
            :validate-address-fn="validateAddress"
            :autocomplete-results="autocompleteResults"
            :contacts="contacts"
            :max-contacts="maxContacts"
            @update:contacts="updateSelection"
            @search="debounceSearch"
            @keydown.escape.native="escape"
        />
        <contact-input
            class="mobile"
            variant="outline"
            :validate-address-fn="validateAddress"
            :autocomplete-results="autocompleteResults"
            :contacts="contacts"
            :max-contacts="maxContacts"
            @update:contacts="updateSelection"
            @search="debounceSearch"
        />
    </div>
</template>

<script>
import debounce from "lodash/debounce";
import { inject } from "@bluemind/inject";
import { searchVCardsHelper, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { ContactInput } from "@bluemind/business-components";

export default {
    name: "ContactSearchInput",
    components: { ContactInput },
    props: {
        addresses: {
            type: Array,
            default: () => []
        },
        maxContacts: {
            type: Number,
            default: () => undefined
        }
    },
    data() {
        return {
            autocompleteResults: [],
            contacts: [],
            debounceSearch: debounce(this.search, 200)
        };
    },
    computed: {
        selectedAddresses() {
            return [...new Set(this.contacts.map(({ address }) => address))];
        }
    },
    watch: {
        addresses: {
            async handler() {
                try {
                    const promises = this.addresses.map(address =>
                        EmailValidator.validateAddress(address)
                            ? inject("AddressBooksPersistence").search(searchVCardsHelper(address, 1))
                            : null
                    );
                    const vcards = await Promise.all(promises);
                    this.contacts = vcards.map((vcard, index) =>
                        vcard?.total === 1
                            ? VCardInfoAdaptor.toContact(vcard.values.pop())
                            : { address: this.addresses[index] }
                    );
                } catch {
                    this.contacts = [];
                }
            },
            immediate: true
        }
    },
    methods: {
        async search(pattern) {
            if (!pattern) {
                this.autocompleteResults = [];
                return;
            }
            const searchResults = await inject("AddressBooksPersistence").search(searchVCardsHelper(pattern, 5, true));
            this.autocompleteResults = searchResults.values.flatMap(vcard => {
                const contact = VCardInfoAdaptor.toContact(vcard);
                return !this.selectedAddresses.includes(contact.address) ? contact : [];
            });
        },
        validateAddress: EmailValidator.validateAddress,
        updateSelection(contacts) {
            this.$emit(
                "update:addresses",
                contacts.map(({ address }) => address)
            );
        },
        escape(event) {
            if (this.autocompleteResults.length > 0) {
                event.stopPropagation();
                this.autocompleteResults = [];
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";

.contact-search-input {
    .contact-input.desktop {
        @include until-lg {
            display: none !important;
        }
    }
    .contact-input.mobile {
        @include from-lg {
            display: none !important;
        }
    }
}
</style>
