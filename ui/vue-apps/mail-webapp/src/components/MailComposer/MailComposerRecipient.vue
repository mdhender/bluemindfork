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
        @search="debouncedSearch"
        @update:contacts="update"
        @expand="expandContact(contacts, $event)"
        @expandSearch="expandSearch = true"
        @autocompleteHidden="expandSearch = false"
        @delete="SET_ADDRESS_WEIGHT({ address: $event.address, weight: -1 })"
    >
        {{ $t(`common.${recipientType}`) }}
    </mail-contact-card-slots>
</template>

<script>
import debounce from "lodash/debounce";
import { mapActions, mapMutations } from "vuex";
import { fetchContactMembers, fetchMembersWithAddress, RecipientAdaptor, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { ContactInput } from "@bluemind/business-components";
import { CHECK_CORPORATE_SIGNATURE } from "~/actions";
import apiAddressbooks from "~/store/api/apiAddressbooks";
import { SET_ADDRESS_WEIGHT, SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_TO } from "~/mutations";
import { ADDRESS_AUTOCOMPLETE } from "~/getters";
import { ComposerActionsMixin } from "~/mixins";
import MailContactCardSlots from "../MailContactCardSlots";

export default {
    name: "MailComposerRecipient",
    components: { MailContactCardSlots },
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
            if (this.searchResults) {
                // remove contacts already set and remove duplicates
                const contactsAlreadySet = this.contacts.map(({ address, dn }) => `${dn}<${address}>`);
                const searchResultKeyFn = contact => `${contact.value.formatedName}<${contact.value.mail}>`;
                const contacts = this.searchResults.values?.reduce((result, contact) => {
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
                contacts?.sort((a, b) => priorityFn(b.value.mail) - priorityFn(a.value.mail));

                autocompleteExpandedResults = contacts?.map(vcardInfo => VCardInfoAdaptor.toContact(vcardInfo));
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
                this.autocompleteResults?.length < 5 &&
                this.autocompleteExpandedResults?.length > this.autocompleteResults?.length
            );
        }
    },
    mounted() {
        if (this.recipientType === "to" && this.message.to.length === 0) {
            this.$nextTick(() => this.$refs["contact-input"].$children[0].focus());
        }
    },
    methods: {
        ...mapActions("mail", { CHECK_CORPORATE_SIGNATURE }),
        ...mapMutations("mail", { SET_ADDRESS_WEIGHT, SET_MESSAGE_TO, SET_MESSAGE_CC, SET_MESSAGE_BCC }),
        async expandContact(contacts, index) {
            const contact = contacts[index];
            contact.members = await fetchContactMembers(contactContainerUid(contact), contact.uid);
            contacts.splice(index, 1, ...contact.members);
            this.update(contacts, true);
        },
        async search(searchedRecipient) {
            this.searchResults = searchedRecipient === "" ? null : await apiAddressbooks.search(searchedRecipient, -1);
        },
        async update(contacts, expand) {
            this[`SET_MESSAGE_${this.recipientType.toUpperCase()}`]({
                messageKey: this.message.key,
                [this.recipientType]: expand
                    ? await contactsToRecipients(contacts, expand)
                    : contacts.map(c => ({
                          dn: c.dn || "",
                          address: c.address || "",
                          kind: c.kind,
                          memberCount: c.members?.length || 0,
                          uid: c.uid,
                          containerUid: c.urn?.split("@")[1]
                      }))
            });
            this.CHECK_CORPORATE_SIGNATURE({ message: this.message });
            this.debouncedSave();
        },
        validateDnAndAddress(input, contact) {
            return contact.kind === "group" ? !!contact.dn : EmailValidator.validateDnAndAddress(input);
        }
    }
};

/**
 * Obtain a list of recipients based on the address:
 * - if the address has a value, return the recipient representing this contact
 * - if not, return the recipients of the members having an address value (recursively)
 */
async function contactToRecipients(contact) {
    if (contact.address) {
        return [contact];
    } else if (contact.members?.length) {
        return await fetchMembersWithAddress(contactContainerUid(contact), contact.uid);
    }
    return [];
}

function contactContainerUid(contact) {
    return contact.urn?.split("@")[1];
}

async function contactsToRecipients(contacts) {
    return (await Promise.all(contacts.map(contactToRecipients))).flatMap(r => r);
}
</script>
