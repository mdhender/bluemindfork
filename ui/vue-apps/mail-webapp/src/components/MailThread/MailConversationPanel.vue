<template>
    <article
        class="mail-conversation-panel overflow-x-hidden bg-surface"
        :aria-label="$t('mail.application.region.conversation')"
    >
        <mail-thread v-if="conversationIsLoaded && conversationSize > 1" />
        <mail-message v-else-if="conversationIsLoaded && conversationSize == 1" />
        <mail-viewer-loading v-else />
    </article>
</template>

<script>
import MailMessage from "./MailMessage";
import MailThread from "./MailThread";
import { CONVERSATION_IS_LOADED, CURRENT_CONVERSATION_METADATA } from "~/getters";
import MailViewerLoading from "../MailViewer/MailViewerLoading";

export default {
    name: "MailConversationPanel",
    components: { MailThread, MailMessage, MailViewerLoading },
    computed: {
        conversationIsLoaded() {
            const conversation = this.$store.getters["mail/" + CURRENT_CONVERSATION_METADATA];
            return conversation && this.$store.getters["mail/" + CONVERSATION_IS_LOADED](conversation);
        },
        conversationSize() {
            return this.$store.getters["mail/" + CURRENT_CONVERSATION_METADATA]?.messages?.length || 0;
        }
    }
};
</script>

<style lang="scss">
.mail-conversation-panel {
    min-height: 100%;
    .overflow-x-hidden {
        overflow-x: hidden;
    }
}
</style>
