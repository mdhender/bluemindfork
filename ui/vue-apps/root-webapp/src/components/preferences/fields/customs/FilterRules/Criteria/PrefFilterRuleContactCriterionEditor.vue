<template>
    <bm-contact-input
        class="pref-filter-rule-contact-criterion-editor border"
        tabindex="0"
        :contacts="contacts"
        :max-contacts="1"
        :autocomplete-results="autocompleteResults"
        :validate-address-fn="validateAddress"
        @search="onSearch"
        @update:contacts="updateEmails"
    />
</template>

<script>
import { BmContactInput } from "@bluemind/styleguide";
import { searchVCardsHelper, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";

export default {
    name: "PrefFilterRuleContactCriterionEditor",
    components: { BmContactInput },
    props: {
        criterion: {
            type: Object,
            required: true
        }
    },
    data() {
        return { autocompleteResults: [], dn: "" };
    },
    computed: {
        contacts: {
            get() {
                return this.criterion.value ? [{ address: this.criterion.value, dn: this.dn }] : [];
            },
            set(value) {
                value.length > 0 ? (this.criterion.value = value[0].address) : (this.criterion.value = "");
            }
        }
    },
    watch: {
        criterion: {
            async handler(criterion) {
                if (criterion?.value) {
                    // FIXME BmContactInput.watch:contacts should use isEqual instead of length comparison
                    this.dn = (await searchContacts(criterion.value))[0]?.dn || "";
                }
            },
            immediate: true
        }
    },
    methods: {
        async onSearch(pattern) {
            this.autocompleteResults = pattern ? await searchContacts(pattern) : [];
        },
        updateEmails(contacts) {
            this.criterion.value = contacts
                .map(contact => contact.address)
                .filter(email => EmailValidator.validateAddress(email))[0];
            this.autocompleteResults = [];
        },
        validateAddress: EmailValidator.validateAddress
    }
};

async function searchContacts(pattern) {
    const searchResults = await inject("AddressBooksPersistence").search(searchVCardsHelper(pattern, 5, true));
    return searchResults.values.map(vcardInfo => VCardInfoAdaptor.toContact(vcardInfo));
}
</script>
