<template>
    <bm-file-drop-zone
        class="mail-composer-attachments z-index-110 attachments mb-2"
        file-type-regex="image/(jpeg|jpg|png|gif)"
        v-on="$listeners"
    >
        <template #dropZone>
            <bm-icon icon="paper-clip" size="2x" />
            <h2 class="text-center p-2">
                {{ $tc("mail.new.attachments.images.drop.zone", draggedFilesCount) }}
            </h2>
        </template>
        <mail-attachments-block v-if="message.attachments.length > 0" :message="message" expanded />
    </bm-file-drop-zone>
</template>

<script>
import { BmIcon, BmFileDropZone } from "@bluemind/styleguide";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";

export default {
    name: "MailComposerAttachments",
    components: {
        BmFileDropZone,
        BmIcon,
        MailAttachmentsBlock
    },
    props: {
        draggedFilesCount: {
            type: Number,
            default: 0
        },
        message: {
            type: Object,
            required: true
        }
    }
};
</script>
