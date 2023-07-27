<template>
    <contact-input
        class="pref-filter-rule-contact-criterion-editor"
        tabindex="0"
        :contacts.sync="contacts"
        :max-contacts="1"
        :autocomplete-results="autocompleteResults"
        :validate-address-fn="validateAddress"
        @search="onSearch"
    />
</template>

<script>
import { ContactInput } from "@bluemind/business-components";
import { searchVCardsHelper, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";

export default {
    name: "PrefFilterRuleContactCriterionEditor",
    components: { ContactInput },
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
            set(contacts) {
                const value = contacts.length > 0 ? contacts[0].address : "";
                this.$emit("update:criterion", { ...this.criterion, value });
                this.autocompleteResults = [];
            }
        }
    },
    watch: {
        criterion: {
            async handler(criterion) {
                if (criterion?.value) {
                    // FIXME ContactInput.watch:contacts should use isEqual instead of length comparison
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
        validateAddress: EmailValidator.validateAddress
    }
};

async function searchContacts(pattern) {
    const searchResults = await inject("AddressBooksPersistence").search(searchVCardsHelper(pattern, 5, true));
    return searchResults.values.map(vcardInfo => VCardInfoAdaptor.toContact(vcardInfo));
}
</script>
