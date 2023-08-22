<template>
    <bm-modal
        ref="delegatesModal"
        dialog-class="pref-delegates-modal"
        header-class="d-flex align-items-center py-6"
        centered
        :visible="visible"
        :title="!delegate ? $t('preferences.account.delegates.create') : $t('preferences.account.delegates.edit')"
        @hidden="$emit('update:visible', false)"
        @show="changed = false"
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
            <bm-label-icon class="font-weight-bold" icon="user">
                {{ $t("preferences.account.delegates.delegate") }}
            </bm-label-icon>
        </contact-input>
        <bm-form-radio-group v-model="delegationRight" class="py-4">
            <bm-form-radio :value="Verb.SendOnBehalf">
                {{ $t("preferences.account.delegates.edit.send_on_behalf") }}
            </bm-form-radio>
            <bm-form-radio :value="Verb.SendAs">
                {{ $t("preferences.account.delegates.edit.send_as") }}<bm-icon class="pl-4" icon="user-outline" />
            </bm-form-radio>
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
                class="w-100 py-4"
                :options="rights"
                placeholder="Aucun Droit"
                @input="changed = true"
            />
            <bm-form-checkbox>
                {{ $t("preferences.account.delegates.calendar.invitations") }}
                <bm-icon class="pl-4" icon="open-envelope" />
            </bm-form-checkbox>
            <bm-form-checkbox>{{ $t("preferences.account.delegates.calendar.private") }}</bm-form-checkbox>
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
                class="w-100 py-4"
                :options="rights"
                placeholder="Aucun Droit"
                @input="changed = true"
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
                class="w-100 py-4"
                :options="rights"
                placeholder="Aucun Droit"
                @input="changed = true"
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
                class="w-100 py-4"
                :options="rights"
                placeholder="Aucun Droit"
                @input="changed = true"
            />
        </div>
        <!-- Footer -->
        <template #modal-footer>
            <div class="d-flex flex-fill align-items-center m-0">
                <bm-form-checkbox class="d-flex flex-fill">
                    {{ $t("preferences.account.delegates.inform") }}
                </bm-form-checkbox>
                <bm-button class="mr-6" variant="text" @click="$refs.delegatesModal.hide()">
                    {{ $t("common.cancel") }}
                </bm-button>
                <bm-button variant="fill-accent" :disabled="!selectedDelegate || !hasChanged" @click="save">
                    {{ !delegate ? $t("common.create") : $t("common.edit") }}
                </bm-button>
            </div>
        </template>
    </bm-modal>
</template>

<script>
import without from "lodash.without";
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

import {
    acls,
    delegates,
    delegations,
    fetchAcls,
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
import { computed } from "vue";

const Right = {
    NONE: { verbs: [] },
    REVIEWER: { verbs: [Verb.Read] },
    AUTHOR: { verbs: [Verb.Read, Verb.Write] },
    EDITOR: { verbs: [Verb.Read, Verb.Write, Verb.Manage] }
};

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
            delegates,
            delegations,
            fetchAcls,
            setCalendarAcl,
            setContactsAcl,
            setMailboxAcl,
            setTodoListAcl
        };
    },
    data() {
        return {
            autocompleteResults: [],
            changed: false,
            selectedContacts: [],
            Verb,
            initialDelegationRight: undefined,
            delegationRight: undefined,
            calendarApp,
            todoListApp,
            messageApp,
            contactsApp,
            calendarRight: undefined,
            todoListRight: undefined,
            messageRight: undefined,
            contactsRight: undefined,
            rights: [
                { value: Right.NONE, text: this.$t("preferences.account.delegates.right.none") },
                {
                    value: Right.REVIEWER,
                    text: this.$t("preferences.account.delegates.right.reviewer.with_description", {
                        reviewer: this.$t("preferences.account.delegates.right.reviewer")
                    })
                },
                {
                    value: Right.AUTHOR,
                    text: this.$t("preferences.account.delegates.right.author.with_description", {
                        author: this.$t("preferences.account.delegates.right.author")
                    })
                },
                {
                    value: Right.EDITOR,
                    text: this.$t("preferences.account.delegates.right.editor.with_description", {
                        editor: this.$t("preferences.account.delegates.right.editor")
                    })
                }
            ]
        };
    },
    computed: {
        hasChanged() {
            return (
                this.changed ||
                this.selectedDelegate !== this.delegate ||
                this.delegationRight !== this.initialDelegationRight
            );
        },
        selectedDelegate() {
            return this.selectedContacts[0]?.uid;
        }
    },
    watch: {
        delegate: {
            handler: async function () {
                this.selectedContacts = await this.fetchInitialContacts();
            },
            immediate: true
        },
        selectedDelegate(value) {
            this.initialDelegationRight =
                this.delegations?.find(({ subject }) => subject === value)?.verb || Verb.SendOnBehalf;
            this.delegationRight = this.initialDelegationRight;

            this.calendarRight = aclToRight(this.acls.calendar.acl, value, Right.AUTHOR);
            this.todoListRight = aclToRight(this.acls.todoList.acl, value, Right.AUTHOR);
            this.messageRight = aclToRight(this.acls.mailbox.acl, value, Right.NONE);
            this.contactsRight = aclToRight(this.acls.addressBook.acl, value, Right.NONE);
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
            const delegationAc = { subject: this.selectedDelegate, verb: this.delegationRight };

            const calendarAcl = rightToAcl(this.calendarRight, this.selectedDelegate).concat([delegationAc]);
            promises.push(setCalendarAcl(calendarAcl, this.selectedDelegate));

            const todoListAcl = rightToAcl(this.todoListRight, this.selectedDelegate).concat([delegationAc]);
            promises.push(setTodoListAcl(todoListAcl, this.selectedDelegate));

            const mailboxAcl = rightToAcl(this.messageRight, this.selectedDelegate).concat([delegationAc]);
            promises.push(setMailboxAcl(mailboxAcl, this.selectedDelegate));

            const contactsAcl = rightToAcl(this.contactsRight, this.selectedDelegate).concat([delegationAc]);
            promises.push(setContactsAcl(contactsAcl, this.selectedDelegate));

            await Promise.all(promises);
            fetchAcls();
            this.$refs.delegatesModal.hide();
        }
    }
};

function rightToAcl(right, subject) {
    return right.verbs.map(verb => ({ verb, subject }));
}

const orderedRights = [Right.EDITOR, Right.AUTHOR, Right.REVIEWER, Right.NONE];

function aclToRight(acl, delegate, defaultRight) {
    if (!acl || !delegate) {
        return defaultRight;
    }

    const filteredAclVerbs = acl
        .filter(({ subject, verb }) => subject === delegate && [Verb.Read, Verb.Write, Verb.Manage].includes(verb))
        .map(({ verb }) => verb);

    for (const right of orderedRights) {
        const rightVerbs = right.verbs;
        if (without(rightVerbs, ...filteredAclVerbs).length === 0) {
            return right;
        }
    }
}
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables";

.pref-delegates-modal {
    .modal-header {
        background-color: $neutral-bg-lo1;
    }
    .modal-content {
        max-height: 80vh;
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
    }
    .modal-footer {
        box-shadow: $box-shadow-sm;
    }
}
</style>
