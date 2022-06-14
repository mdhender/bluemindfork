<template>
    <bm-row class="no-gutters align-items-center attachment-infos">
        <bm-col
            class="col-auto"
            :title="$t('mail.content.file-type', { fileType: $t('mail.content.' + fileTypeIcon) })"
        >
            <bm-icon :icon="fileTypeIcon" size="2x" class="align-bottom" />
        </bm-col>
        <bm-col class="text-nowrap text-truncate flex-grow-1 px-2 attachment-text">
            <span :title="attachment.fileName" class="font-weight-bold">{{ attachment.fileName }} </span>
            <br />
            <div class="d-inline-flex">
                <mail-attachment-tags :attachment="attachment" :message="message" />
                {{ fileSize }}
            </div>
        </bm-col>
        <bm-col class="col-auto py-1">
            <slot name="actions" />
        </bm-col>
    </bm-row>
</template>
<script>
import { BmCol, BmIcon, BmRow } from "@bluemind/styleguide";
import { MimeType, getPartDownloadUrl } from "@bluemind/email";
import { computeUnit } from "@bluemind/file-utils";
import MailAttachmentTags from "./MailAttachmentTags";

export default {
    name: "AttachmentInfos",
    components: {
        BmCol,
        BmIcon,
        BmRow,
        MailAttachmentTags
    },
    props: {
        attachment: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        fileTypeIcon() {
            return MimeType.matchingIcon(this.attachment.mime);
        },
        fileSize() {
            return this.attachment.size > 0 ? computeUnit(this.attachment.size, this.$i18n) : "--";
        },
        previewUrl() {
            return getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.attachment);
        }
    }
};
</script>
