<template>
    <div class="mail-conversation-viewer-hidden-items">
        <div class="pl-5 row">
            <div class="col-1 vertical-line" />
            <div class="col spacer" />
        </div>
        <div class="pl-5 row">
            <div class="col-1 text-center">
                <bm-avatar :count="count" size="sm" />
            </div>
            <div class="col pl-3 align-self-center">
                <bm-button
                    class="font-weight-bold text-neutral pl-0"
                    variant="link"
                    @click="$emit('do-show-hidden-messages')"
                >
                    {{ $t("mail.conversation.show.middle.messages", { count }) }}
                </bm-button>
            </div>
        </div>
        <div class="pl-5 row">
            <div class="col-1" :class="{ 'vertical-line': !isLast }" />
            <div class="col spacer" />
        </div>
    </div>
</template>
<script>
import { BmButton, BmAvatar } from "@bluemind/styleguide";

export default {
    name: "MailConversationViewerHiddenItems",
    components: { BmButton, BmAvatar },
    props: {
        count: {
            type: Number,
            required: true
        },
        conversationSize: {
            type: Number,
            required: true
        },
        index: {
            type: Number,
            required: true
        }
    },
    computed: {
        isLast() {
            return this.index + this.count === this.conversationSize;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-conversation-viewer-hidden-items {
    background-color: $neutral-bg;
}
</style>
