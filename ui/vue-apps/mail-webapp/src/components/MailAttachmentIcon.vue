<template>
    <bm-icon
        v-if="message.hasAttachment || message.hasICS"
        class="mail-attachment-icon"
        :icon="message.hasICS ? 'event' : 'paper-clip'"
        :size="size"
    />
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "MailAttachmentIcon",
    components: {
        BmIcon
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        size: {
            type: String,
            default: undefined
        }
    }
};
</script>
