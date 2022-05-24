import debounce from "lodash/debounce";
import { RecipientAdaptor, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { mapActions, mapMutations } from "vuex";
import { CHECK_CORPORATE_SIGNATURE } from "~/actions";
import apiAddressbooks from "~/store/api/apiAddressbooks";
import { SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_TO } from "~/mutations";
import ComposerActionsMixin from "./ComposerActionsMixin";

const recipientModes = { TO: 1, CC: 2, BCC: 4 }; // flags for the display mode of MailComposer's recipients fields
export default {
    mixins: [ComposerActionsMixin],
    props: {
        message: {
            type: Object,
            required: true
        },
        isReplyOrForward: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            recipientModes,
            /**
             * @example
             * $_EditRecipientsMixin_mode = (TO|CC|BCC) means we want to display all 3 fields
             * $_EditRecipientsMixin_mode = TO means we want to display TO field only
             */
            $_EditRecipientsMixin_mode: recipientModes.TO | recipientModes.CC | recipientModes.BCC,
            autocompleteResults: [],
            autocompleteResultsTo: [],
            autocompleteResultsCc: [],
            autocompleteResultsBcc: [],
            to: [],
            cc: [],
            bcc: []
        };
    },
    computed: {
        displayedRecipientFields: {
            get() {
                return (
                    this._data.$_EditRecipientsMixin_mode |
                    (this.message.to.length > 0 && recipientModes.TO) |
                    (this.message.cc.length > 0 && recipientModes.CC) |
                    (this.message.bcc.length > 0 && recipientModes.BCC)
                );
            },
            set(mode) {
                this._data.$_EditRecipientsMixin_mode = mode;
            }
        }
    },
    watch: {
        autocompleteResults: function () {
            this.autocompleteResultsTo = this.getAutocompleteResults("to");
            this.autocompleteResultsCc = this.getAutocompleteResults("cc");
            this.autocompleteResultsBcc = this.getAutocompleteResults("bcc");
        }
    },
    created() {
        this.to = RecipientAdaptor.toContacts(this.message.to);
        this.cc = RecipientAdaptor.toContacts(this.message.cc);
        this.bcc = RecipientAdaptor.toContacts(this.message.bcc);
    },
    async mounted() {
        this._data.$_EditRecipientsMixin_mode = this.isReplyOrForward
            ? recipientModes.TO
            : recipientModes.TO | recipientModes.CC;
        if (this.message.to.length === 0) {
            await this.$nextTick();
            this.$refs.to.focus();
        }
    },
    methods: {
        ...mapActions("mail", { CHECK_CORPORATE_SIGNATURE }),
        ...mapMutations("mail", { SET_MESSAGE_TO, SET_MESSAGE_CC, SET_MESSAGE_BCC }),
        async expandContact(contacts, index) {
            const contact = contacts[index];
            contact.entries = await fetchContactMembers(contact);
            contacts.splice(index, 1, ...contact.entries.map(e => ({ entries: [e] })));
        },
        onSearch(fieldFocused, searchedPattern) {
            this.fieldFocused = fieldFocused;
            this.search(searchedPattern);
        },
        search: debounce(function (searchedRecipient) {
            if (searchedRecipient === "") {
                this.autocompleteResults = [];
            } else {
                return apiAddressbooks.search(searchedRecipient).then(results => {
                    if (results.values.length === 0) {
                        this.autocompleteResults = undefined;
                    } else {
                        this.autocompleteResults = results.values.map(vcardInfo =>
                            VCardInfoAdaptor.toContact(vcardInfo)
                        );
                    }
                });
            }
        }, 200),
        getAutocompleteResults(fromField) {
            if (fromField !== this.fieldFocused || this.autocompleteResults === undefined) {
                return [];
            }
            if (this.autocompleteResults.length > 0) {
                return this.autocompleteResults;
            }
        },
        async updateTo(contacts) {
            this.SET_MESSAGE_TO({ messageKey: this.message.key, to: await contactsToRecipients(contacts) });
            this.CHECK_CORPORATE_SIGNATURE({ message: this.message });
            this.debouncedSave();
        },
        async updateCc(contacts) {
            this.SET_MESSAGE_CC({ messageKey: this.message.key, cc: await contactsToRecipients(contacts) });
            this.CHECK_CORPORATE_SIGNATURE({ message: this.message });
            this.debouncedSave();
        },
        async updateBcc(contacts) {
            this.SET_MESSAGE_BCC({ messageKey: this.message.key, bcc: await contactsToRecipients(contacts) });
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

async function contactToRecipients(contact) {
    return contact.kind === "group" ? await fetchContactMembers(contact) : contact.entries;
}

async function contactsToRecipients(contacts) {
    return (await Promise.all(contacts.map(contactToRecipients))).flatMap(r => r);
}

async function fetchContactMembers(contact) {
    const containerUid = contact.urn?.split("@")[1];
    const vCard = await inject("AddressBookPersistence", containerUid).getComplete(contact.uid);
    return vCard && vCard.value.kind === "group" && vCard.value.organizational?.member?.length
        ? vCard.value.organizational.member.map(m => ({
              dn: m.commonName,
              address: m.mailto,
              urn: `${m.itemUid}@${m.containerUid}`,
              uid: m.itemUid
          }))
        : [];
}
