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
            <slot name="subtitle" :size="fileSize">
                {{ fileSize }}
            </slot>
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

export default {
    name: "AttachmentInfos",
    components: {
        BmCol,
        BmIcon,
        BmRow
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
            return MimeType.matchingIcon(this.attachment.extra?.mime || this.attachment.mime);
        },
        fileSize() {
            return computeUnit(this.attachment.size || this.attachment.extra.size, this.$i18n);
        },
        previewUrl() {
            return getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.attachment);
        }
    }
};
</script>
