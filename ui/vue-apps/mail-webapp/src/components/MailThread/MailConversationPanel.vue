<template>
    <article
        class="mail-conversation-panel overflow-x-hidden"
        :aria-label="$t('mail.application.region.conversation')"
        :class="{ 'bg-surface': !isComposerDisplayedAlone }"
    >
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
import MailViewerLoading from "../MailViewer/MailViewerLoading";
import { ACTIVE_MESSAGE, CONVERSATION_IS_LOADED, CURRENT_CONVERSATION_METADATA } from "~/getters";
import { mapGetters } from "vuex";

export default {
    name: "MailConversationPanel",
    components: { MailThread, MailMessage, MailViewerLoading },
    computed: {
        ...mapGetters("mail", { ACTIVE_MESSAGE, CONVERSATION_IS_LOADED, CURRENT_CONVERSATION_METADATA }),
        conversationSize() {
            return this.CURRENT_CONVERSATION_METADATA?.messages?.length || 0;
        },
        isComposerDisplayedAlone() {
            return this.conversationSize === 1 && this.ACTIVE_MESSAGE?.composing === true;
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
