<script setup>
import { computed, ref, watch, watchEffect } from "vue";
import { Contact } from "@bluemind/business-components";
import { DirEntryAdaptor } from "@bluemind/contact";
import { Verb } from "@bluemind/core.container.api";
import i18n from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
import { BmIconButton, BmTable } from "@bluemind/ui-components";
import { delegates, fetchAcls, removeDelegate } from "./delegation";

const items = ref([]);
const fields = [
    { key: "contact", label: i18n.t("preferences.account.delegates.delegate") },
    { key: "edit", label: "", class: "edit-cell" }
];
const perPage = 5;
const currentPage = ref(1);

const contactCache = {};
const getContact = async uid => {
    let contact = contactCache[uid];
    if (!contact) {
        contact = DirEntryAdaptor.toContact({ value: await inject("DirectoryPersistence").findByEntryUid(uid) });
        contactCache[uid] = contact;
    }
    return contact;
};

watchEffect(async () => {
    const promises = Object.keys(delegates.value).map(async uid => ({ uid, contact: await getContact(uid) }));
    items.value = await Promise.all(promises);
});

const remove = async userUid => {
    await removeDelegate(userUid);
    fetchAcls();
};
</script>

<template>
    <bm-table
        :items="items"
        :fields="fields"
        :per-page="perPage"
        :current-page="currentPage"
        class="pref-delegates-table"
    >
        <template #cell(contact)="cell">
            <contact :contact="cell.value" transparent bold enable-card />
        </template>
        <template #cell(edit)="cell">
            <bm-icon-button variant="compact" icon="pencil" @click="$emit('edit', cell.item.uid)" />
            <bm-icon-button variant="compact" icon="trash" @click="remove(cell.item.uid)" />
        </template>
    </bm-table>
</template>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables";

.pref-delegates-table {
    &.b-table {
        max-width: base-px-to-rem(900);
        table-layout: fixed;
        td > * {
            height: 100%;
        }
    }
    .edit-cell {
        display: flex;
        justify-content: end;
        gap: $sp-5;
    }
}
</style>
