<template>
    <bm-list-group class="address-book-list scroller-y">
        <bm-list-group-item
            v-for="addressBook in addressBooksList"
            :key="addressBook.uid"
            data-browse
            button
            role="listitem"
            :class="{ active: selected === addressBook.uid }"
            class="text-truncate"
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
        addressbooks: { type: Array, required: true },
        selectedAddressbook: { type: String, default: "" }
    },
    data() {
        return {
            vertical: true
        };
    },
    computed: {
        selected: {
            get() {
                return this.selectedAddressbook;
            },
            set(selection) {
                this.$emit("selected", selection);
            }
        },
        addressBooksList() {
            return sortAddressBooks(this.addressbooks, this.userId);
        }
    },
    created() {
        this.selected = this.addressBooksList[0]?.uid;
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
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";

$padding-y: $sp-3 + $sp-2;

.address-book-list {
    background-color: $surface;
    .list-group-item {
        flex: none;
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
