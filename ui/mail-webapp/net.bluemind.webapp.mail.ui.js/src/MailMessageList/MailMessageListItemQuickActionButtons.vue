<template>
    <bm-button-toolbar class="mail-message-list-item-quick-action-buttons pt-1 justify-content-end">
        <bm-button-group>
            <bm-button
                :aria-label="$tc('mail.actions.remove.aria')"
                class="p-1 mr-2 border-0 no-bg hovershadow"
                @click.prevent="shouldRemoveItem(message.uid)"
            >
                <bm-icon icon="trash" size="lg" />
            </bm-button>
            <bm-button
                v-if="message.states.includes('not-seen')"
                class="p-1 border-0 no-bg hovershadow"
                :aria-label="$tc('mail.actions.mark_read.aria')"
                @click.prevent="markAsSeen(true)"
            >
                <bm-icon class="hovershadow" icon="unread" size="lg" />
            </bm-button>
            <bm-button
                v-else
                class="p-1 border-0 no-bg hovershadow"
                :aria-label="$tc('mail.actions.mark_unread.aria')"
                @click.prevent="markAsSeen(false)"
            >
                <bm-icon icon="read" size="lg" />
            </bm-button>
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmButtonGroup, BmButton, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapMutations } from "vuex";

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
        ...mapGetters("backend.mail/folders", { folder: "currentFolder" })
    },
    methods: {
        ...mapMutations("backend.mail/items", ["shouldRemoveItem"]),
        markAsSeen(isSeen) {
            this.$store.dispatch("backend.mail/items/updateSeen", {
                folder: this.folder,
                uid: this.message.uid,
                isSeen: isSeen
            });
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
