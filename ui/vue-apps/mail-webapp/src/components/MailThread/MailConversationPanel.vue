<template>
    <article
        class="mail-conversation-panel overflow-x-hidden bg-surface"
        :aria-label="$t('mail.application.region.conversation')"
    >
        <mail-thread v-if="conversationSize > 1" />
        <mail-message v-else-if="conversationSize == 1" />
        <mail-viewer-loading v-else />
    </article>
</template>

<script>
import MailMessage from "./MailMessage";
import MailThread from "./MailThread";
import { CURRENT_CONVERSATION_METADATA } from "~/getters";
import MailViewerLoading from "../MailViewer/MailViewerLoading";

export default {
    name: "MailConversationPanel",
    components: { MailThread, MailMessage, MailViewerLoading },
    computed: {
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
