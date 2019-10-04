<template>
    <bm-button-toolbar class="mail-message-list-item-quick-action-buttons pt-1 justify-content-end">
        <bm-button-group>
            <bm-button
                :aria-label="$tc('mail.actions.remove.aria')"
                class="p-1 mr-2 border-0 no-bg hovershadow"
                @click.prevent="remove(message.id)"
            >
                <bm-icon icon="trash" size="lg" />
            </bm-button>
            <bm-button
                v-if="message.states.includes('not-seen')"
                class="p-1 border-0 no-bg hovershadow"
                :aria-label="$tc('mail.actions.mark_read.aria')"
                @click.prevent="markAsRead(message.id)"
            >
                <bm-icon class="hovershadow" icon="unread" size="lg" />
            </bm-button>
            <bm-button
                v-else
                class="p-1 border-0 no-bg hovershadow"
                :aria-label="$tc('mail.actions.mark_unread.aria')"
                @click.prevent="markAsUnread(message.id)"
            >
                <bm-icon icon="read" size="lg" />
            </bm-button>
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmButtonGroup, BmButton, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";

export default {
    name: "MailMessageListItemQuickActionButtons",
    components: {
        BmButtonToolbar,
        BmButtonGroup,
        BmButton,
        BmIcon
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail-webapp", ["nextMessageId"]),
        ...mapState("mail-webapp", ["currentMessageId"])
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread"]),
        remove() {
            if (this.currentMessageId == this.message.id) {
                this.$router.push("" + (this.nextMessageId || ""));
            }
            this.$store.dispatch("mail-webapp/remove", this.message.id);
        }
    }
};
</script>
<style>
.mail-message-list-item-quick-action-buttons .no-bg {
    background: none !important;
}
.mail-message-list-item-quick-action-buttons .hovershadow:hover {
    box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.25);
    border-radius: 1px;
}
</style>
