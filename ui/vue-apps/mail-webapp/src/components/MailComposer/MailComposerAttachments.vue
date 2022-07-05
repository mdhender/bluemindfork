<template>
    <bm-file-drop-zone
        class="mail-composer-attachments z-index-110 attachments mb-2"
        :should-activate-fn="shouldActivateForImages"
        v-on="$listeners"
    >
        <template #dropZone>
            <bm-icon icon="paper-clip" size="2x" />
            <h2 class="text-center p-2">
                {{ $tc("mail.new.attachments.images.drop.zone", draggedFilesCount) }}
            </h2>
        </template>
        <mail-attachments-block :attachments="message.attachments" :message="message" expanded />
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
    },
    methods: {
        shouldActivateForImages(event) {
            const regex = "image/(jpeg|jpg|png|gif)";
            const files = event.dataTransfer.items.length
                ? Object.keys(event.dataTransfer.items).map(key => event.dataTransfer.items[key])
                : [];
            const matchFunction = f => f.type.match(new RegExp(regex, "i"));
            return files.length > 0 && files.every(matchFunction);
        }
    }
};
</script>
