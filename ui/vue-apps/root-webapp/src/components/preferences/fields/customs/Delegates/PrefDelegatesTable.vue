<script setup>
import { computed, ref, watch, watchEffect } from "vue";
import { SUCCESS } from "@bluemind/alert.store";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";
import { Contact } from "@bluemind/business-components";
import { DirEntryAdaptor } from "@bluemind/contact";
import { Verb } from "@bluemind/core.container.api";
import i18nInstance from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
import store from "@bluemind/store";
import { matchPattern } from "@bluemind/string";
import { BmFormInput, BmIcon, BmIconButton, BmModal, BmPagination, BmTable } from "@bluemind/ui-components";
import { useDelegation } from "./delegation";

const {
    canSeePrivateEvents,
    Container,
    delegates,
    delegationTypes,
    fetchAcls,
    getCalendarRight,
    getContactsRight,
    getMessageRight,
    getTodoListRight,
    hasCopyImipMailboxRuleAction,
    hasIncoherentCopyImipOption,
    removeDelegate
} = useDelegation();

const perPage = ref(5);
const currentPage = ref(1);
const items = ref([]);
const pattern = ref("");
const fields = [
    {
        key: "contact",
        label: i18nInstance.t("preferences.account.delegates.delegate"),
        class: "contact-cell",
        sortable: true
    },
    {
        key: "calendarRight",
        label: i18nInstance.t("common.application.calendar"),
        class: "right-cell calendar-right-cell"
    },
    { key: "todoListRight", label: i18nInstance.t("common.application.tasks"), class: "right-cell" },
    { key: "messageRight", label: i18nInstance.t("common.application.webmail"), class: "right-cell" },
    {
        key: "contactsRight",
        label: i18nInstance.t("common.application.contacts"),
        class: "right-cell"
    },
    { key: "edit", label: "", class: "edit-cell" }
];

const filteredItems = computed(() =>
    items.value.filter(({ contact: { dn, address } }) => matchPattern(pattern.value, [dn, address]))
);

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
    const promises = Object.keys(delegates.value).map(async uid => ({
        uid,
        contact: await getContact(uid),
        calendarRight: getCalendarRight(uid),
        todoListRight: getTodoListRight(uid),
        messageRight: getMessageRight(uid),
        contactsRight: getContactsRight(uid),
        hasCopyImip: hasCopyImipMailboxRuleAction(uid),
        isSendAs: delegationTypes.value[uid] === Verb.SendAs,
        canSeePrivateEvents: canSeePrivateEvents(uid),
        incoherentCopyImip: hasIncoherentCopyImipOption(uid)
    }));

    items.value = await Promise.all(promises);
});

const confirmDeleteModal = ref();
const deletedDelegate = ref();
const remove = contact => {
    deletedDelegate.value = contact?.dn || contact?.address;
    confirmDeleteModal.value.onOk = async () => {
        await removeDelegate(contact.uid);
        await fetchAcls();
        store.dispatch(`alert/${SUCCESS}`, SAVE_ALERT);
        confirmDeleteModal.value?.hide();
    };
    confirmDeleteModal.value.show();
};
</script>

<template>
    <div class="pref-delegates-table">
        <bm-form-input
            v-model="pattern"
            class="pref-filter mt-2 mb-5"
            :placeholder="$t('common.filter')"
            icon="funnel"
            resettable
            left-icon
            :aria-label="$t('common.filter')"
            autocomplete="off"
            @reset="pattern = ''"
        />
        <bm-table
            :items="filteredItems"
            :fields="fields"
            :per-page="perPage"
            :current-page="currentPage"
            sort-by="contact"
        >
            <template #cell(contact)="cell">
                <div class="d-flex align-items-center">
                    <contact :contact="cell.value" class="text-truncate" transparent bold enable-card />
                    <bm-icon
                        v-if="cell.item.isSendAs"
                        icon="user-outline"
                        class="ml-4"
                        :title="$t('preferences.account.delegates.edit.send_as')"
                    />
                </div>
            </template>
            <template #cell(calendarRight)="cell">
                <div class="d-flex align-items-center pr-5">
                    <div class="text-truncate">{{ cell.value.shortText(Container.CALENDAR) }}</div>
                    <bm-icon
                        v-if="cell.item.hasCopyImip"
                        icon="mail-title"
                        class="ml-4"
                        :title="$t('preferences.account.delegates.calendar.invitations')"
                    />
                    <bm-icon
                        v-if="cell.item.canSeePrivateEvents"
                        icon="lock-fill"
                        class="ml-4"
                        :title="$t('preferences.account.delegates.calendar.private')"
                    />
                    <bm-icon
                        v-if="cell.item.incoherentCopyImip"
                        class="ml-4 text-warning"
                        icon="exclamation-circle"
                        :title="$t('preferences.account.delegates.calendar.invitations.incoherent.in_table')"
                    />
                </div>
            </template>
            <template #cell(todoListRight)="cell">
                {{ cell.value.shortText() }}
            </template>
            <template #cell(messageRight)="cell">
                {{ cell.value.shortText() }}
            </template>
            <template #cell(contactsRight)="cell">
                {{ cell.value.shortText() }}
            </template>
            <template #cell(edit)="cell">
                <div>
                    <bm-icon-button variant="compact" icon="pencil" @click="$emit('edit', cell.item.uid)" />
                    <bm-icon-button variant="compact" icon="trash" @click="remove(cell.item.contact)" />
                </div>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="filteredItems.length" :per-page="perPage" />
        <bm-modal
            ref="confirmDeleteModal"
            :title="i18nInstance.t('preferences.account.delegates.delete')"
            :ok-title="i18nInstance.t('common.delete')"
            :cancel-title="i18nInstance.t('common.cancel')"
            centered
            auto-focus-button="ok"
        >
            <i18n path="preferences.account.delegates.delete.confirm">
                <template #user>
                    <span class="bold">{{ deletedDelegate }}</span>
                </template>
            </i18n>
        </bm-modal>
    </div>
</template>

<style lang="scss">
@use "sass:math";
@import "@bluemind/ui-components/src/css/utils/text";
@import "@bluemind/ui-components/src/css/utils/variables";

.pref-delegates-table {
    .b-table {
        max-width: base-px-to-rem(900);
        table-layout: fixed;
    }
    .contact-cell {
        overflow: hidden;
    }
    td.contact-cell {
        > .contact {
            max-width: 100%;
        }
    }
    .right-cell {
        white-space: nowrap !important;
        @include text-overflow;
        width: math.div(136, 900) * 100%;
        &.calendar-right-cell {
            width: math.div(170, 900) * 100%;
        }
    }
    .edit-cell {
        width: base-px-to-rem(80);
        > div {
            display: flex;
            gap: $sp-5;
        }
    }
}
</style>
