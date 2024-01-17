<template>
    <bm-modal
        id="recipient-picker"
        dialog-class="mail-composer-recipient-modal"
        variant="advanced"
        size="xl"
        height="md"
        @change="search = ''"
    >
        <template #modal-header="{ close }">
            <bm-modal-header
                :title="$t('recipient_picker.title')"
                @close="
                    if (showMobileSearchInput) showMobileSearchInput = false;
                    else close();
                "
            >
                <bm-form-input
                    v-if="showMobileSearchInput"
                    ref="mobile-search-input"
                    v-model="search"
                    class="mobile-search-input mobile-only"
                    autofocus
                    icon="search"
                    left-icon
                    :placeholder="
                        $t('recipient_picker.search_input.placeholder', { addressBookName: selectedAddressBook.name })
                    "
                    variant="inline-on-fill-primary"
                    @keydown.enter="
                        resetBeforeSearchIfRequired(search);
                        performSearch(search);
                    "
                />
                <template v-else>
                    <address-book-mobile-dropdown
                        class="mobile-only"
                        :address-books="addressBooks"
                        :selected-address-book="selectedAddressBook"
                        :user-id="userId"
                        @selected="selectedAddressBookId = $event"
                    />
                    <bm-icon-button
                        class="mobile-only mx-3"
                        variant="compact-on-fill-primary"
                        size="lg"
                        icon="search"
                        @click="showMobileSearchInput = true"
                    />
                </template>
            </bm-modal-header>
        </template>

        <selected-contacts :contacts.sync="selectedContacts" :contacts-type="recipientContactsType" />
        <hr />

        <div class="d-flex flex-fill">
            <address-book-list
                class="desktop-only"
                :addressbooks="addressBooks"
                :user-id="userId"
                :selected-addressbook="selectedAddressBookId"
                @selected="selectedAddressBookId = $event"
            />
            <div class="d-flex flex-column flex-fill">
                <bm-form-input
                    ref="desktop-search-input"
                    v-model="search"
                    class="desktop-search-input desktop-only"
                    :icon="!resettable ? 'search' : 'cancel'"
                    left-icon
                    :resettable="resettable"
                    :placeholder="
                        $t('recipient_picker.search_input.placeholder', { addressBookName: selectedAddressBook.name })
                    "
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
                        displayedSearchInput().focus();
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
import { Fields as SearchFields, searchVCardsHelper, sortAddressBooks } from "@bluemind/contact";
import { BmAlertArea, BmButton, BmIconButton, BmModal, BmModalHeader, BmFormInput } from "@bluemind/ui-components";
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
        BmIconButton,
        BmFormInput,
        BmModal,
        BmModalHeader,
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
            showMobileSearchInput: false,
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
                if (!addressbookId) {
                    return;
                }
                const addressbookRepository = inject("AddressBookPersistence", addressbookId);
                const ids = await addressbookRepository.sortedIds();
                this.allContacts = (await addressbookRepository.multipleGetById(ids)).map(contact =>
                    transformRawContact(contact, addressbookId)
                );
            } finally {
                this.loading = false;
            }
        },
        search(searchValue) {
            this.resetBeforeSearchIfRequired(searchValue);
            this.debounceSearch(searchValue);
        },
        showMobileSearchInput(value) {
            if (value === false) {
                this.search = "";
            }
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
                const addressbookRepository = inject("AddressBookPersistence", this.selectedAddressBookId);
                const queryObject = searchVCardsHelper(
                    searchValue,
                    50,
                    false,
                    this.selectedAddressBookId,
                    SearchFields.EMAIL,
                    SearchFields.NAME,
                    SearchFields.COMPANY
                );
                const searchResults = await addressbookRepository.search(queryObject);

                this.searchedContacts =
                    searchResults.total > 0
                        ? (
                              await addressbookRepository.multipleGetById(
                                  searchResults.values.map(({ internalId }) => internalId)
                              )
                          ).map(contact => transformRawContact(contact, this.selectedAddressBookId))
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
        },

        displayedSearchInput() {
            const el = this.$refs["mobile-search-input"]?.$el;
            if (el && window.getComputedStyle(el).display !== "none") {
                return this.$refs["mobile-search-input"];
            }
            return this.$refs["desktop-search-input"];
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

function extractDefaultCommunication(contact, targetKey) {
    return (
        contact.value.communications[targetKey].find(
            key => key.parameters.find(param => param.label === "DEFAULT" && param.value === true) !== -1
        )?.value ?? ""
    );
}

function transformRawContact(contact, addressBookId) {
    return {
        uid: contact.uid,
        name: contact.value.identification.formatedName.value,
        email: extractDefaultCommunication(contact, "emails"),
        company: contact.value.organizational?.org?.company,
        kind: contact.value.kind,
        members: contact.value.organizational?.member,
        urn: `${contact.uid}@${addressBookId}`
    };
}
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";

.mail-composer-recipient-modal {
    .modal-header {
        z-index: 1;
        @include until-lg {
            .bm-navbar-title {
                display: none;
            }
        }
        .address-book-mobile-dropdown {
            height: 100%;
            min-width: 0;
            flex: 1;
        }
    }

    .modal-body {
        padding: 0 !important;
        background-color: $backdrop;
        display: flex;
        flex-direction: column;

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
            min-width: 15%;
            max-width: 25%;
        }
    }
    hr {
        margin: 0;
    }

    .bm-form-input.mobile-search-input {
        margin-left: $sp-5;
        margin-right: $sp-3;
        flex: 1;
    }
    .bm-form-input.desktop-search-input {
        background-color: $surface;
    }
}
</style>
