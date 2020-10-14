<template>
    <div class="mail-conversation-viewer-field-sep row pl-5">
        <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
        <div class="col pl-3 py-0"><hr /></div>
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
.mail-conversation-viewer-field-sep {
    hr {
        margin-top: 0.25rem;
    }
}
</style>
