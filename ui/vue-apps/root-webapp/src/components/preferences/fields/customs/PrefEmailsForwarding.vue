<template>
    <div>
        <bm-form-checkbox v-model="forwarding.enabled" class="mb-3">
            {{ $t("preferences.mail.emails_forwarding.to") }}
        </bm-form-checkbox>
        <div class="d-flex">
            <bm-contact-input
                class="mr-2 border border-dark"
                :disabled="!forwarding.enabled"
                :contacts="contacts"
                :autocomplete-results="autocompleteResults"
                :validate-address-fn="validateAddress"
                @search="onSearch"
                @update:contacts="updateEmails"
            />
            <bm-form-select v-model="forwarding.localCopy" :options="options" :disabled="!forwarding.enabled" />
        </div>
    </div>
</template>

<script>
import { VCardQueryOrderBy } from "@bluemind/addressbook.api";
import { getQuery, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { BmContactInput, BmFormCheckbox, BmFormSelect } from "@bluemind/styleguide";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefEmailsForwarding",
    components: { BmContactInput, BmFormCheckbox, BmFormSelect },
    data() {
        return {
            forwarding: {
                emails: [],
                enabled: false,
                localCopy: false
            },
            autocompleteResults: [],
            options: [
                { text: this.$t("preferences.mail.emails_forwarding.no_local_copy"), value: false },
                { text: this.$t("preferences.mail.emails_forwarding.local_copy"), value: true }
            ]
        };
    },
    computed: {
        ...mapState("preferences", {
            isMailboxFilterLoaded: ({ mailboxFilter }) => mailboxFilter.loaded,
            localForwarding: ({ mailboxFilter }) => mailboxFilter.local.forwarding
        }),
        contacts() {
            return this.forwarding.emails.map(email => ({ address: email, dn: "" }));
        }
    },
    watch: {
        isMailboxFilterLoaded() {
            if (this.isMailboxFilterLoaded) {
                this.init();
            }
        },
        localForwarding() {
            if (JSON.stringify(this.localForwarding) !== JSON.stringify(this.forwarding)) {
                this.init();
            }
        },
        forwarding: {
            handler() {
                this.SET_FORWARDING(this.forwarding);
            },
            deep: true
        }
    },
    methods: {
        ...mapMutations("preferences", ["SET_FORWARDING"]),
        async init() {
            this.forwarding = JSON.parse(JSON.stringify(this.localForwarding));
        },
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
            this.forwarding.emails = contacts
                .map(contact => contact.address)
                .filter(email => EmailValidator.validateAddress(email));
            this.autocompleteResults = [];
        },
        validateAddress: EmailValidator.validateAddress
    }
};
</script>
