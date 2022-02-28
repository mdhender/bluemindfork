<template>
    <div>
        <bm-form-checkbox v-model="value.enabled" class="mb-3">
            {{ $t("preferences.mail.emails_forwarding.to") }}
        </bm-form-checkbox>
        <div class="d-flex">
            <bm-contact-input
                class="mr-2 border border-dark"
                :disabled="!value.enabled"
                :contacts="contacts"
                :autocomplete-results="autocompleteResults"
                :validate-address-fn="validateAddress"
                @search="onSearch"
                @update:contacts="updateEmails"
            />
            <bm-form-select
                v-model="value.localCopy"
                :options="options"
                :disabled="!value.enabled"
                class="align-self-start"
            />
        </div>
    </div>
</template>

<script>
import { searchVCardsHelper, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { BmContactInput, BmFormCheckbox, BmFormSelect } from "@bluemind/styleguide";
import CentralizedSaving from "../../mixins/CentralizedSaving";

export default {
    name: "PrefEmailsForwarding",
    components: { BmContactInput, BmFormCheckbox, BmFormSelect },
    mixins: [CentralizedSaving],
    data() {
        return {
            autocompleteResults: [],
            options: [
                { text: this.$t("preferences.mail.emails_forwarding.no_local_copy"), value: false },
                { text: this.$t("preferences.mail.emails_forwarding.local_copy"), value: true }
            ]
        };
    },
    computed: {
        contacts() {
            return this.value.emails.map(email => ({ address: email, dn: "" }));
        },
        isValid() {
            return !this.value.enabled || this.value.emails.length > 0;
        }
    },
    created() {
        const save = async ({ state: { current }, dispatch }) => {
            await dispatch("preferences/SAVE_FORWARDING", current.value, { root: true });
        };
        this.registerSaveAction(save);

        this.value = { ...this.$store.state.preferences.mailboxFilter.forwarding };
    },
    methods: {
        async onSearch(pattern) {
            if (!pattern) {
                this.autocompleteResults = [];
                return;
            }
            const searchResults = await inject("AddressBooksPersistence").search(searchVCardsHelper(pattern));
            this.autocompleteResults = searchResults.values.map(vcardInfo => VCardInfoAdaptor.toContact(vcardInfo));
        },
        updateEmails(contacts) {
            this.value.emails = contacts
                .map(contact => contact.address)
                .filter(email => EmailValidator.validateAddress(email));
            this.autocompleteResults = [];
        },
        validateAddress: EmailValidator.validateAddress
    }
};
</script>
