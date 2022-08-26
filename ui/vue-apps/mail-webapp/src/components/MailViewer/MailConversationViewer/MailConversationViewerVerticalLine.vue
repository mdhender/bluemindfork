<template>
    <div
        class="vertical-line"
        :class="{
            'vertical-line-after-avatar': afterAvatar,
            'vertical-line-transparent': afterAvatar ? index === maxIndex : index === 0
        }"
    />
</template>
<script>
export default {
    name: "MailConversationViewerVerticalLine",
    props: {
        index: {
            type: Number,
            required: true
        },
        maxIndex: {
            type: Number,
            required: true
        },
        afterAvatar: {
            type: Boolean,
            default: false
        }
    }
};
</script>
