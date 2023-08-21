<template>
    <bm-modal
        id="recipient-picker"
        dialog-class="mail-composer-recipient-modal"
        :title="$t('recipient_picker.title')"
        size="xl"
        body-class="overflow-hidden d-flex flex-column"
        centered
        @change="search = ''"
    >
        <div class="select-wrapper d-lg-none">
            <address-book-mobile-dropdown
                :address-books="addressBooks"
                :selected-address-book="selectedAddressBook"
                :user-id="userId"
                @selected="selectedAddressBookId = $event"
            />
        </div>

        <selected-contacts :contacts.sync="selectedContacts" :contacts-type="recipientContactsType" />
        <hr />

        <div class="recipient-modal-body d-flex flex-fill">
            <address-book-list
                class="d-none d-lg-flex"
                :addressbooks="addressBooks"
                :user-id="userId"
                :selected-addressbook="selectedAddressBookId"
                @selected="selectedAddressBookId = $event"
            />
            <div class="d-flex flex-column flex-fill">
                <bm-form-input
                    ref="inputSearch"
                    v-model="search"
                    :icon="!resettable ? 'search' : 'cancel'"
                    left-icon
                    :resettable="resettable"
                    :placeholder="
                        $t('recipient_picker.search_input.placeholder', { addressBookName: selectedAddressBook.name })
                    "
                    class="search-input"
                    variant="underline"
                    @reset="search = ''"
                    @keydown.enter="
                        resetBeforeSearchIfRequired(search);
                        performSearch(search);
                    "
                />
                <contact-list
                    class="flex-fill"
                    :contacts="contacts"
                    :loading="loading"
                    :search="search"
                    :addressbook="selectedAddressBook"
                    :user-id="userId"
                    :selected="selectedForCurrentAddressBook"
                    @selected="updateSelected"
                    @reset-search="
                        search = '';
                        $refs.inputSearch.focus();
                    "
                />
            </div>
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
            <template #default="{ alert }"><component :is="alert.renderer" :alert="alert" /></template>
        </bm-alert-area>
    </bm-modal>
</template>

<script>
import debounce from "lodash.debounce";
import { mapActions, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { ERROR, REMOVE } from "@bluemind/alert.store";
import { searchVCardsHelper, sortAddressBooks } from "@bluemind/contact";
import { BmAlertArea, BmButton, BmModal, BmFormInput } from "@bluemind/ui-components";
import AddressBookLabelIcon from "./AddressBookLabelIcon";
import AddressBookList from "./AddressBookList";
import AddressBookMobileDropdown from "./AddressBookMobileDropdown.vue";
import ContactList from "./ContactList";
import SelectedContacts from "./SelectedContacts";

export default {
    name: "MailComposerRecipientModal",
    components: {
        AddressBookLabelIcon,
        AddressBookList,
        AddressBookMobileDropdown,
        BmAlertArea,
        BmButton,
        BmFormInput,
        BmModal,
        ContactList,
        SelectedContacts
    },
    props: {
        selected: { type: Array, default: () => [] },
        recipientContactsType: { type: String, default: "" }
    },
    data() {
        return {
            addressBooks: [],
            loading: false,
            userId: undefined,
            allContacts: [],
            searchedContacts: [],
            search: "",
            selectedAddressBookId: undefined,
            highlightPattern: "",
            debounceSearch: debounce(function (searchValue) {
                this.performSearch(searchValue);
            }, 500)
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
        },
        contacts() {
            const contacts = this.isSearching ? this.searchedContacts : this.allContacts;
            return this.sortByName(contacts.slice());
        },
        isSearching() {
            return this.resettable && this.searchedContacts != null;
        },
        resettable() {
            return Boolean(this.search);
        }
    },
    watch: {
        async selectedAddressBookId(addressbookId) {
            this.search = "";
            this.loading = true;

            try {
                if (!addressbookId) return [];
                const addressbookRepository = inject("AddressBookPersistence", addressbookId);
                const ids = await addressbookRepository.sortedIds();
                this.allContacts = (await addressbookRepository.multipleGetById(ids)).map(contact => ({
                    uid: contact.uid,
                    name: contact.value.identification.formatedName.value,
                    email: extractDefaultCommunication(contact, "emails"),
                    tel: extractDefaultCommunication(contact, "tels"),
                    kind: contact.value.kind,
                    members: contact.value.organizational?.member,
                    urn: `${contact.uid}@${addressbookId}`
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
        },
        search(searchValue) {
            this.resetBeforeSearchIfRequired(searchValue);
            this.debounceSearch(searchValue);
        }
    },
    async created() {
        this.userId = inject("UserSession").userId;
        this.addressBooks = sortAddressBooks(
            await inject("ContainersPersistence").getContainers(await this.subscribedContainerUids()),
            this.userId
        );
    },
    methods: {
        resetBeforeSearchIfRequired(searchValue) {
            if (!this.searchedContacts?.length || searchValue === "") {
                this.searchedContacts = null;
                this.highlightPattern = "";
            }
        },

        async subscribedContainerUids() {
            return (await inject("OwnerSubscriptionsPersistence").list())
                .filter(sub => sub.value.containerType === "addressbook")
                .map(sub => sub.value.containerUid);
        },
        addressbookById(addressbookId) {
            return this.addressBooks.find(a => a.uid === addressbookId) || {};
        },
        async performSearch(searchValue) {
            if (searchValue) {
                this.loading = true;

                const searchResults = await inject("AddressBooksPersistence").search(
                    searchVCardsHelper(searchValue, 50, false, this.selectedAddressBookId)
                );
                this.searchedContacts =
                    searchResults.total > 0
                        ? searchResults.values.map(contact => ({
                              uid: contact.uid,
                              name: contact.displayName,
                              email: contact.value.mail,
                              tel: contact.value.tel
                          }))
                        : [];

                this.loading = false;
                this.debounceSearch.cancel();
            }
            await this.$nextTick();
            this.highlightPattern = searchValue;
        },
        ...mapActions("alert", { ERROR, REMOVE }),
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
        },
        sortByName(contacts) {
            return contacts.sort((a, b) => a.name.trim().localeCompare(b.name.trim()));
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

        .select-wrapper {
            background-color: $surface;
            padding: $sp-5;
            .bm-form-select .address-book-label-icon {
                max-width: 100%;
                &,
                > div {
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
            }
        }
        .address-book-list {
            max-width: 20%;
        }
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
    .bm-form-input {
        background-color: $surface;
    }
}
</style>
