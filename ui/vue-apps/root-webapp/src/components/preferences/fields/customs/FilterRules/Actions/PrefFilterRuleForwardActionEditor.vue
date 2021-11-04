<template>
    <div class="pref-filter-rule-forward-action-editor">
        <bm-contact-input
            class="border"
            :contacts="contacts"
            :max-contacts="1"
            :autocomplete-results="autocompleteResults"
            :validate-address-fn="validateAddress"
            @search="onSearch"
            @update:contacts="updateEmails"
        />
        <bm-form-checkbox v-model="action.value.localCopy" :value="true" :unchecked-value="false">
            {{ $t("preferences.mail.filters.action.forward.keep_copy") }}
        </bm-form-checkbox>
    </div>
</template>

<script>
import { BmContactInput, BmFormCheckbox } from "@bluemind/styleguide";
import { VCardQueryOrderBy } from "@bluemind/addressbook.api";
import { getQuery, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";

export default {
    name: "PrefFilterRuleForwardActionEditor",
    components: { BmContactInput, BmFormCheckbox },
    props: {
        action: {
            type: Object,
            required: true
        }
    },
    data() {
        return { autocompleteResults: [], maxContacts: 1 };
    },
    computed: {
        contacts: {
            get() {
                return this.action.value?.emails?.map(email => ({ address: email, dn: "" }));
            },
            set(value) {
                this.action.value.emails = value.map(v => v.address);
            }
        }
    },
    created() {
        if (!this.action.value) {
            this.action.value = { emails: [], localCopy: false };
        }
    },
    methods: {
        async onSearch(pattern) {
            if (!pattern) {
                this.autocompleteResults = [];
                return;
            }
            const searchResults = await inject("AddressBooksPersistence").search({
                from: 0,
                size: 5,
                query: getQuery(pattern),
                orderBy: VCardQueryOrderBy.Pertinance,
                escapeQuery: false
            });
            this.autocompleteResults = searchResults.values.map(vcardInfo => VCardInfoAdaptor.toContact(vcardInfo));
        },
        updateEmails(contacts) {
            this.action.value.emails = contacts
                .map(contact => contact.address)
                .filter(email => EmailValidator.validateAddress(email));
            this.autocompleteResults = [];
        },
        validateAddress: EmailValidator.validateAddress
    }
};
</script>
