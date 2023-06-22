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
        <mail-composer-recipient-button :recipient-type="$t(`common.${recipientType}`)" />
        <slot />
    </mail-contact-card-slots>
</template>

<script>
import debounce from "lodash/debounce";
import { mapMutations } from "vuex";
import { fetchContactMembers, RecipientAdaptor, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { ContactInput } from "@bluemind/business-components";
import { mailTipUtils } from "@bluemind/mail";

import apiAddressbooks from "~/store/api/apiAddressbooks";
import { SET_ADDRESS_WEIGHT, SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_TO } from "~/mutations";
import { ADDRESS_AUTOCOMPLETE } from "~/getters";
import { ComposerActionsMixin } from "~/mixins";
import MailContactCardSlots from "../MailContactCardSlots";
import MailComposerRecipientButton from "./MailComposerRecipientButton.vue";

const { getMailTipContext } = mailTipUtils;

export default {
    name: "MailComposerRecipient",
    components: { MailContactCardSlots, MailComposerRecipientButton },
    mixins: [ComposerActionsMixin],
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
        contacts() {
            return RecipientAdaptor.toContacts(this.message[this.recipientType]);
        },
        autocompleteExpandedResults() {
            let autocompleteExpandedResults;
            const { sortedAddresses } = this.$store.getters[`mail/${ADDRESS_AUTOCOMPLETE}`];
            if (this.searchResults?.total > 0) {
                // remove contacts already set and remove duplicates
                const contactsAlreadySet = this.contacts.map(({ address, dn }) => `${dn}<${address}>`);
                const searchResultKeyFn = contact => `${contact.value.formatedName || ""}<${contact.value.mail || ""}>`;
                const contacts = this.searchResults.values.reduce((result, contact) => {
                    if (
                        !contactsAlreadySet.includes(searchResultKeyFn(contact)) &&
                        !result.some(r => searchResultKeyFn(r) === searchResultKeyFn(contact))
                    ) {
                        result.push(contact);
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
        ...mapMutations("mail", { SET_ADDRESS_WEIGHT, SET_MESSAGE_TO, SET_MESSAGE_CC, SET_MESSAGE_BCC }),
        async expandContact(index) {
            const contacts = [...this.contacts];
            const contact = contacts[index];
            contact.members = await fetchContactMembers(contactContainerUid(contact), contact.uid);
            contacts.splice(index, 1, ...contact.members);
            this.update(contacts);
        },
        async search(searchedRecipient) {
            this.searchResults = searchedRecipient === "" ? null : await apiAddressbooks.search(searchedRecipient, -1);
        },
        async update(contacts) {
            this[`SET_MESSAGE_${this.recipientType.toUpperCase()}`]({
                messageKey: this.message.key,
                [this.recipientType]: contacts.map(c => ({
                    dn: c.dn || "",
                    address: c.address || "",
                    kind: c.kind,
                    memberCount: c.members?.length || 0,
                    uid: c.uid,
                    containerUid: c.urn?.split("@")[1]
                }))
            });
            this.getMailTips();
            this.debouncedSave();
        },
        validateDnAndAddress(input, contact) {
            if (contact.kind === "group") {
                return Boolean(contact.dn);
            }
            return contact.dn ? EmailValidator.validateDnAndAddress(input) : EmailValidator.validateAddress(input);
        },
        async getMailTips() {
            await this.$execute("get-mail-tips", { context: getMailTipContext(this.message), message: this.message });
        }
    }
};

function contactContainerUid(contact) {
    return contact.urn?.split("@")[1];
}
</script>
