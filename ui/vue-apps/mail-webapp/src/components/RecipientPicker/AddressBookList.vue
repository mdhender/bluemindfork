<template>
    <bm-list-group class="address-book-list scroller-y-stable">
        <bm-list-group-item
            v-for="addressBook in addressbooks"
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
            vertical: true,
            tabNavigation: false
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
        }
    },
    created() {
        this.selected = this.addressbooks[1]?.uid;
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";

$padding-y: $sp-3 + $sp-2;

.address-book-list {
    background-color: $surface;
    border-right: 1px solid $neutral-fg-lo2;
    .list-group-item {
        flex: none;
        padding-top: $padding-y;
        padding-right: $sp-3 + $sp-2 !important;
        padding-bottom: $padding-y;
        padding-left: $sp-6;
        gap: $sp-4;
        border: none !important;
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
    .address-book-label-icon {
        max-width: 100%;
        &,
        > div {
            overflow: hidden;
            text-overflow: ellipsis;
        }
    }
}
</style>
