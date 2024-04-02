<template>
    <contact-input
        class="pref-filter-rule-contact-criterion-editor"
        variant="underline"
        tabindex="0"
        :contacts.sync="contacts"
        :max-contacts="1"
        :auto-collapsible="false"
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
        return { autocompleteResults: [] };
    },
    computed: {
        contacts: {
            get() {
                return this.criterion.value
                    ? this.criterion.value.address
                        ? [this.criterion.value]
                        : [{ address: this.criterion.value }]
                    : [];
            },
            set(contacts) {
                const value = contacts.length > 0 ? contacts[0] : "";
                this.$emit("update:criterion", {
                    ...this.criterion,
                    value,
                    sanitize: criterion => ({ ...criterion, value: criterion?.value?.address })
                });
                this.autocompleteResults = [];
            }
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
    const searchResults = await inject("AddressBooksPersistence").search(
        searchVCardsHelper(pattern, { size: 5, noGroup: true })
    );
    return searchResults.values.map(vcardInfo => VCardInfoAdaptor.toContact(vcardInfo));
}
</script>
