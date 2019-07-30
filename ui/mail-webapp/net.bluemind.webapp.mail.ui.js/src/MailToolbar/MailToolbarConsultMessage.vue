<template>
    <div>
        <bm-button
            v-if="!!uid && message.states.includes('not-seen')"
            :disabled="!uid"
            variant="none"
            class="unread"
            :aria-label="$tc('mail.actions.mark_read.aria')"
            @click="updateSeen({ folder, uid, isSeen: true })"
        >
            <bm-icon icon="read" size="2x" />
            {{ $tc("mail.actions.mark_read") }}
        </bm-button>
        <bm-button
            v-else
            :disabled="!uid"
            variant="none"
            class="read"
            :aria-label="$tc('mail.actions.mark_unread.aria')"
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
    </div>
</template>

<script>
import { BmButton, BmIcon }  from "@bluemind/styleguide";
import { mapActions, mapState, mapGetters } from "vuex";

export default {
    name: "MailToolbarConsultMessage",
    components: {
        BmButton,
        BmIcon
    },
    computed: {
        ...mapGetters("backend.mail/items", { message: "currentMessage" }),
        ...mapGetters("backend.mail/folders", { folder: "currentFolder" }),
        ...mapState("backend.mail/items", { uid: "current" })
    },
    methods: {
        ...mapActions("backend.mail/items", ["updateSeen"])
    }
};
</script>

<style>
.unread, .read {
    width: 8rem;
}
</style>