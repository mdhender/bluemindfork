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
import { VCardQueryOrderBy } from "@bluemind/addressbook.api";
import { getQuery, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { BmContactInput, BmFormCheckbox, BmFormSelect } from "@bluemind/styleguide";
import { mapMutations, mapState } from "vuex";
import CentralizedSaving from "../../mixins/CentralizedSaving";

export default {
    name: "PrefEmailsForwarding",
    components: { BmContactInput, BmFormCheckbox, BmFormSelect },
    mixins: [CentralizedSaving],
    data() {
        return {
            value: {
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
            isMailboxFilterLoaded: ({ mailboxFilter }) => mailboxFilter.loaded
        }),
        contacts() {
            return this.value.emails.map(email => ({ address: email, dn: "" }));
        }
    },
    watch: {
        isMailboxFilterLoaded() {
            if (this.isMailboxFilterLoaded) {
                this.init();
            }
        }
    },
    created() {
        const save = async ({ state: { current, saved }, dispatch }) => {
            if (current && !current.options.saved) {
                try {
                    await dispatch("preferences/SAVE_MAILBOX_FILTER", { forwarding: current.value }, { root: true });
                    this.PUSH_STATE({ value: current.value, options: { saved: true } });
                } catch {
                    this.PUSH_STATE(saved);
                }
            }
        };
        this.registerSaveAction(save);
    },
    methods: {
        ...mapMutations("preferences", ["SET_FORWARDING"]),
        async init() {
            this.value = { ...this.$store.state.preferences.mailboxFilter.remote.forwarding };
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
            this.value.emails = contacts
                .map(contact => contact.address)
                .filter(email => EmailValidator.validateAddress(email));
            this.autocompleteResults = [];
        },
        validateAddress: EmailValidator.validateAddress
    }
};
</script>
