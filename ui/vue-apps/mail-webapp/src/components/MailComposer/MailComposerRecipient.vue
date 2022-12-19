<template>
    <bm-contact-input
        ref="contact-input"
        class="mail-composer-recipient w-100"
        :class="{ 'expanded-search': expandSearch }"
        variant="underline"
        :contacts="contacts"
        :autocomplete-results="expandSearch ? autocompleteExpandedResults : autocompleteResults"
        :validate-address-fn="validateDnAndAddress"
        :show-expand="showExpand"
        @search="debouncedSearch"
        @update:contacts="update"
        @expand="expandContact(contacts, $event, update)"
        @expandSearch="expandSearch = true"
        @autocompleteHidden="expandSearch = false"
        @delete="SET_ADDRESS_WEIGHT({ address: $event.address, weight: -1 })"
    >
        {{ $t(`common.${recipientType}`) }}
    </bm-contact-input>
</template>

<script>
import debounce from "lodash/debounce";
import { mapActions, mapMutations } from "vuex";
import { RecipientAdaptor, VCardAdaptor, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { BmContactInput } from "@bluemind/ui-components";
import { CHECK_CORPORATE_SIGNATURE } from "~/actions";
import apiAddressbooks from "~/store/api/apiAddressbooks";
import { SET_ADDRESS_WEIGHT, SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_TO } from "~/mutations";
import { ADDRESS_AUTOCOMPLETE } from "~/getters";
import { ComposerActionsMixin } from "~/mixins";

export default {
    name: "MailComposerRecipient",
    components: { BmContactInput },
    mixins: [ComposerActionsMixin],
    props: {
        message: { type: Object, required: true },
        recipientType: { type: String, required: true, validator: value => ["to", "cc", "bcc"].includes(value) }
    },
    data() {
        return {
            searchResults: undefined,
            debouncedSearch: debounce(this.search, 200),
            expandSearch: false
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
                const contacts = this.searchResults.values;
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
            this.$nextTick(() => this.$refs["contact-input"].focus());
        }
    },
    methods: {
        ...mapActions("mail", { CHECK_CORPORATE_SIGNATURE }),
        ...mapMutations("mail", { SET_ADDRESS_WEIGHT, SET_MESSAGE_TO, SET_MESSAGE_CC, SET_MESSAGE_BCC }),
        async expandContact(contacts, index, updateFn) {
            const contact = contacts[index];
            contact.members = await fetchContactMembers(contactContainerUid(contact), contact.uid);
            contacts.splice(index, 1, ...contact.members);
            updateFn(contacts);
        },
        async search(searchedRecipient) {
            this.searchResults = searchedRecipient === "" ? null : await apiAddressbooks.search(searchedRecipient);
        },
        async update(contacts) {
            this[`SET_MESSAGE_${this.recipientType.toUpperCase()}`]({
                messageKey: this.message.key,
                [this.recipientType]: await contactsToRecipients(contacts)
            });
            this.CHECK_CORPORATE_SIGNATURE({ message: this.message });
            this.debouncedSave();
        },
        validateAddress(input, contact) {
            return contact.kind === "group" ? !!contact.dn : EmailValidator.validateAddress(input);
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

/** Recursively fetch members having an address. */
async function fetchMembersWithAddress(containerUid, contactUid) {
    const vCard = await fetchContact(containerUid, contactUid);
    const members = vCard?.value.organizational?.member;
    return members?.length
        ? (
              await Promise.all(
                  members.map(async m =>
                      m.mailto
                          ? { address: m.mailto, dn: m.commonName }
                          : await fetchMembersWithAddress(m.containerUid, m.itemUid)
                  )
              )
          ).flatMap(r => r)
        : [];
}

/** Fetch first level members, with extended info. */
async function fetchContactMembers(containerUid, contactUid) {
    const vCard = await fetchContact(containerUid, contactUid);
    const members = vCard?.value.organizational?.member;
    return members?.length
        ? Promise.all(
              members.map(async m => {
                  const memberVCard = await fetchContact(m.containerUid, m.itemUid);
                  return {
                      ...VCardAdaptor.toContact(memberVCard),
                      uid: m.itemUid,
                      urn: `${m.itemUid}@${m.containerUid}`
                  };
              })
          )
        : [];
}

function fetchContact(containerUid, uid) {
    return inject("AddressBookPersistence", containerUid).getComplete(uid);
}
</script>
