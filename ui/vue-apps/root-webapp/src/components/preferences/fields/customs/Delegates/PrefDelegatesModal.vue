<template>
    <bm-modal
        ref="delegatesModal"
        dialog-class="pref-delegates-modal"
        :visible="visible"
        :title="!delegate ? $t('preferences.account.delegates.create') : $t('preferences.account.delegates.edit')"
        variant="advanced"
        scrollable
        size="md"
        height="lg"
        @hidden="$emit('update:visible', false)"
        @show="init"
    >
        <contact-input
            class="d-flex align-items-center"
            select-only
            variant="underline"
            :contacts.sync="selectedContacts"
            :max-contacts="1"
            :autocomplete-results="autocompleteResults"
            :placeholder="$t('common.user')"
            :disabled="!!delegate"
            @search="onSearch"
        >
            <bm-label-icon class="bold" icon="user">
                {{ $t("preferences.account.delegates.delegate") }}
            </bm-label-icon>
        </contact-input>
        <bm-form-radio-group v-model="delegationRight" class="py-4">
            <bm-form-radio :value="Verb.SendOnBehalf">
                {{ $t("preferences.account.delegates.edit.send_on_behalf") }}
            </bm-form-radio>
            <!-- TODO: uncomment once implemented
            <bm-form-radio :value="Verb.SendAs">
                {{ $t("preferences.account.delegates.edit.send_as") }}<bm-icon class="pl-4" icon="user-outline" />
            </bm-form-radio> -->
        </bm-form-radio-group>
        <!-- Calendars -->
        <div class="pt-2 pb-4">
            <div class="d-flex align-items-center">
                <bm-app-icon :icon-app="calendarApp.icon" /><span class="app-label font-weight-bold pl-3">
                    {{ $t("common.application.calendar") }}
                </span>
            </div>
            <bm-form-select
                v-model="calendarRight"
                :auto-min-width="false"
                class="my-4"
                :options="rights(Container.CALENDAR)"
                @input="
                    calendarRightSufficientForCopyImipOption ? undefined : (copyImipToDelegate = false);
                    containerRightsChanged = true;
                "
            />
            <bm-form-checkbox
                v-model="copyImipToDelegate"
                :disabled="!calendarRightSufficientForCopyImipOption"
                @change="copyImipToDelegateChanged = true"
            >
                {{ $t("preferences.account.delegates.calendar.invitations") }}
                <bm-icon class="pl-4" icon="open-envelope" />
            </bm-form-checkbox>
            <!--  TODO: uncomment once implemented
            <bm-form-checkbox>{{ $t("preferences.account.delegates.calendar.private") }}</bm-form-checkbox> -->
        </div>
        <!-- TodoLists -->
        <div class="pt-2 pb-4">
            <div class="d-flex align-items-center">
                <bm-app-icon :icon-app="todoListApp.icon" /><span class="app-label font-weight-bold pl-3">
                    {{ $t("common.application.tasks") }}
                </span>
            </div>
            <bm-form-select
                v-model="todoListRight"
                :auto-min-width="false"
                class="my-4"
                :options="rights(Container.TODO_LIST)"
                @input="containerRightsChanged = true"
            />
        </div>
        <!-- Mailboxes -->
        <div class="pt-2 pb-4">
            <div class="d-flex align-items-center">
                <bm-app-icon :icon-app="messageApp.icon" /><span class="app-label font-weight-bold pl-3">
                    {{ $t("common.application.webmail") }}
                </span>
            </div>
            <bm-form-select
                v-model="messageRight"
                :auto-min-width="false"
                class="my-4"
                :options="rights(Container.MAILBOX)"
                @input="containerRightsChanged = true"
            />
        </div>
        <!-- Contacts -->
        <div class="pt-2 pb-4">
            <div class="d-flex align-items-center">
                <bm-app-icon :icon-app="contactsApp.icon" /><span class="app-label font-weight-bold pl-3">
                    {{ $t("common.application.contacts") }}
                </span>
            </div>
            <bm-form-select
                v-model="contactsRight"
                :auto-min-width="false"
                class="my-4"
                :options="rights(Container.CONTACTS)"
                @input="containerRightsChanged = true"
            />
        </div>
        <!-- Footer -->
        <template #modal-footer>
            <!--  TODO: uncomment once implemented
             <bm-form-checkbox class="d-flex flex-fill">
                {{ $t("preferences.account.delegates.inform") }}
            </bm-form-checkbox> -->
            <bm-button variant="text" @click="$refs.delegatesModal.hide()">
                {{ $t("common.cancel") }}
            </bm-button>
            <bm-button variant="fill-accent" :disabled="!selectedDelegate || !hasChanged" @click="save">
                {{ !delegate ? $t("common.create") : $t("common.edit") }}
            </bm-button>
        </template>
    </bm-modal>
