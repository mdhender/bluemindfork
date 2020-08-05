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
                <h1>{{ selectedMessageKeys.length }}</h1>
                <h1>{{ $t("mail.message.selected") }}</h1>
            </div>

            <div class="bg-white py-2 px-3 actions-button w-75 mt-4">
                <div class="arrow-up" />
                <bm-button v-if="!areAllSelectedMessagesRead" variant="outline-secondary" @click="markAsRead()">
                    <bm-label-icon icon="read">{{
                        $tc("mail.actions.mark_as_read", selectedMessageKeys.length)
                    }}</bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="!areAllSelectedMessagesUnread"
                    variant="outline-secondary"
                    @click="markAsUnread(selectedMessageKeys)"
                >
                    <bm-label-icon icon="unread">
                        {{ $tc("mail.actions.mark_as_unread", selectedMessageKeys.length) }}
                    </bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="!areAllSelectedMessagesFlagged && !anyMessageReadOnly"
                    variant="outline-secondary"
                    @click="markAsFlagged(selectedMessageKeys)"
                >
                    <bm-label-icon icon="flag-outline">{{
                        $tc("mail.actions.mark_flagged", selectedMessageKeys.length)
                    }}</bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="!areAllSelectedMessagesUnflagged && !anyMessageReadOnly"
                    variant="outline-secondary"
                    @click="markAsUnflagged(selectedMessageKeys)"
                >
                    <bm-label-icon icon="flag-fill">
                        {{ $tc("mail.actions.mark_unflagged", selectedMessageKeys.length) }}
                    </bm-label-icon>
                </bm-button>
                <bm-button
                    v-if="!anyMessageReadOnly"
                    variant="outline-secondary"
                    @click.exact="removeSelectedMessages"
                    @click.shift.exact="purgeSelectedMessages"
                >
                    <bm-label-icon icon="trash">
                        {{ $tc("mail.actions.remove", selectedMessageKeys.length) }}
                    </bm-label-icon>
                </bm-button>
            </div>

            <bm-button variant="inline-secondary" class="my-4" @click="removeSelection">
                {{ $t("common.cancel.selection") }}
            </bm-button>

            <hr v-if="!areAllMessagesSelected" class="w-75 border-dark" />

            <div v-if="!areAllMessagesSelected" class="mt-3">
                <h3 v-if="!isSearchMode" class="d-inline px-3 align-middle">
                    {{ $t("mail.message.select.all.folder") }}
                    <mail-folder-icon :shared="isFolderOfMailshare(currentFolder)" :folder="currentFolder">
                        <span class="font-weight-bold">{{ currentFolder.path }}</span>
                    </mail-folder-icon>
                </h3>
                <h3 v-else class="d-inline px-3 align-middle">
                    {{ $t("mail.message.select.all.search") }}
                    <bm-icon icon="search" /> <span class="font-weight-bold">"{{ search.pattern }}"</span>
                </h3>
                <bm-button @click="addAllToSelectedMessages(itemKeys)">{{ $t("common.select.all") }}</bm-button>
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

export default {
    name: "MailMultipleSelectionActions",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon,
        MailComponentAlert,
        MailFolderIcon
    },
    data() {
        return {
            isReadOnlyAlertDismissed: false,
            multipleSelectionIllustration
        };
    },
    computed: {
        ...mapState("mail-webapp", ["selectedMessageKeys", "search"]),
        ...mapState("mail-webapp/messages", ["itemKeys"]),
        ...mapGetters("mail-webapp", [
            "areAllMessagesSelected",
            "areAllSelectedMessagesFlagged",
            "areAllSelectedMessagesRead",
            "areAllSelectedMessagesUnflagged",
            "areAllSelectedMessagesUnread",
            "areMessagesFiltered",
            "isSearchMode",
            "nextMessageKey"
        ]),
        ...mapState("mail", ["folders", "mailboxes", "activeFolder"]),
        ...mapGetters("mail", ["MY_DEFAULT_FOLDERS"]),
        anyMessageReadOnly() {
            return this.selectedMessageKeys
                .map(messageKey => ItemUri.container(messageKey))
                .some(folderKey => !this.folders[folderKey].writable);
        },
        isSelectionMultiple() {
            return this.selectedMessageKeys.length > 1;
        },
        currentFolder() {
            return this.folders[this.activeFolder];
        }
    },
    methods: {
        ...mapActions("mail-webapp", {
            markAsFlagged: "markAsFlagged",
            markAsUnflagged: "markAsUnflagged",
            markAsUnread: "markAsUnread",
            markFolderAsRead: "markFolderAsRead",
            markMessagesAsRead: "markAsRead",
            purge: "purge",
            remove: "remove"
        }),
        ...mapMutations("mail-webapp", ["addAllToSelectedMessages", "deleteAllSelectedMessages"]),
        ...mapMutations("mail-webapp/currentMessage", { clearCurrentMessage: "clear" }),
        removeSelection() {
            this.deleteAllSelectedMessages();
            this.clearCurrentMessage();
        },
        markAsRead() {
            const areAllMessagesInFolderSelected =
                this.areAllMessagesSelected && !this.areMessagesFiltered && !this.isSearchMode;
            areAllMessagesInFolderSelected
                ? this.markFolderAsRead(this.activeFolder)
                : this.markMessagesAsRead(this.selectedMessageKeys);
        },
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailbox].type === "mailshares";
        },
        async purgeSelectedMessages() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$tc("mail.actions.purge.modal.content", this.selectedMessageKeys.length),
                {
                    title: this.$tc("mail.actions.purge.modal.title", this.selectedMessageKeys.length),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false
                }
            );
            if (confirm) {
                // do this before followed async operations
                const nextMessageKey = this.nextMessageKey;
                this.purge(this.selectedMessageKeys);
                if (!this.isSelectionMultiple) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
        },
        async removeSelectedMessages() {
            if (this.activeFolder === this.MY_DEFAULT_FOLDERS.TRASH.key) {
                this.purgeSelectedMessages();
            } else {
                // do this before followed async operations
                const nextMessageKey = this.nextMessageKey;
                this.remove(this.selectedMessageKeys);
                if (!this.isSelectionMultiple) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
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
