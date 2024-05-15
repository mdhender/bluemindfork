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
        <bm-form-radio-group
            v-model="formData.delegationRight.current"
            class="radio-group"
            :disabled="!selectedDelegate"
        >
            <bm-form-radio :value="Verb.SendOnBehalf">
                {{ $t("preferences.account.delegates.edit.send_on_behalf") }}
            </bm-form-radio>
            <bm-form-radio :value="Verb.SendAs" class="my-3">
                {{ $t("preferences.account.delegates.edit.send_as") }}
                <span class="tail-content"><bm-icon class="ml-4" icon="user-outline" /></span>
            </bm-form-radio>
        </bm-form-radio-group>
        <div class="delegation-notice">
            <div class="delegation-notice-label">
                {{ $t("preferences.account.delegates.edit.notice.label") }}
            </div>
            <div class="pt-1 sender-preview">
                <contact :contact="userAsContact" transparent bold-dn avatar-size="md" />
                <span v-if="formData.delegationRight.current === Verb.SendOnBehalf" class="ml-3 sent-by">
                    <i18n path="preferences.account.delegates.edit.notice.send_by">
                        <template #delegate>
                            <span class="bold">{{
                                selectedDelegate
                                    ? selectedContacts[0].dn
                                    : $t("preferences.account.delegates.edit.notice.send_by.placeholder")
                            }}</span>
                        </template>
                    </i18n>
                </span>
            </div>
        </div>
        <!-- Calendars -->
        <div class="pt-2 pb-4">
            <div class="d-flex align-items-center">
                <bm-app-icon :icon-app="calendarApp.icon" /><span class="app-label font-weight-bold pl-3">
                    {{ $t("common.application.calendar") }}
                </span>
            </div>
            <bm-form-select
                v-model="formData.calendarRight.current"
                :disabled="!selectedDelegate"
                :auto-min-width="false"
                class="mt-3 mb-4"
                :options="rights(Container.CALENDAR)"
                @input="onCalendarRightChanged"
            />
            <bm-form-checkbox
                v-model="formData.copyImipToDelegate.current"
                :disabled="copyImipToDelegateDisabled"
                class="my-2 ml-3"
            >
                <span
                    :title="
                        copyImipToDelegateDisabled
                            ? $t('preferences.account.delegates.option_needs_permission', {
                                  right: $t('preferences.account.delegates.right.calendar.can_edit')
                              })
                            : undefined
                    "
                >
                    {{ $t("preferences.account.delegates.calendar.invitations") }}
                    <span class="tail-content ml-2">
                        <bm-icon class="ml-4" icon="mail-title" />
                        <bm-icon
                            v-if="incoherentCopyImipToDelegate"
                            class="ml-3 text-warning"
                            icon="exclamation-circle"
                            :title="
                                $t('preferences.account.delegates.calendar.invitations.incoherent.in_modal', {
                                    right: $t('preferences.account.delegates.right.calendar.can_edit')
                                })
                            "
                        />
                    </span>
                </span>
            </bm-form-checkbox>
            <div v-if="formData.copyImipToDelegate.current" class="ml-3 pt-3 pb-2 text-warning d-flex">
                <bm-icon icon="exclamation-circle-fill" class="mt-2" />
                <span class="ml-4">
                    {{ $t("preferences.account.delegates.calendar.invitations.no_private_event") }}
                    <bm-read-more
                        href="https://doc.bluemind.net/release/5.1/guide_de_l_utilisateur/parametrer_le_compte_utilisateur#2-donner-les-droits-de-gestion-des-invitations"
                    />
                </span>
            </div>
            <bm-form-checkbox
                v-model="formData.seePrivateEvents.current"
                :disabled="!selectedDelegate || !isSeePrivateEventsPossible"
                class="my-4 ml-3"
            >
                <span
                    :title="
                        !isSeePrivateEventsPossible
                            ? $t('preferences.account.delegates.option_needs_permission', {
                                  right: $t('preferences.account.delegates.right.calendar.can_read')
                              })
                            : undefined
                    "
                >
                    {{ $t("preferences.account.delegates.calendar.private") }}
                    <span class="tail-content"><bm-icon class="ml-3" icon="lock-fill" /></span>
                </span>
            </bm-form-checkbox>
        </div>
        <!-- TodoLists -->
        <div class="pt-2 pb-4">
            <div class="d-flex align-items-center">
                <bm-app-icon :icon-app="todoListApp.icon" /><span class="app-label font-weight-bold pl-3">
                    {{ $t("common.application.tasks") }}
                </span>
            </div>
            <bm-form-select
                v-model="formData.todoListRight.current"
                :disabled="!selectedDelegate"
                :auto-min-width="false"
                class="mt-3 mb-4"
                :options="rights(Container.TODO_LIST)"
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
                v-model="formData.messageRight.current"
                :disabled="!selectedDelegate"
                :auto-min-width="false"
                class="mt-3 mb-4"
                :options="rights(Container.MAILBOX)"
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
                v-model="formData.contactsRight.current"
                :disabled="!selectedDelegate"
                :auto-min-width="false"
                class="mt-3 mb-4"
                :options="rights(Container.CONTACTS)"
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
import BmRoles from "@bluemind/roles";
import {
    BmButton,
    BmFormCheckbox,
    BmFormRadio,
    BmFormRadioGroup,
    BmFormSelect,
    BmIcon,
    BmLabelIcon,
    BmModal,
    BmReadMore
} from "@bluemind/ui-components";
import { Contact } from "@bluemind/business-components";
import BmAppIcon from "../../../../BmAppIcon";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";
import { useDelegation } from "./delegation";

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
        BmReadMore,
        Contact,
        ContactInput
    },
    props: {
        delegate: { type: String, default: undefined },
        visible: { type: Boolean, default: false },
        receiveImipOption: { type: Number, required: true }
    },
    setup() {
        const {
            aclToRight,
            addDelegateToCopyImipMailboxRule,
            canSeePrivateEvents,
            Container,
            delegates,
            delegationTypes,
            fetchAcls,
            getCalendarAcl,
            getContactsAcl,
            getMailboxAcl,
            getTodoListAcl,
            hasCopyImipMailboxRuleAction,
            hasIncoherentCopyImipOption,
            removeDelegateFromCopyImipMailboxRule,
            Right,
            setContainerAcl
        } = useDelegation();
        return {
            aclToRight,
            addDelegateToCopyImipMailboxRule,
            canSeePrivateEvents,
            Container,
            delegates,
            delegationTypes,
            fetchAcls,
            getCalendarAcl,
            getContactsAcl,
            getMailboxAcl,
            getTodoListAcl,
            hasCopyImipMailboxRuleAction,
            hasIncoherentCopyImipOption,
            removeDelegateFromCopyImipMailboxRule,
            Right,
            setContainerAcl
        };
    },
    data() {
        return {
            autocompleteResults: [],
            calendarApp,
            contactsApp,
            messageApp,
            selectedContacts: [],
            todoListApp,
            Verb,
            formData: {
                copyImipToDelegate: { current: undefined, initial: undefined },
                calendarRight: { current: undefined, initial: undefined },
                contactsRight: { current: undefined, initial: undefined },
                messageRight: { current: undefined, initial: undefined },
                todoListRight: { current: undefined, initial: undefined },
                delegationRight: { current: undefined, initial: undefined },
                seePrivateEvents: { current: undefined, initial: undefined },
                delegate: { current: undefined, initial: undefined }
            }
        };
    },
    computed: {
        hasChanged() {
            return Object.values(this.formData).some(changed);
        },
        selectedDelegate() {
            return this.selectedContacts[0]?.uid;
        },
        isCopyImipOptionPossible() {
            return (
                inject("UserSession").roles.includes(BmRoles.SELF_CHANGE_MAILBOX_FILTER) &&
                this.formData.calendarRight.current?.level >= this.Right.CAN_EDIT.level
            );
        },
        isSeePrivateEventsPossible() {
            return this.formData.calendarRight.current?.level >= this.Right.CAN_READ.level;
        },
        userAsContact() {
            return {
                dn: this.$store.state.preferences.containers.myMailboxContainer.ownerDisplayname,
                address: inject("UserSession").defaultEmail
            };
        },
        incoherentCopyImipToDelegate() {
            return this.hasIncoherentCopyImipOption(
                this.selectedDelegate,
                this.formData.copyImipToDelegate.current,
                this.formData.calendarRight.current
            );
        },
        copyImipToDelegateDisabled() {
            return !this.incoherentCopyImipToDelegate && (!this.selectedDelegate || !this.isCopyImipOptionPossible);
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
                this.formData.delegationRight.initial = this.delegationTypes[this.delegate] || Verb.SendOnBehalf;
                this.formData.delegationRight.current = this.formData.delegationRight.initial;

                const isNew = !this.delegate;

                this.formData.calendarRight.initial = this.aclToRight(
                    this.getCalendarAcl(),
                    value,
                    this.Right.CAN_EDIT,
                    isNew
                );
                this.formData.calendarRight.current = this.formData.calendarRight.initial;
                this.formData.todoListRight.initial = this.aclToRight(
                    this.getTodoListAcl(),
                    value,
                    this.Right.CAN_EDIT,
                    isNew
                );
                this.formData.todoListRight.current = this.formData.todoListRight.initial;
                this.formData.messageRight.initial = this.aclToRight(
                    this.getMailboxAcl(),
                    value,
                    this.Right.HAS_NO_RIGHTS,
                    isNew
                );
                this.formData.messageRight.current = this.formData.messageRight.initial;
                this.formData.contactsRight.initial = this.aclToRight(
                    this.getContactsAcl(),
                    value,
                    this.Right.HAS_NO_RIGHTS,
                    isNew
                );
                this.formData.contactsRight.current = this.formData.contactsRight.initial;
                this.formData.copyImipToDelegate.initial = this.hasCopyImipMailboxRuleAction(value);
                this.formData.copyImipToDelegate.current = this.formData.copyImipToDelegate.initial;
                this.formData.seePrivateEvents.initial = this.canSeePrivateEvents(value);
                this.formData.seePrivateEvents.current = this.formData.seePrivateEvents.initial;
                this.formData.delegate.initial = this.delegate;
                this.formData.delegate.current = value;
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
                kindsFilter: [BaseDirEntry.Kind.USER],
                size: 10
            });
            const userUid = inject("UserSession").userId;
            const excludedUsers = [userUid, ...Object.keys(this.delegates)];
            this.autocompleteResults = dirEntries.values
                .filter(({ uid }) => !excludedUsers.includes(uid))
                .map(DirEntryAdaptor.toContact);
        },
        async save() {
            await Promise.all([
                this.saveMailboxAcl(),
                this.saveCalendarAcl(),
                this.saveTodoListAcl(),
                this.saveContactsAcl(),
                this.saveCopyImip()
            ]);
            this.$refs.delegatesModal.hide();
            await this.fetchAcls();
            this.$store.dispatch(`alert/${SUCCESS}`, SAVE_ALERT);
        },
        saveMailboxAcl() {
            return changed(this.formData.delegate) ||
                changed(this.formData.messageRight) ||
                changed(this.formData.delegationRight)
                ? this.setContainerAcl(
                      this.Container.MAILBOX,
                      this.selectedDelegate,
                      this.formData.messageRight.current,
                      [this.formData.delegationRight.current]
                  )
                : Promise.resolve();
        },
        saveCalendarAcl() {
            return changed(this.formData.delegate) ||
                changed(this.formData.calendarRight) ||
                changed(this.formData.seePrivateEvents)
                ? this.setContainerAcl(
                      this.Container.CALENDAR,
                      this.selectedDelegate,
                      this.formData.calendarRight.current,
                      this.formData.seePrivateEvents.current ? [Verb.ReadExtended] : []
                  )
                : Promise.resolve();
        },
        saveTodoListAcl() {
            return changed(this.formData.delegate) || changed(this.formData.todoListRight)
                ? this.setContainerAcl(
                      this.Container.TODO_LIST,
                      this.selectedDelegate,
                      this.formData.todoListRight.current
                  )
                : Promise.resolve();
        },
        saveContactsAcl() {
            return changed(this.formData.delegate) || changed(this.formData.contactsRight)
                ? this.setContainerAcl(
                      this.Container.CONTACTS,
                      this.selectedDelegate,
                      this.formData.contactsRight.current
                  )
                : Promise.resolve();
        },
        saveCopyImip() {
            return changed(this.formData.copyImipToDelegate)
                ? this.formData.copyImipToDelegate.current
                    ? this.addDelegateToCopyImipMailboxRule({
                          uid: this.selectedDelegate,
                          receiveImipOption: this.receiveImipOption
                      })
                    : this.removeDelegateFromCopyImipMailboxRule(this.selectedDelegate)
                : Promise.resolve();
        },
        rights(container) {
            return [
                { value: this.Right.HAS_NO_RIGHTS, text: this.Right.HAS_NO_RIGHTS.text(container) },
                { value: this.Right.CAN_READ, text: this.Right.CAN_READ.text(container) },
                { value: this.Right.CAN_EDIT, text: this.Right.CAN_EDIT.text(container) },
                { value: this.Right.CAN_MANAGE_SHARES, text: this.Right.CAN_MANAGE_SHARES.text(container) }
            ];
        },
        onCalendarRightChanged() {
            if (!this.isCopyImipOptionPossible && this.formData.copyImipToDelegate.current) {
                this.formData.copyImipToDelegate.current = false;
            }
            if (!this.isSeePrivateEventsPossible && this.formData.seePrivateEvents.current) {
                this.formData.seePrivateEvents.current = false;
            }
        }
    }
};

function changed({ current, initial }) {
    return current !== initial;
}
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/typography";
@import "@bluemind/ui-components/src/css/utils/variables";

.pref-delegates-modal {
    .modal-content {
        .delete-autocomplete {
            display: none;
        }
        .radio-group {
            padding: $sp-5 0 $sp-3 $sp-4;
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
            .suggestions {
                width: 100%;
                .contact-kind {
                    display: none;
                }
            }
        }
        .delegation-notice {
            margin-top: $sp-2;
            margin-bottom: $sp-6 - $sp-2;
            padding: $sp-4 0 $sp-4 $sp-6;
            background-color: $neutral-bg-lo1;

            .delegation-notice-label {
                color: $neutral-fg-lo1;
                @include caption-bold;
                margin-bottom: $sp-3;
            }

            .sender-preview {
                $offset: base-px-to-rem(37);
                text-indent: -$offset;
                padding-left: $offset;
                padding-right: $sp-2;

                .contact {
                    text-indent: 0;
                    .contact-main-part {
                        margin-left: $sp-5 + $sp-2;
                    }
                }

                .sent-by {
                    position: relative;
                    bottom: base-px-to-rem(10);
                    text-indent: base-px-to-rem(20);
                }
            }
        }
    }
}
</style>
