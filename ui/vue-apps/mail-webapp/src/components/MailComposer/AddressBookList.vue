<template>
    <bm-list-group class="address-book-list">
        <bm-list-group-item
            v-for="addressBook in addressBooksList"
            :key="addressBook.uid"
            data-browse
            button
            role="listitem"
            :class="{ active: selected === addressBook.uid }"
            @click="selected = addressBook.uid"
        >
            <address-book-label-icon :address-book="addressBook" :user-id="userId" />
        </bm-list-group-item>
    </bm-list-group>
</template>

<script>
import { isPersonalAddressBook, isDirectoryAddressBook, isCollectAddressBook } from "@bluemind/contact";
import { BmListGroup, BmListGroupItem, BrowsableContainer } from "@bluemind/ui-components";
import AddressBookLabelIcon from "./AddressBookLabelIcon";

export default {
    name: "AddressBookList",
    components: { BmListGroup, BmListGroupItem, AddressBookLabelIcon },
    mixins: [BrowsableContainer],
    props: {
        userId: { type: String, required: true },
        addressbooks: { type: Array, required: true }
    },
    data() {
        return {
            selected: undefined,
            vertical: true
        };
    },
    computed: {
        addressBooksList() {
            return sortAddressBooks(this.addressbooks, this.userId);
        }
    }
};

function sortAddressBooks(addressBooks, userId) {
    return addressBooks.sort((a, b) => {
        if (!isShared(a, userId) != !isShared(b, userId)) return !isShared(a, userId) ? -1 : 1;

        if (isDirectoryAddressBook(a.uid, a.domainUid) != isDirectoryAddressBook(b.uid, b.domainUid))
            return isDirectoryAddressBook(a.uid, a.domainUid) ? -1 : 1;

        if (isPersonalAddressBook(a.uid, a.owner) != isPersonalAddressBook(b.uid, b.owner))
            return isPersonalAddressBook(a.uid, a.owner) ? -1 : 1;

        if (isCollectAddressBook(a.uid, a.owner) != isCollectAddressBook(b.uid, b.owner))
            return isCollectAddressBook(a.uid, a.owner) ? -1 : 1;

        return a.name.localeCompare(b.name);
    });
}

function isShared(addressBook, userId) {
    return !isDirectoryAddressBook(addressBook.uid, addressBook.domainUid) && userId !== addressBook.owner;
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
@import "~@bluemind/ui-components/src/css/mixins/responsiveness";

$padding-y: $sp-3 + $sp-2;

.address-book-list {
    .list-group-item {
        padding: $padding-y 0 $padding-y $sp-6;
        gap: $sp-4;
        border-color: $neutral-fg-lo3 !important;
        height: $tree-node-height-tactile;
        @include from-lg {
            height: $tree-node-height;
        }
        &.active,
        &:active {
            &:hover {
                background-color: $secondary-bg;
            }
        }
    }
}
</style>
