<template>
    <div>
        <bm-button
            v-if="message.states.includes('not-seen')"
            variant="none"
            class="unread"
            :aria-label="$tc('mail.actions.mark_read.aria')"
            @click="updateSeen({ folder, id: message.id, isSeen: true })"
        >
            <bm-icon icon="read" size="2x" />
            {{ $tc("mail.actions.mark_read") }}
        </bm-button>
        <bm-button
            v-else
            variant="none"
            class="read"
            :aria-label="$tc('mail.actions.mark_unread.aria')"
            @click="updateSeen({ folder, id: message.id, isSeen: false })"
        >
            <bm-icon icon="unread" size="2x" />
            {{ $tc("mail.actions.mark_unread") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.toolbar.move.aria')">
            <bm-icon icon="folder" size="2x" />
            {{ $tc("mail.toolbar.move") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.actions.spam.aria')">
            <bm-icon icon="forbidden" size="2x" />
            {{ $tc("mail.actions.spam") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.actions.remove.aria')" @click="shouldRemoveItem(message.id)">
            <bm-icon icon="trash" size="2x" />
            {{ $tc("mail.actions.remove") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.toolbar.more.aria')">
            <bm-icon icon="3dots" size="2x" />
            {{ $tc("mail.toolbar.more") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapMutations, mapGetters } from "vuex";

export default {
    name: "MailToolbarConsultMessage",
    components: {
        BmButton,
        BmIcon
    },
    computed: {
        ...mapGetters("backend.mail/items", { message: "currentMessage" }),
        ...mapGetters("backend.mail/folders", { folder: "currentFolder" })
    },
    methods: {
        ...mapActions("backend.mail/items", ["updateSeen"]),
        ...mapMutations("backend.mail/items", ["shouldRemoveItem"])
    }
};
</script>

<style>
.unread,
.read {
    width: 8rem;
}
</style>
