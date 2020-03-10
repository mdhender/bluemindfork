<template>
    <div class="mail-toolbar-consult-message">
        <bm-button
            v-if="currentMessage.states.includes('not-seen')"
            v-bm-tooltip.bottom.ds500
            variant="link"
            class="unread"
            :title="$tc('mail.actions.mark_read.aria')"
            :aria-label="$tc('mail.actions.mark_read.aria')"
            @click="markAsRead([currentMessage.key])"
        >
            <bm-icon icon="read" size="2x" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_read") }}</span>
        </bm-button>
        <bm-button
            v-else
            v-bm-tooltip.bottom.ds500
            variant="link"
            class="read"
            :title="$tc('mail.actions.mark_unread.aria')"
            :aria-label="$tc('mail.actions.mark_unread.aria')"
            @click="markAsUnread([currentMessage.key])"
        >
            <bm-icon icon="unread" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.mark_unread") }}</span>
        </bm-button>
        <mail-toolbar-consult-message-move-action />
        <bm-button
            v-bm-tooltip.bottom.ds500
            variant="link"
            :title="$tc('mail.actions.spam.aria')"
            :aria-label="$tc('mail.actions.spam.aria')"
        >
            <bm-icon icon="forbidden" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.spam") }}</span>
        </bm-button>
        <bm-button
            v-bm-tooltip.bottom.ds500
            variant="link"
            :title="$tc('mail.actions.remove.aria')"
            :aria-label="$tc('mail.actions.remove.aria')"
            @click.exact="remove"
            @click.shift.exact="openPurgeModal"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
        <mail-toolbar-consult-message-other-actions />
    </div>
</template>

<script>
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import MailToolbarConsultMessageMoveAction from "./MailToolbarConsultMessageMoveAction";
import MailToolbarConsultMessageOtherActions from "./MailToolbarConsultMessageOtherActions";
import { SHOW_PURGE_MODAL } from "../../VueBusEventTypes";

export default {
    name: "MailToolbarConsultMessage",
    components: {
        BmButton,
        BmIcon,
        MailToolbarConsultMessageMoveAction,
        MailToolbarConsultMessageOtherActions
    },
    directives: { BmTooltip },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapGetters("mail-webapp", ["nextMessageKey", "my"]),
        ...mapGetters("mail-webapp/currentMessage", { currentMessage: "message" })
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread"]),
        openPurgeModal() {
            this.$bus.$emit(SHOW_PURGE_MODAL);
        },
        remove() {
            if (this.currentFolderKey === this.my.TRASH.key) {
                this.openPurgeModal();
                return;
            }
            this.$router.push("" + (this.nextMessageKey || ""));
            this.$store.dispatch("mail-webapp/remove", this.currentMessage.key);
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
