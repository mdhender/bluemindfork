<template>
    <bm-button-toolbar class="message-list-item-quick-action-buttons justify-content-end">
        <bm-button-group>
            <bm-button
                v-bm-tooltip.ds500.top.viewport
                :aria-label="$tc('mail.actions.remove.aria')"
                :title="$tc('mail.actions.remove.aria')"
                class="p-1 mr-2 border-0 hovershadow"
                variant="link"
                @click.shift.exact.prevent="openPurgeModal"
                @click.exact.prevent="remove(message.key)"
            >
                <bm-icon icon="trash" size="sm" />
            </bm-button>
            <bm-button
                v-if="message.states.includes('not-seen')"
                v-bm-tooltip.ds500.top.viewport
                class="p-1 border-0 hovershadow"
                :aria-label="$tc('mail.actions.mark_read.aria')"
                :title="$tc('mail.actions.mark_read.aria')"
                variant="link"
                @click.prevent="markAsRead([message.key])"
            >
                <bm-icon icon="read" size="sm" />
            </bm-button>
            <bm-button
                v-else
                v-bm-tooltip.ds500.top.viewport
                class="p-1 border-0 hovershadow"
                :aria-label="$tc('mail.actions.mark_unread.aria')"
                :title="$tc('mail.actions.mark_unread.aria')"
                variant="link"
                @click.prevent="markAsUnread([message.key])"
            >
                <bm-icon icon="unread" size="sm" />
            </bm-button>
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmButtonGroup, BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import { SHOW_PURGE_MODAL } from "../VueBusEventTypes";

export default {
    name: "MessageListItemQuickActionButtons",
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
        ...mapGetters("mail-webapp", ["nextMessageKey", "my"]),
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" })
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread"]),
        remove() {
            if (this.currentFolderKey === this.my.TRASH.key) {
                this.openPurgeModal();
                return;
            }
            if (this.currentMessageKey === this.message.key) {
                this.$router.push("" + (this.nextMessageKey || ""));
            }
            this.$store.dispatch("mail-webapp/remove", this.message.key);
        },
        openPurgeModal() {
            this.$bus.$emit(SHOW_PURGE_MODAL, this.message.key);
        }
    }
};
</script>
<style>
.message-list-item-quick-action-buttons .hovershadow:hover {
    box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.25);
}
</style>
