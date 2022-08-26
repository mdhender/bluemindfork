<template>
    <div class="mail-conversation-viewer-field-sep conversation-viewer-row d-flex flex-nowrap">
        <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
        <div class="flex-fill"><hr /></div>
    </div>
</template>

<script>
import MailConversationViewerVerticalLine from "./MailConversationViewerVerticalLine";

export default {
    name: "MailConversationViewerFieldSep",
    components: { MailConversationViewerVerticalLine },
    props: {
        index: {
            type: Number,
            required: true
        },
        maxIndex: {
            type: Number,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-conversation-viewer-field-sep {
    hr {
        margin-top: -1px;
        margin-bottom: $sp-3;
    }
}
</style>
