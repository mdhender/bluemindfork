<template>
    <bm-modal id="recipient-picker" :title="$t('recipientPicker.title')" size="custom">
        <selected-contacts :contacts.sync="selectedContacts" />
        <hr />
        <div class="recipient-modal-body d-flex">
            <address-book-list
                :addressbooks="addressBooks"
                :user-id="userId"
                :selected-addressbook="selectedAddressbookId"
                @selected="selectedAddressbookId = $event"
            />
            <div class="flex-fill">
                <contact-list
                    class="h-100"
                    :contacts="contacts"
                    :loading="loading"
                    :addressbook="selectedAddressbook"
                    :user-id="userId"
                    :selected.sync="selectedContacts"
                />
            </div>
        </div>
    </bm-modal>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmModal } from "@bluemind/ui-components";
import AddressBookList from "./AddressBookList";
import ContactList from "./ContactList";
import SelectedContacts from "./SelectedContacts";

export default {
    name: "MailComposerRecipientModal",
    components: { BmModal, AddressBookList, ContactList, SelectedContacts },
    data() {
        return {
            addressBooks: [],
            contacts: [],
            loading: false,
            selectedContacts: [],
            selectedAddressbookId: undefined,
            userId: undefined
        };
    },
    computed: {
        selectedAddressbook() {
            return this.addressBooks.find(a => a.uid === this.selectedAddressbookId) || {};
        }
    },
    watch: {
        async selectedAddressbookId(value) {
            this.loading = true;
            try {
                if (!value) return [];
                const ids = await inject("AddressBookPersistence", value).sortedIds();
                this.contacts = (await inject("AddressBookPersistence", value).multipleGetById(ids)).map(contact => ({
                    uid: contact.uid,
                    name: contact.value.identification.formatedName.value,
                    email: extractDefaultCommunication(contact, "emails"),
                    tel: extractDefaultCommunication(contact, "tels")
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
        async subscribedContainerUids() {
            return (await inject("OwnerSubscriptionsPersistence").list())
                .filter(sub => sub.value.containerType === "addressbook")
                .map(sub => sub.value.containerUid);
        },
        addressbookById(addressbookId) {
            return this.addressBooks.find(a => a.uid === addressbookId) || {};
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/variables";

#recipient-picker {
    height: 80vh;
    .modal-custom {
        width: 79.6%;
        max-width: 79.6%;
    }
    .modal-header {
        background-color: $neutral-bg-lo1;
        padding-top: base-px-to-rem(16);
        padding-bottom: base-px-to-rem(13);
        padding-left: $sp-7;
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
}
</style>
