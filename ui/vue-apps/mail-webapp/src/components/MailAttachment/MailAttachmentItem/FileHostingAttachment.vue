<template>
    <div class="file-hosting-attachment">
        <attachment-preview v-if="!compact" :attachment="fileHosted" :message="message" />
        <attachment-infos :attachment="fileHosted" :message="message">
            <template v-slot:actions="scope">
                <slot name="actions" v-bind="scope" />
            </template>
            <template v-slot:subtitle="scope"> <bm-icon icon="cloud" class="mr-1" />{{ scope.size }} </template>
        </attachment-infos>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";
import AttachmentInfos from "./AttachmentInfos";
import AttachmentPreview from "./AttachmentPreview";

export default {
    name: "FileHostingAttachment",
    components: {
        AttachmentPreview,
        AttachmentInfos,
        BmIcon
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
    },
    computed: {
        fileHosted() {
            return { ...this.attachment, ...this.attachment.extra };
        }
    }
};
</script>
