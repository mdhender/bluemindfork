<template>
    <div class="mail-toolbar-consult-message">
        <bm-button
            v-show="displayMarkAsRead"
            v-bm-tooltip.bottom
            variant="simple-dark"
            class="unread"
            :title="$tc('mail.actions.mark_read.aria', selectedMessageKeys.length || 1)"
            :aria-label="$tc('mail.actions.mark_read.aria', selectedMessageKeys.length || 1)"
            @click="doMarkAsRead"
        >
            <bm-icon icon="read" size="2x" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_read", selectedMessageKeys.length || 1) }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsUnread"
            v-bm-tooltip.bottom
            variant="simple-dark"
            class="read"
            :title="$tc('mail.actions.mark_unread.aria', selectedMessageKeys.length || 1)"
            :aria-label="$tc('mail.actions.mark_unread.aria', selectedMessageKeys.length || 1)"
            @click="doMarkAsUnread"
        >
            <bm-icon icon="unread" size="2x" />
            <span class="d-none d-lg-block">{{
                $tc("mail.actions.mark_unread", selectedMessageKeys.length || 1)
            }}</span>
        </bm-button>
        <mail-toolbar-selected-messages-move-action v-show="!isSelectionMultiple && !selectionHasReadOnlyFolders" />
        <bm-button
            v-show="!selectionHasReadOnlyFolders"
            v-bm-tooltip.bottom
            variant="simple-dark"
            :title="$tc('mail.actions.remove.aria')"
            :aria-label="$tc('mail.actions.remove.aria')"
            @click.exact="remove"
            @click.shift.exact="purge"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsFlagged"
            v-bm-tooltip.bottom
            variant="simple-dark"
            class="flagged"
            :title="$tc('mail.actions.mark_flagged.aria', selectedMessageKeys.length)"
            :aria-label="$tc('mail.actions.mark_flagged.aria', selectedMessageKeys.length)"
            @click="doMarkAsFlagged"
        >
            <bm-icon icon="flag-outline" size="2x" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_flagged") }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsUnflagged"
            v-bm-tooltip.bottom
            variant="simple-dark"
            class="unflagged"
            :title="$tc('mail.actions.mark_unflagged.aria', selectedMessageKeys.length)"
            :aria-label="$tc('mail.actions.mark_unflagged.aria', selectedMessageKeys.length)"
            @click="doMarkAsUnflagged"
        >
            <bm-icon icon="flag-fill" size="2x" class="text-warning" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_as_unflagged") }}</span>
        </bm-button>
        <mail-toolbar-selected-messages-other-actions v-if="!selectionHasReadOnlyFolders" />
    </div>
</template>

<script>
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { ItemUri } from "@bluemind/item-uri";
import { mapActions, mapGetters, mapState } from "vuex";
import MailToolbarSelectedMessagesMoveAction from "./MailToolbarSelectedMessagesMoveAction";
import MailToolbarSelectedMessagesOtherActions from "./MailToolbarSelectedMessagesOtherActions";
import { Flag } from "@bluemind/email";

export default {
    name: "MailToolbarSelectedMessages",
    components: {
        BmButton,
        BmIcon,
        MailToolbarSelectedMessagesMoveAction,
        MailToolbarSelectedMessagesOtherActions
    },
    directives: { BmTooltip },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey", "selectedMessageKeys"]),
        ...mapGetters("mail-webapp", [
            "areAllMessagesSelected",
            "areAllSelectedMessagesFlagged",
            "areAllSelectedMessagesRead",
            "areAllSelectedMessagesUnflagged",
            "areAllSelectedMessagesUnread",
            "areMessagesFiltered",
            "isSearchMode",
            "my",
            "nextMessageKey"
        ]),
        ...mapGetters("mail-webapp/currentMessage", { currentMessage: "message" }),
        ...mapState("mail", ["folders"]),
        isSelectionMultiple() {
            return this.selectedMessageKeys.length > 1;
        },
        displayMarkAsRead() {
            if (this.isSelectionMultiple) {
                return !this.areAllSelectedMessagesRead;
            } else {
                return this.currentMessage.states.includes("not-seen");
            }
        },
        displayMarkAsUnread() {
            if (this.isSelectionMultiple) {
                return !this.areAllSelectedMessagesUnread;
            } else {
                return !this.currentMessage.states.includes("not-seen");
            }
        },
        displayMarkAsFlagged() {
            if (this.selectionHasReadOnlyFolders) {
                return false;
            } else if (this.isSelectionMultiple) {
                return !this.areAllSelectedMessagesFlagged;
            } else {
                return !this.currentMessage.flags.includes(Flag.FLAGGED);
            }
        },
        displayMarkAsUnflagged() {
            if (this.selectionHasReadOnlyFolders) {
                return false;
            } else if (this.isSelectionMultiple) {
                return !this.areAllSelectedMessagesUnflagged;
            } else {
                return this.currentMessage.flags.includes(Flag.FLAGGED);
            }
        },
        selectionHasReadOnlyFolders() {
            const selection = this.selectedMessageKeys.length ? this.selectedMessageKeys : [this.currentMessage.key];
            return selection.some(messageKey => !this.folders[ItemUri.container(messageKey)].writable);
        }
    },
    methods: {
        ...mapActions("mail-webapp", {
            markAsUnread: "markAsUnread",
            markFolderAsRead: "markFolderAsRead",
            markMessagesAsFlagged: "markAsFlagged",
            markMessagesAsRead: "markAsRead",
            markMessagesAsUnflagged: "markAsUnflagged"
        }),
        async purge() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$tc("mail.actions.purge.modal.content", this.selectedMessageKeys.length || 1, {
                    subject: this.currentMessage && this.currentMessage.subject
                }),
                {
                    title: this.$tc("mail.actions.purge.modal.title", this.selectedMessageKeys.length || 1),
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
                this.$store.dispatch(
                    "mail-webapp/purge",
                    this.selectedMessageKeys.length > 1 ? this.selectedMessageKeys : this.currentMessage.key
                );
                if (!this.isSelectionMultiple) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
        },
        remove() {
            if (this.currentFolderKey === this.my.TRASH.key) {
                this.purge();
            } else {
                // do this before followed async operations
                const nextMessageKey = this.nextMessageKey;
                this.$store.dispatch(
                    "mail-webapp/remove",
                    this.selectedMessageKeys.length > 1 ? this.selectedMessageKeys : this.currentMessage.key
                );
                if (!this.isSelectionMultiple) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
        },
        selectedKeys() {
            return this.isSelectionMultiple ? this.selectedMessageKeys : [this.currentMessage.key];
        },
        doMarkAsRead() {
            const areAllMessagesInFolderSelected =
                this.areAllMessagesSelected && !this.areMessagesFiltered && !this.isSearchMode;
            areAllMessagesInFolderSelected
                ? this.markFolderAsRead(this.currentFolderKey)
                : this.markMessagesAsRead(this.selectedKeys());
        },
        doMarkAsUnread() {
            if (this.isSelectionMultiple) {
                this.markAsUnread(this.selectedMessageKeys);
            } else {
                this.markAsUnread([this.currentMessage.key]);
            }
        },
        doMarkAsFlagged() {
            this.markMessagesAsFlagged(this.selectedKeys());
        },
        doMarkAsUnflagged() {
            this.markMessagesAsUnflagged(this.selectedKeys());
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-toolbar-consult-message .unread,
.mail-toolbar-consult-message .read,
.mail-toolbar-consult-message .flagged,
.mail-toolbar-consult-message .unflagged {
    @media (min-width: map-get($grid-breakpoints, "lg")) {
        width: 8rem;
    }
}
</style>
