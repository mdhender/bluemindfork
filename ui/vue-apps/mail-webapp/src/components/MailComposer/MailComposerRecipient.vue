<template>
    <mail-contact-card-slots
        ref="contact-input"
        :component="ContactInput"
        class="mail-composer-recipient w-100"
        :class="{ 'expanded-search': expandSearch }"
        variant="underline"
        :contacts="contacts"
        :autocomplete-results="expandSearch ? autocompleteExpandedResults : autocompleteResults"
        :validate-address-fn="validateDnAndAddress"
        :show-expand="showExpand"
        extension="mail.composer.recipients"
        @search="debouncedSearch"
        @update:contacts="update"
        @expand="expandContact"
        @expandSearch="expandSearch = true"
        @autocompleteHidden="expandSearch = false"
        @delete="SET_ADDRESS_WEIGHT({ address: $event.address, weight: -1 })"
    >
        <mail-composer-recipient-button :recipient-type="$t(`common.${recipientType}`)" v-on="$listeners" />
        <slot />
    </mail-contact-card-slots>
</template>

<script>
import debounce from "lodash/debounce";
import { mapMutations } from "vuex";
import { contactContainerUid, fetchContactMembers, RecipientAdaptor,removeDuplicatedContacts, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { ContactInput } from "@bluemind/business-components";
import { mailTipUtils } from "@bluemind/mail";

import apiAddressbooks from "~/store/api/apiAddressbooks";
import { ADDRESS_AUTOCOMPLETE } from "~/getters";
import { SET_ADDRESS_WEIGHT } from "~/mutations";

import { ComposerActionsMixin } from "~/mixins";
import MailContactCardSlots from "../MailContactCardSlots";
import MailComposerRecipientButton from "./MailComposerRecipientButton.vue";

export default {
    name: "MailComposerRecipient",
    components: { MailContactCardSlots, MailComposerRecipientButton },
    props: {
        message: { type: Object, required: true },
        recipientType: { type: String, required: true, validator: value => ["to", "cc", "bcc"].includes(value) }
    },
    data() {
        return {
            searchResults: undefined,
            debouncedSearch: debounce(this.search, 200),
            expandSearch: false,
            ContactInput
        };
    },
    computed: {
        contacts: {
            get() {
                return RecipientAdaptor.toContacts(this.message[this.recipientType]);
            },
            set(updatedContacts) {
                this.$emit("update:contacts", updatedContacts);
            }
        },
        autocompleteExpandedResults() {
            let autocompleteExpandedResults;
            const { sortedAddresses } = this.$store.getters[`mail/${ADDRESS_AUTOCOMPLETE}`];

            if (this.searchResults?.total > 0) {
                // remove contacts already set and remove duplicates
                const contactsAlreadySet = this.contacts.reduce(
                    (set, { address, dn }) => set.add(`${dn}<${address}>`),
                    new Set()
                );

                const searchResultKeyFn = contact => `${contact.value.formatedName || ""}<${contact.value.mail}>`;
                const contacts = this.searchResults.values.reduce((result, contact) => {
                    const contactKey = searchResultKeyFn(contact);
                    if (!contactsAlreadySet.has(contactKey)) {
                        result.push(contact);
                        contactsAlreadySet.add(contactKey);
                    }
                    return result;
                }, []);

                // sort by priority
                const priorityFn = address => sortedAddresses.indexOf(address) || Number.MAX_VALUE;
                contacts.sort((a, b) => priorityFn(b.value.mail) - priorityFn(a.value.mail));

                autocompleteExpandedResults = contacts.map(VCardInfoAdaptor.toContact);
            }
            return autocompleteExpandedResults || [];
        },
        autocompleteResults() {
            let autocompleteResults;
            const { excludedAddresses } = this.$store.getters[`mail/${ADDRESS_AUTOCOMPLETE}`];
            if (this.autocompleteExpandedResults) {
                autocompleteResults = this.autocompleteExpandedResults.filter(
                    ({ address }) => !excludedAddresses.includes(address)
                );
            }
            return autocompleteResults || [];
        },
        showExpand() {
            return (
                !this.expandSearch &&
                (this.autocompleteResults.length > 5 ||
                    this.autocompleteExpandedResults.length > this.autocompleteResults.length)
            );
        }
    },
    mounted() {
        if (this.recipientType === "to" && this.message.to.length === 0) {
            this.$nextTick(() => this.$refs["contact-input"].$children[0].focus());
        }
    },
    methods: {
        ...mapMutations("mail", { SET_ADDRESS_WEIGHT }),
        async expandContact(index) {
            const contacts = [...this.contacts];
            const contact = contacts[index];
            contact.members = await fetchContactMembers(contactContainerUid(contact), contact.uid);
            contacts.splice(index, 1, ...contact.members);
            this.update(removeDuplicatedContacts(contacts));
        },

        async search(searchedRecipient) {
            this.searchResults = searchedRecipient === "" ? null : await apiAddressbooks.search(searchedRecipient, -1);
        },
        update(contacts) {
            this.contacts = contacts;
        },
        validateDnAndAddress(input, contact) {
            if (contact.kind === "group") {
                return Boolean(contact.dn);
            }
            return contact.dn ? EmailValidator.validateDnAndAddress(input) : EmailValidator.validateAddress(input);
        }
    }
};
</script>
