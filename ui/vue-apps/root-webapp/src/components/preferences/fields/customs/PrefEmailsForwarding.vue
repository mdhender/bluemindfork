<template>
    <div class="pref-emails-forwarding">
        <bm-form-checkbox v-model="value.enabled">
            {{ $t("preferences.mail.emails_forwarding.to") }}
        </bm-form-checkbox>
        <contact-input
            class="mr-5 pref-field-input"
            :auto-collapsible="false"
            :disabled="!value.enabled"
            :contacts="contacts"
            :autocomplete-results="autocompleteResults"
            :validate-address-fn="validateAddress"
            @search="onSearch"
            @update:contacts="updateEmails"
        />
        <bm-form-checkbox v-model="value.localCopy" :disabled="!value.enabled" @click.prevent.stop>{{
            $t("preferences.mail.emails_forwarding.local_copy")
        }}</bm-form-checkbox>
    </div>
</template>

<script>
import { searchVCardsHelper, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { BmFormCheckbox } from "@bluemind/ui-components";
import { ContactInput } from "@bluemind/business-components";
import CentralizedSaving from "../../mixins/CentralizedSaving";

export default {
    name: "PrefEmailsForwarding",
    components: { BmFormCheckbox, ContactInput },
    mixins: [CentralizedSaving],
    data() {
        return {
            autocompleteResults: []
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
            const searchResults = await inject("AddressBooksPersistence").search(
                searchVCardsHelper(pattern, { size: 5, noGroup: true })
            );
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

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-emails-forwarding {
    display: flex;
    flex-direction: column;
    gap: $sp-5;

    .contact-input {
        max-width: 100%;
    }
}
</style>
