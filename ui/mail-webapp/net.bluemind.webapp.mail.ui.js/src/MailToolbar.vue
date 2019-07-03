<template>
    <bm-button-toolbar key-nav class="mail-toolbar flex-nowrap w-50">
        <bm-button
            v-if="!!uid && message.states.includes('not-seen')"
            :disabled="!uid"
            variant="none"
            :aria-label="$tc('mail.actions.mark_read.aria')"
            class="text-nowrap text-truncate"
            @click="updateSeen({ folder, uid, isSeen: true })"
        >
            <bm-icon icon="read" size="2x" />
            {{ $tc("mail.actions.mark_read") }}
        </bm-button>
        <bm-button
            v-else
            :disabled="!uid"
            variant="none"
            :aria-label="$tc('mail.actions.mark_unread.aria')"
            class="text-nowrap text-truncate"
            @click="updateSeen({ folder, uid, isSeen: false })"
        >
            <bm-icon icon="unread" size="2x" />
            {{ $tc("mail.actions.mark_unread") }}
        </bm-button>
        <bm-button :disabled="!uid" variant="none" :aria-label="$tc('mail.actions.remove.aria')">
            <bm-icon icon="trash" size="2x" />
            {{ $tc("mail.actions.remove") }}
        </bm-button>
        <bm-button :disabled="!uid" variant="none" :aria-label="$tc('mail.toolbar.move.aria')">
            <bm-icon icon="folder" size="2x" />
            {{ $tc("mail.toolbar.move") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.toolbar.more.aria')">
            <bm-icon icon="3dots" size="2x" />
            {{ $tc("mail.toolbar.more") }}
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import BmButtonToolbar from "@bluemind/styleguide/components/buttons/BmButtonToolbar";
import BmButton from "@bluemind/styleguide/components/buttons/BmButton";
import BmIcon from "@bluemind/styleguide/components/BmIcon";
import { mapActions, mapState, mapGetters } from "vuex";
export default {
    name: "MailToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    },
    computed: {
        ...mapGetters("backend.mail/items", { message: "currentMessage" }),
        ...mapState("backend.mail/items", { uid: "current" }),
        ...mapGetters("backend.mail/folders", { folder: "currentFolder" })
    },
    methods: {
        ...mapActions("backend.mail/items", ["updateSeen"])
    }
};
</script>

<style lang="scss">
//TODO might move inside bluemind-styleguide
@import "~@bluemind/styleguide/css/variables";

.mail-toolbar .btn {
    flex-basis: 11em;
    flex-grow: 1;
    flex-shrink: 0;
}

.mail-toolbar .btn:focus,
.mail-toolbar .btn.focus {
    box-shadow: none !important;
}
</style>
