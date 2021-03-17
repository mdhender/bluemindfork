<template>
    <div class="mail-multiple-selection-actions">
        <mail-component-alert
            v-if="anyMessageReadOnly && !isReadOnlyAlertDismissed"
            icon="info-circle-plain"
            @close="isReadOnlyAlertDismissed = true"
        >
            {{ $t("mail.selection.alert.readonly") }}
        </mail-component-alert>
        <div
            class="h-100 mt-5 d-flex flex-column text-center align-items-center"
            :style="'background: url(' + multipleSelectionIllustration + ') no-repeat center top'"
        >
            <div class="font-weight-bold mt-5 mb-2">
                <h1>{{ selection.length }}</h1>
                <h1>{{ $t("mail.message.selected") }}</h1>
            </div>

            <div v-if="!anyMessageReadOnly" class="bg-white py-2 px-3 actions-button w-75 mt-4">
                <div class="arrow-up" />
                <bm-button v-if="!ALL_SELECTED_MESSAGES_ARE_READ" variant="outline-secondary" @click="markAsRead()">
                    <bm-label-icon icon="read">
                        {{ $tc("mail.actions.mark_as_read", selection.length) }}
                    </bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="!ALL_SELECTED_MESSAGES_ARE_UNREAD"
                    variant="outline-secondary"
                    @click="MARK_MESSAGES_AS_UNREAD(selection.map(key => messages[key]))"
                >
                    <bm-label-icon icon="unread">
                        {{ $tc("mail.actions.mark_as_unread", selection.length) }}
                    </bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="!ALL_SELECTED_MESSAGES_ARE_FLAGGED"
                    variant="outline-secondary"
                    @click="MARK_MESSAGES_AS_FLAGGED(selection.map(key => messages[key]))"
                >
                    <bm-label-icon icon="flag-outline">
                        {{ $tc("mail.actions.mark_flagged", selection.length) }}
                    </bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="!ALL_SELECTED_MESSAGES_ARE_UNFLAGGED"
                    variant="outline-secondary"
                    @click="MARK_MESSAGES_AS_UNFLAGGED(selection.map(key => messages[key]))"
                >
                    <bm-label-icon icon="flag-fill">
                        {{ $tc("mail.actions.mark_unflagged", selection.length) }}
                    </bm-label-icon>
                </bm-button>
                <bm-button
                    variant="outline-secondary"
                    @click.exact="MOVE_MESSAGES_TO_TRASH(selection.map(key => messages[key]))"
                    @click.shift.exact="REMOVE_MESSAGES(selection.map(key => messages[key]))"
                >
                    <bm-label-icon icon="trash">
                        {{ $tc("mail.actions.remove", selection.length) }}
                    </bm-label-icon>
                </bm-button>
            </div>

            <bm-button variant="inline-secondary" class="my-4" @click="removeSelection">
                {{ $t("common.cancel.selection") }}
            </bm-button>

            <hr v-if="!ALL_MESSAGES_ARE_SELECTED" class="w-75 border-dark" />

            <div v-if="!ALL_MESSAGES_ARE_SELECTED" class="mt-3">
                <h3 v-if="!MESSAGE_LIST_IS_SEARCH_MODE" class="d-inline px-3 align-middle">
                    {{ $t("mail.message.select.all.folder") }}
                    <mail-folder-icon
                        :shared="isFolderOfMailshare(currentFolder)"
                        :folder="currentFolder"
                        class="font-weight-bold ml-1"
                    >
                        {{ currentFolder.path }}
                    </mail-folder-icon>
                </h3>
                <h3 v-else class="d-inline px-3 align-middle">
                    {{ $t("mail.message.select.all.search") }}
                    <bm-icon icon="search" /><span class="font-weight-bold">"{{ messageList.search.pattern }}"</span>
                </h3>
                <bm-button @click="SELECT_ALL_MESSAGES(messageList.messageKeys)">
                    {{ $t("common.select.all") }}
                </bm-button>
            </div>
        </div>
    </div>
</template>

<script>
import { BmButton, BmLabelIcon, BmIcon } from "@bluemind/styleguide";
import { ItemUri } from "@bluemind/item-uri";
import { mapActions, mapState, mapGetters, mapMutations } from "vuex";
import MailComponentAlert from "./MailComponentAlert";
import MailFolderIcon from "./MailFolderIcon";
import multipleSelectionIllustration from "../../assets/multiple-selection.png";
import { MailboxType } from "~model/mailbox";
import { RemoveMixin } from "~mixins";

