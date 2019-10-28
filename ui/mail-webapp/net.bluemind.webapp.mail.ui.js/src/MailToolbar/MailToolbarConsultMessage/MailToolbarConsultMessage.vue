<template>
    <div class="mail-toolbar-consult-message">
        <global-events @keydown.tab.capture="forceCloseMoveAutocomplete" />
        <bm-button
            v-if="currentMessage.states.includes('not-seen')"
            variant="none"
            class="unread"
            :aria-label="$tc('mail.actions.mark_read.aria')"
            @click="markAsRead(currentMessage.id)"
        >
            <bm-icon icon="read" size="2x" /> {{ $tc("mail.actions.mark_read") }}
        </bm-button>
        <bm-button
            v-else
            variant="none"
            class="read"
            :aria-label="$tc('mail.actions.mark_unread.aria')"
            @click="markAsUnread(currentMessage.id)"
        >
            <bm-icon icon="unread" size="2x" /> {{ $tc("mail.actions.mark_unread") }}
        </bm-button>
        <mail-toolbar-consult-message-move-action />
        <bm-button variant="none" :aria-label="$tc('mail.actions.spam.aria')">
            <bm-icon icon="forbidden" size="2x" />
            {{ $tc("mail.actions.spam") }}
        </bm-button>
        <bm-button 
            variant="none"
            :aria-label="$tc('mail.actions.remove.aria')"
            @click.exact="remove"
            @click.shift.exact="openPurgeModal"
        >
            <bm-icon icon="trash" size="2x" />
            {{ $tc("mail.actions.remove") }}
        </bm-button>
        <mail-toolbar-consult-message-other-actions />
    </div>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import MailToolbarConsultMessageMoveAction from "./MailToolbarConsultMessageMoveAction";
import MailToolbarConsultMessageOtherActions from "./MailToolbarConsultMessageOtherActions";
import { SHOW_PURGE_MODAL } from "../../VueBusEventTypes";

export default {
    name: "MailToolbarConsultMessage",
    components: {
        BmButton,
        BmIcon,
        GlobalEvents,
        MailToolbarConsultMessageMoveAction,
        MailToolbarConsultMessageOtherActions
    },
    computed: {
        ...mapState("mail-webapp", ["currentFolderUid"]),
        ...mapGetters("mail-webapp", ["currentMessage", "nextMessageId"]),
        ...mapGetters("mail-webapp/folders", ["defaultFolders"]),
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread"]),
        openPurgeModal() {
            this.$bus.$emit(SHOW_PURGE_MODAL);
        },
        remove() {
            if (this.currentFolderUid == this.defaultFolders.TRASH.uid) {
                this.openPurgeModal();
                return;
            }
            this.$router.push("" + (this.nextMessageId || ""));
            this.$store.dispatch("mail-webapp/remove", this.currentMessage.id);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-toolbar-consult-message .unread,
.mail-toolbar-consult-message .read {
    width: 8rem;
}
</style>
