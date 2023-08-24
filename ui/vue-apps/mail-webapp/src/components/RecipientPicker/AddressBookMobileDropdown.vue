<script setup>
import { computed, defineProps } from "vue";
import { BmFormSelect } from "@bluemind/ui-components";
import AddressBookLabelIcon from "./AddressBookLabelIcon";

const emit = defineEmits(["selected"]);
const props = defineProps({
    addressBooks: { type: Array, required: true },
    selectedAddressBook: { type: Object, required: true },
    userId: { type: String, required: true }
});

const selectedAddressBookId = computed({
    get() {
        return props.selectedAddressBook.uid;
    },
    set(value) {
        emit("selected", value);
    }
});

const books = props.addressBooks.map(book => ({
    ...book,
    text: book.name,
    value: book.uid
}));
</script>

<template>
    <bm-form-select
        v-model="selectedAddressBookId"
        class="address-book-mobile-dropdown"
        variant="inline-on-fill-primary"
        :options="books"
        :auto-min-width="false"
    >
        <template #selected>
            <address-book-label-icon :address-book="selectedAddressBook" :user-id="userId" />
        </template>
        <template #item="{ item }">
            <address-book-label-icon :address-book="item" :user-id="userId" />
        </template>
    </bm-form-select>
</template>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/typography";
@import "@bluemind/ui-components/src/css/utils/variables";

.bm-form-select.address-book-mobile-dropdown {
    .btn.dropdown-toggle {
        flex: 1;
        justify-content: flex-start;
        gap: $sp-4 !important;
        height: 100%;
        @include bold-tight;
    }

    .dropdown-item {
        padding-left: 0 !important;
    }

    .bm-label-icon.address-book-label-icon {
        max-width: 100%;
        .bm-icon {
            margin-right: $sp-4 !important;
        }
    }
}
</style>
