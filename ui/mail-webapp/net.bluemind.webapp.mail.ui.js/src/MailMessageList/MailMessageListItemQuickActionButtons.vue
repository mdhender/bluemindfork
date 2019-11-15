<template>
    <bm-button-toolbar class="mail-message-list-item-quick-action-buttons pt-1 justify-content-end">
        <bm-button-group>
            <bm-button
                v-bm-tooltip.d500.top.viewport
                :aria-label="$tc('mail.actions.remove.aria')"
                :title="$tc('mail.actions.remove.aria')"
                class="p-1 mr-2 border-0 hovershadow"
                variant="link"
                @click.shift.exact.prevent="openPurgeModal"
                @click.exact.prevent="remove(message.id)"
            >
                <bm-icon icon="trash" size="lg" />
            </bm-button>
            <bm-button
                v-if="message.states.includes('not-seen')" 
                v-bm-tooltip.d500.top.viewport
                class="p-1 border-0 hovershadow"
                :aria-label="$tc('mail.actions.mark_read.aria')"
                :title="$tc('mail.actions.mark_read.aria')"
                variant="link"
                @click.prevent="markAsRead(message.id)"
            >
                <bm-icon icon="read" size="lg" />
            </bm-button>
            <bm-button
                v-else 
                v-bm-tooltip.d500.top.viewport
                class="p-1 border-0 hovershadow"
                :aria-label="$tc('mail.actions.mark_unread.aria')"
                :title="$tc('mail.actions.mark_unread.aria')"
                variant="link"
                @click.prevent="markAsUnread(message.id)"
            >
                <bm-icon icon="unread" size="lg" />
            </bm-button>
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmButtonGroup, BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import { SHOW_PURGE_MODAL } from "../VueBusEventTypes";

export default {
    name: "MailMessageListItemQuickActionButtons",
    components: {
        BmButtonToolbar,
        BmButtonGroup,
        BmButton,
        BmIcon
    },
    directives: { BmTooltip },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail-webapp", ["nextMessageId"]),
        ...mapState("mail-webapp", ["currentMessageId", "currentFolderUid"]),
        ...mapGetters("mail-webapp/folders", ["defaultFolders"]),
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread"]),
        remove() {
            if (this.currentFolderUid == this.defaultFolders.TRASH.uid) {
                this.openPurgeModal();
                return;
            }
            if (this.currentMessageId == this.message.id) {
                this.$router.push("" + (this.nextMessageId || ""));
            }
            this.$store.dispatch("mail-webapp/remove", this.message.id);
        },
        openPurgeModal() {
            this.$bus.$emit(SHOW_PURGE_MODAL, [ this.message.id ]);
        }
    }
};
</script>
<style>
.mail-message-list-item-quick-action-buttons .hovershadow:hover {
    box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.25);
}

</style>
