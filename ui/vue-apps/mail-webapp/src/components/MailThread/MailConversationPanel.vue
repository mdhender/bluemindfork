<template>
    <article class="mail-conversation-panel overflow-x-hidden" :aria-label="$t('mail.application.region.conversation')">
        <template v-if="CONVERSATION_IS_LOADED(CURRENT_CONVERSATION_METADATA)">
            <mail-thread v-if="conversationSize > 1" />
            <mail-message v-else-if="conversationSize == 1" />
        </template>
        <mail-viewer-loading v-else />
    </article>
</template>

<script>
import MailMessage from "./MailMessage";
import MailThread from "./MailThread";
import { CURRENT_CONVERSATION_METADATA } from "~/getters";
import MailViewerLoading from "../MailViewer/MailViewerLoading";
import { CONVERSATION_IS_LOADED } from "~/getters";
import { mapGetters } from "vuex";

export default {
    name: "MailConversationPanel",
    components: { MailThread, MailMessage, MailViewerLoading },
    computed: {
        ...mapGetters("mail", { CONVERSATION_IS_LOADED, CURRENT_CONVERSATION_METADATA }),
        conversationSize() {
            return this.CURRENT_CONVERSATION_METADATA?.messages?.length || 0;
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