</template>

<script>
import { computed } from "vue";
import { SUCCESS } from "@bluemind/alert.store";
import { ContactInput } from "@bluemind/business-components";
import { DirEntryAdaptor } from "@bluemind/contact";
import { Verb } from "@bluemind/core.container.api";
import { BaseDirEntry } from "@bluemind/directory.api";
import { mapExtensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";
import {
    BmButton,
    BmFormCheckbox,
    BmFormRadio,
    BmFormRadioGroup,
    BmFormSelect,
    BmIcon,
    BmLabelIcon,
    BmModal
} from "@bluemind/ui-components";
import BmAppIcon from "../../../../BmAppIcon";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";
import {
    acls,
    aclToRight,
    addDelegateToCopyImipMailboxRule,
    Container,
    delegates,
    delegations,
    fetchAcls,
    hasCopyImipMailboxRuleAction,
    removeDelegateFromCopyImipMailboxRule,
    Right,
    rightToAcl,
    setCalendarAcl,
    setContactsAcl,
    setMailboxAcl,
    setTodoListAcl
} from "./delegation";

const apps = mapExtensions("net.bluemind.webapp", ["application"]).application;
const findAppFn = bundle => apps?.find(({ $bundle }) => $bundle === bundle);
const calendarApp = findAppFn("net.bluemind.webmodules.calendar");
const todoListApp = findAppFn("net.bluemind.webmodules.todolist");
const messageApp = findAppFn("net.bluemind.webapp.mail.js");
const contactsApp = findAppFn("net.bluemind.webmodules.contact");

export default {
    name: "PrefDelegatesModal",
    components: {
        BmButton,
        BmFormCheckbox,
        BmAppIcon,
        BmFormRadio,
        BmFormRadioGroup,
        BmFormSelect,
        BmIcon,
        BmLabelIcon,
        BmModal,
        ContactInput
    },
    props: {
        delegate: { type: String, default: undefined },
        visible: { type: Boolean, default: false }
    },
    setup() {
        return {
            acls,
            addDelegateToCopyImipMailboxRule,
            delegates,
            delegations,
            fetchAcls,
            hasCopyImipMailboxRuleAction,
            removeDelegateFromCopyImipMailboxRule,
            setCalendarAcl,
            setContactsAcl,
            setMailboxAcl,
            setTodoListAcl
        };
    },
    data() {
        return {
            autocompleteResults: [],
            calendarApp,
            calendarRight: undefined,
            contactsApp,
            contactsRight: undefined,
            Container,
            containerRightsChanged: false,
            copyImipToDelegate: undefined,
            copyImipToDelegateChanged: false,
            delegationRight: undefined,
            initialDelegationRight: undefined,
            messageApp,
            messageRight: undefined,
            selectedContacts: [],
            todoListApp,
            todoListRight: undefined,
            Verb
        };
    },
    computed: {
        hasChanged() {
            return (
                this.containerRightsChanged ||
                this.selectedDelegate !== this.delegate ||
                this.delegationRight !== this.initialDelegationRight ||
                this.copyImipToDelegateChanged
            );
        },
        selectedDelegate() {
            return this.selectedContacts[0]?.uid;
        },
        calendarRightSufficientForCopyImipOption() {
            return this.calendarRight.level >= Right.CAN_EDIT.level;
        }
    },
    watch: {
        delegate: {
            handler: async function () {
                this.selectedContacts = await this.fetchInitialContacts();
            },
            immediate: true
        },
        selectedDelegate: {
            handler: async function (value) {
                this.initialDelegationRight =
                    this.delegations?.find(({ subject }) => subject === value)?.verb || Verb.SendOnBehalf;
                this.delegationRight = this.initialDelegationRight;

                const isNew = !this.delegate;

                this.calendarRight = aclToRight(this.acls.calendar.acl, value, Right.CAN_EDIT, isNew);
                this.todoListRight = aclToRight(this.acls.todoList.acl, value, Right.CAN_EDIT, isNew);
                this.messageRight = aclToRight(this.acls.mailbox.acl, value, Right.HAS_NO_RIGHTS, isNew);
                this.contactsRight = aclToRight(this.acls.addressBook.acl, value, Right.HAS_NO_RIGHTS, isNew);

                this.copyImipToDelegate = await this.hasCopyImipMailboxRuleAction(value);
            },
            immediate: true
        }
    },
    methods: {
        async fetchInitialContacts() {
            return this.delegate
                ? [
                      DirEntryAdaptor.toContact({
                          value: await inject("DirectoryPersistence").findByEntryUid(this.delegate)
                      })
                  ]
                : [];
        },
        init() {
            this.containerRightsChanged = false;
            this.copyImipToDelegateChanged = false;
            if (!this.delegate) {
                this.selectedContacts = [];
            }
        },
        async onSearch(pattern) {
            if (!pattern) {
                this.autocompleteResults = [];
                return;
            }
            const dirEntries = await inject("DirectoryPersistence").search({
                nameOrEmailFilter: pattern,
                kindsFilter: [BaseDirEntry.Kind.USER, BaseDirEntry.Kind.GROUP],
                size: 10
            });
            const userUid = inject("UserSession").userId;
            const excludedUsers = [userUid, ...Object.keys(this.delegates)];
            this.autocompleteResults = dirEntries.values
                .filter(({ uid }) => !excludedUsers.includes(uid))
                .map(DirEntryAdaptor.toContact);
        },
        async save() {
            const promises = [];

            if (this.selectedDelegate !== this.delegate || this.containerRightsChanged) {
                const calendarAcl = rightToAcl(this.calendarRight, this.selectedDelegate);
                promises.push(setCalendarAcl(calendarAcl, this.selectedDelegate));

                const todoListAcl = rightToAcl(this.todoListRight, this.selectedDelegate);
                promises.push(setTodoListAcl(todoListAcl, this.selectedDelegate));

                const delegationAc = { subject: this.selectedDelegate, verb: this.delegationRight };
                const mailboxAcl = rightToAcl(this.messageRight, this.selectedDelegate).concat([delegationAc]);
                promises.push(setMailboxAcl(mailboxAcl, this.selectedDelegate));

                const contactsAcl = rightToAcl(this.contactsRight, this.selectedDelegate);
                promises.push(setContactsAcl(contactsAcl, this.selectedDelegate));
            }

            if (this.copyImipToDelegateChanged) {
                promises.push(
                    this.copyImipToDelegate
                        ? this.addDelegateToCopyImipMailboxRule({
                              uid: this.selectedDelegate,
                              address: this.selectedContacts[0].address
                          })
                        : this.removeDelegateFromCopyImipMailboxRule(this.selectedDelegate)
                );
            }

            await Promise.all(promises);
            this.$refs.delegatesModal.hide();
            await fetchAcls();
            this.$store.dispatch(`alert/${SUCCESS}`, SAVE_ALERT);
        },
        rights(container) {
            return [
                { value: Right.HAS_NO_RIGHTS, text: Right.HAS_NO_RIGHTS.text(container) },
                { value: Right.CAN_READ, text: Right.CAN_READ.text(container) },
                { value: Right.CAN_EDIT, text: Right.CAN_EDIT.text(container) },
                { value: Right.CAN_MANAGE_SHARES, text: Right.CAN_MANAGE_SHARES.text(container) }
            ];
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables";

.pref-delegates-modal {
    .modal-content {
        .delete-autocomplete {
            display: none;
        }
        .bm-form-radio,
        .bm-form-checkbox {
            line-height: $line-height-lg;
        }
        .app-label {
            color: $neutral-fg-hi1;
        }
        .modal-body {
            padding-top: $sp-5;
        }
        .contact-input,
        .bm-form-select {
            width: base-px-to-rem(420);
            max-width: 100%;
        }
        .contact-input {
            .bm-label-icon {
                flex: none;
            }
            .contact-and-input {
                max-width: 100%;
            }
        }
    }
}
</style>
