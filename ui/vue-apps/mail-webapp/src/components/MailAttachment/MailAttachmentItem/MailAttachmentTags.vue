<template>
    <bm-extension
        id="webapp.mail"
        class="mail-attachment-tags"
        path="attachment.infos.tags"
        :attachment="attachment"
        :message="message"
    />
</template>

<script>
import { BmExtension } from "@bluemind/extensions.vue";
export default {
    name: "MailAttachmentTags",
    components: { BmExtension },
    props: {
        attachment: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    }
};
</script>
