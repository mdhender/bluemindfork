<template>
    <div class="mail-toolbar-consult-message">
        <bm-button
            v-show="displayMarkAsRead"
            v-bm-tooltip.bottom.ds500
            variant="simple-dark"
            class="unread"
            :title="$tc('mail.actions.mark_read.aria', selectedMessageKeys.length)"
            :aria-label="$tc('mail.actions.mark_read.aria', selectedMessageKeys.length)"
            @click="doMarkAsRead"
        >
            <bm-icon icon="read" size="2x" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_read") }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsUnread"
            v-bm-tooltip.bottom.ds500
            variant="simple-dark"
            class="read"
            :title="$tc('mail.actions.mark_unread.aria', selectedMessageKeys.length)"
            :aria-label="$tc('mail.actions.mark_unread.aria', selectedMessageKeys.length)"
            @click="doMarkAsUnread"
        >
            <bm-icon icon="unread" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.mark_unread", selectedMessageKeys.length) }}</span>
        </bm-button>
        <mail-toolbar-selected-messages-move-action
            v-show="!hasMultipleMessagesSelected && !isReadOnlyFolder(folderUidOfCurrentMessage)"
        />
        <bm-button
            v-show="!hasMultipleMessagesSelected && !isReadOnlyFolder(folderUidOfCurrentMessage)"
            v-bm-tooltip.bottom.ds500
            variant="simple-dark"
            :title="$tc('mail.actions.remove.aria')"
            :aria-label="$tc('mail.actions.remove.aria')"
            @click.exact="remove"
            @click.shift.exact="purge"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
        <mail-toolbar-selected-messages-other-actions
            v-if="!hasMultipleMessagesSelected && !isReadOnlyFolder(folderUidOfCurrentMessage)"
        />
    </div>
</template>

<script>
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { ItemUri } from "@bluemind/item-uri";
import { mapActions, mapGetters, mapState } from "vuex";
import MailToolbarSelectedMessagesMoveAction from "./MailToolbarSelectedMessagesMoveAction";
import MailToolbarSelectedMessagesOtherActions from "./MailToolbarSelectedMessagesOtherActions";

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
            "nextMessageKey",
            "my",
            "areAllSelectedMessagesRead",
            "areAllSelectedMessagesUnread",
            "areAllMessagesSelected",
            "areMessagesFiltered",
            "isSearchMode",
            "isReadOnlyFolder"
        ]),
        ...mapGetters("mail-webapp/currentMessage", { currentMessage: "message" }),
        hasMultipleMessagesSelected() {
            return this.selectedMessageKeys.length > 1;
        },
        displayMarkAsRead() {
            if (this.hasMultipleMessagesSelected) {
                return !this.areAllSelectedMessagesRead;
            } else {
                return this.currentMessage.states.includes("not-seen");
            }
        },
        displayMarkAsUnread() {
            if (this.hasMultipleMessagesSelected) {
                return !this.areAllSelectedMessagesUnread;
            } else {
                return !this.currentMessage.states.includes("not-seen");
            }
        },
        folderUidOfCurrentMessage() {
            return ItemUri.container(this.currentMessage.key);
        }
    },
    methods: {
        ...mapActions("mail-webapp", {
            markMessagesAsRead: "markAsRead",
            markAsUnread: "markAsUnread",
            markFolderAsRead: "markFolderAsRead"
        }),
        async purge() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("mail.actions.purge.modal.content", { subject: this.currentMessage.subject }),
                {
                    title: this.$t("mail.actions.purge.modal.title"),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false
                }
            );
            if (confirm) {
                this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
                this.$store.dispatch("mail-webapp/purge", this.currentMessage.key);
            }
        },
        remove() {
            if (this.currentFolderKey === this.my.TRASH.key) {
                this.purge();
            } else {
                this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
                this.$store.dispatch("mail-webapp/remove", this.currentMessage.key);
            }
        },
        doMarkAsRead() {
            const selectedKeys = this.hasMultipleMessagesSelected
                ? this.selectedMessageKeys
                : [this.currentMessage.key];

            const areAllMessagesInFolderSelected =
                this.areAllMessagesSelected && !this.areMessagesFiltered && !this.isSearchMode;
            areAllMessagesInFolderSelected
                ? this.markFolderAsRead(this.currentFolderKey)
                : this.markMessagesAsRead(selectedKeys);
        },
        doMarkAsUnread() {
            if (this.hasMultipleMessagesSelected) {
                this.markAsUnread(this.selectedMessageKeys);
            } else {
                this.markAsUnread([this.currentMessage.key]);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-toolbar-consult-message .unread,
.mail-toolbar-consult-message .read {
    @media (min-width: map-get($grid-breakpoints, "lg")) {
        width: 8rem;
    }
}
</style>
