<template>
    <bm-modal id="recipient-picker" :title="$t('recipientPicker.title')">
        <div id="picked-recipient"></div>

        <div id="content">
            <address-book-list :addressbooks="addressBooks" :user-id="userId" />

            <div id="right-panel"></div>
        </div>
    </bm-modal>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmModal } from "@bluemind/ui-components";
import AddressBookList from "./AddressBookList";

export default {
    name: "MailComposerRecipientModal",
    components: { AddressBookList, BmModal },
    data() {
        return {
            addressBooks: [],
            userId: undefined
        };
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
        }
    }
};
</script>

<style lang="scss">
#recipient-picker {
    .modal-body {
        padding: 0;
    }
}
</style>