import {
    ALL_MESSAGES_ARE_SELECTED,
    ALL_SELECTED_MESSAGES_ARE_FLAGGED,
    ALL_SELECTED_MESSAGES_ARE_READ,
    ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
    ALL_SELECTED_MESSAGES_ARE_UNREAD,
    MULTIPLE_MESSAGE_SELECTED,
    MY_TRASH,
    MESSAGE_LIST_FILTERED,
    MESSAGE_LIST_IS_SEARCH_MODE
} from "~getters";
import { SELECT_ALL_MESSAGES, UNSELECT_ALL_MESSAGES } from "~mutations";
import {
    MARK_FOLDER_AS_READ,
    MARK_MESSAGES_AS_FLAGGED,
    MARK_MESSAGES_AS_READ,
    MARK_MESSAGES_AS_UNFLAGGED,
    MARK_MESSAGES_AS_UNREAD
} from "~actions";

export default {
    name: "MailMultipleSelectionActions",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon,
        MailComponentAlert,
        MailFolderIcon
    },
    mixins: [RemoveMixin],
    data() {
        return {
            isReadOnlyAlertDismissed: false,
            multipleSelectionIllustration
        };
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders", "mailboxes", "messages", "messageList", "selection"]),
        ...mapGetters("mail", {
            ALL_MESSAGES_ARE_SELECTED,
            ALL_SELECTED_MESSAGES_ARE_FLAGGED,
            ALL_SELECTED_MESSAGES_ARE_READ,
            ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
            ALL_SELECTED_MESSAGES_ARE_UNREAD,
            MULTIPLE_MESSAGE_SELECTED,
            MY_TRASH,
            MESSAGE_LIST_FILTERED,
            MESSAGE_LIST_IS_SEARCH_MODE
        }),
        anyMessageReadOnly() {
            return this.selection
                .map(messageKey => ItemUri.container(messageKey))
                .some(folderKey => !this.folders[folderKey].writable);
        },
        currentFolder() {
            return this.folders[this.activeFolder];
        }
    },
    methods: {
        ...mapActions("mail", {
            MARK_FOLDER_AS_READ,
            MARK_MESSAGES_AS_READ,
            MARK_MESSAGES_AS_UNREAD,
            MARK_MESSAGES_AS_FLAGGED,
            MARK_MESSAGES_AS_UNFLAGGED
        }),
        ...mapMutations("mail", { SELECT_ALL_MESSAGES, UNSELECT_ALL_MESSAGES }),
        ...mapMutations("mail-webapp/currentMessage", { clearCurrentMessage: "clear" }),
        removeSelection() {
            this.UNSELECT_ALL_MESSAGES();
            this.clearCurrentMessage();
        },
        markAsRead() {
            const mailbox = this.mailboxes[this.currentFolder.mailboxRef.key];
            const areAllMessagesInFolderSelected =
                this.ALL_MESSAGES_ARE_SELECTED && !this.MESSAGE_LIST_FILTERED && !this.MESSAGE_LIST_IS_SEARCH_MODE;
            areAllMessagesInFolderSelected
                ? this.MARK_FOLDER_AS_READ({ folder: this.currentFolder, mailbox })
                : this.MARK_MESSAGES_AS_READ(this.selection.map(key => this.messages[key]));
        },
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

$arrow-width: 3rem;
$arrow-height: $arrow-width / 2;

.mail-multiple-selection-actions {
    h1 {
        color: $info-dark;
        font-size: 2rem;
    }
    hr {
        height: 1px;
    }
    .actions-button {
        .btn {
            margin: $sp-2 $sp-1;
        }
        .arrow-up {
            position: relative;
            display: block;
            width: $arrow-width;
            height: $arrow-height;
            bottom: $arrow-height + $sp-2;
            left: calc(50% - #{$arrow-width});

            &::before {
                position: absolute;
                content: "";
                border-color: transparent;
                border-style: solid;
                bottom: 0;
                border-width: 0 ($arrow-width / 2) $arrow-height;
                border-bottom-color: $white;
            }
        }
    }
}
</style>
