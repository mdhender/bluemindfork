<template>
    <bm-modal
        id="recipient-picker"
        dialog-class="mail-composer-recipient-modal"
        :title="$t('recipient_picker.title')"
        size="xl"
        body-class="overflow-hidden d-flex flex-column"
        centered
    >
        <selected-contacts :contacts.sync="selectedContacts" />
        <hr />
        <div class="recipient-modal-body d-flex flex-fill">
            <address-book-list
                :addressbooks="addressBooks"
                :user-id="userId"
                :selected-addressbook="selectedAddressBookId"
                @selected="selectedAddressBookId = $event"
            />
            <contact-list
                class="flex-fill"
                :contacts="contacts"
                :loading="loading"
                :addressbook="selectedAddressBook"
                :user-id="userId"
                :selected="selectedForCurrentAddressBook"
                @selected="updateSelected"
            />
        </div>
        <template #modal-footer>
            <div>
                <span v-if="selectedContacts.length" class="bold mr-6">
                    {{
                        $tc("recipient_picker.selected.count", selectedContacts.length, {
                            count: selectedContacts.length
                        })
                    }}
                </span>
                <bm-button variant="fill-accent" @click="$bvModal.hide('recipient-picker')">
                    {{ $t("common.done") }}
                </bm-button>
            </div>
        </template>
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="{ alert }"><component :is="alert.renderer" :alert="alert" /></template>
        </bm-alert-area>
    </bm-modal>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { ERROR, REMOVE } from "@bluemind/alert.store";
import { inject } from "@bluemind/inject";
import { BmAlertArea, BmButton, BmModal } from "@bluemind/ui-components";
import AddressBookList from "./AddressBookList";
import ContactList from "./ContactList";
import SelectedContacts from "./SelectedContacts";

export default {
    name: "MailComposerRecipientModal",
    components: { BmAlertArea, BmButton, BmModal, AddressBookList, ContactList, SelectedContacts },
    props: {
        selected: { type: Array, default: () => [] }
    },
    data() {
        return {
            addressBooks: [],
            contacts: [],
            loading: false,
            selectedAddressBookId: undefined,
            userId: undefined
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "recipient-picker") }),
        selectedContacts: {
            get() {
                return this.selected;
            },
            set(value) {
                this.$emit("update:selected", value);
            }
        },
        selectedAddressBook() {
            return this.addressBooks.find(a => a.uid === this.selectedAddressBookId) || {};
        },
        selectedForCurrentAddressBook() {
            return this.selected
                .map(({ dn, address }) => this.contacts.find(({ name, email }) => dn === name && address === email))
                .filter(Boolean);
        }
    },
    watch: {
        async selectedAddressBookId(value) {
            this.loading = true;
            try {
                if (!value) return [];
                const ids = await inject("AddressBookPersistence", value).sortedIds();
                this.contacts = (await inject("AddressBookPersistence", value).multipleGetById(ids)).map(contact => ({
                    uid: contact.uid,
                    name: contact.value.identification.formatedName.value,
                    email: extractDefaultCommunication(contact, "emails"),
                    tel: extractDefaultCommunication(contact, "tels"),
                    kind: contact.value.kind,
                    members: contact.value.organizational?.member,
                    urn: `${contact.uid}@${value}`
                }));
            } finally {
                this.loading = false;
            }

            function extractDefaultCommunication(contact, targetKey) {
                return (
                    contact.value.communications[targetKey].find(
                        key => key.parameters.find(param => param.label === "DEFAULT" && param.value === true) !== -1
                    )?.value ?? ""
                );
            }
        }
    },
    async created() {
        this.userId = inject("UserSession").userId;
        this.addressBooks = await inject("ContainersPersistence").getContainers(await this.subscribedContainerUids());
    },
    methods: {
        ...mapActions("alert", { ERROR, REMOVE }),
        async subscribedContainerUids() {
            return (await inject("OwnerSubscriptionsPersistence").list())
                .filter(sub => sub.value.containerType === "addressbook")
                .map(sub => sub.value.containerUid);
        },
        updateSelected(contacts) {
            const contactUids = contacts.map(({ uid }) => uid);
            const selected = [...this.selected];
            const previousUids = [];
            this.selectedForCurrentAddressBook.forEach(previous => {
                previousUids.push(previous.uid);
                if (!contactUids.includes(previous.uid)) {
                    const index = selected.findIndex(s => s.dn === previous.name && s.address === previous.email);
                    selected.splice(index, 1);
                }
            });
            contacts.forEach(item => {
                if (!previousUids.includes(item.uid)) {
                    selected.push(toContact(item));
                }
            });
            this.selectedContacts = selected;
        }
    }
};

function toContact(contactItem) {
    return {
        address: contactItem.email,
        dn: contactItem.name,
        uid: contactItem.uid,
        kind: contactItem.kind,
        members: contactItem.members,
        urn: contactItem.urn
    };
}
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/type";
@import "@bluemind/ui-components/src/css/utils/variables";

.mail-composer-recipient-modal {
    .modal-header {
        background-color: $neutral-bg-lo1;
        padding-top: base-px-to-rem(16);
        padding-bottom: base-px-to-rem(13);
        padding-left: $sp-7;
    }
    .modal-content {
        height: 80vh;
    }
    .modal-body {
        padding: 0;
        background-color: $backdrop;
    }
    hr {
        margin: 0;
    }
    .recipient-modal-body {
        gap: $sp-4;
    }
    .modal-footer {
        padding: $sp-4;
        z-index: 1;
        box-shadow: $box-shadow-sm;
    }
}
</style>
