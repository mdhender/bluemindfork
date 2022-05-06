<template>
    <div class="default-attachment">
        <attachment-preview v-if="!compact" :attachment="attachment" :message="message" />
        <attachment-infos :attachment="attachment" :message="message">
            <template v-slot:actions="scope">
                <slot name="actions" v-bind="scope" />
            </template>
        </attachment-infos>
    </div>
</template>

<script>
import AttachmentInfos from "./AttachmentInfos";
import AttachmentPreview from "./AttachmentPreview";

export default {
    name: "DefaultAttachment",
    components: {
        AttachmentPreview,
        AttachmentInfos
    },
    props: {
        attachment: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        },
        compact: {
            type: Boolean,
            default: false
        }
    }
};
</script>
