<script setup>
import { computed, defineProps, defineEmits } from "vue";
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
        return selectedAddressBookId.uid;
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
    <bm-form-select v-model="selectedAddressBookId" class="w-100" :options="books" :auto-min-width="false">
        <template #selected>
            <address-book-label-icon :address-book="selectedAddressBook" :user-id="userId" />
        </template>
        <template #item="{ item }">
            <address-book-label-icon :address-book="item" :user-id="userId" />
        </template>
    </bm-form-select>
</template>
