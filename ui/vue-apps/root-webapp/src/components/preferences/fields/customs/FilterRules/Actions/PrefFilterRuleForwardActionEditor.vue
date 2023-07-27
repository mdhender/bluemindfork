<template>
    <div class="pref-filter-rule-forward-action-editor">
        <contact-input
            :contacts.sync="contacts"
            :max-contacts="maxContacts"
            :autocomplete-results="autocompleteResults"
            :validate-address-fn="validateAddress"
            @search="onSearch"
        />
        <bm-form-checkbox v-model="keepCopy" :value="true" :unchecked-value="false" class="mt-3 mb-2">
            {{ $t("preferences.mail.filters.action.REDIRECT.keep_copy") }}
        </bm-form-checkbox>
    </div>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/ui-components";
import { ContactInput } from "@bluemind/business-components";
import { searchVCardsHelper, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";

export default {
    name: "PrefFilterRuleForwardActionEditor",
    components: { BmFormCheckbox, ContactInput },
    props: {
        action: {
            type: Object,
            required: true
        }
    },
    data() {
        return { autocompleteResults: [], maxContacts: 10 };
    },
    computed: {
        keepCopy: {
            get() {
                return this.action.keepCopy ?? false;
            },
            set(keepCopy) {
                this.$emit("update:action", { ...this.action, keepCopy });
            }
        },
        contacts: {
            get() {
                return (this.action.emails ?? []).map(email => ({ address: email, dn: "" }));
            },
            set(contacts) {
                const emails = contacts
                    .map(contact => contact.address)
                    .filter(email => EmailValidator.validateAddress(email));
                this.$emit("update:action", { ...this.action, emails });
                this.autocompleteResults = [];
            }
        }
    },
    methods: {
        async onSearch(pattern) {
            if (!pattern) {
                this.autocompleteResults = [];
                return;
            }
            const searchResults = await inject("AddressBooksPersistence").search(searchVCardsHelper(pattern, 5, true));
            this.autocompleteResults = searchResults.values
                .map(VCardInfoAdaptor.toContact)
                .filter(contact => contact.address !== inject("UserSession").defaultEmail);
        },
        validateAddress: EmailValidator.validateAddress
    }
};
</script>
