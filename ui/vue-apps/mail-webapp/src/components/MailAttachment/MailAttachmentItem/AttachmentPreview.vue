<template>
    <div
        ref="preview-div"
        class="text-center attachment-preview overflow-hidden d-flex justify-content-center align-items-center"
    >
        <img v-if="hasPreview" ref="preview-image" :src="previewUrl" :alt="$tc('common.attachmentPreview')" />
        <div v-else class="preview w-100 text-center mb-1 p-1">
            <bm-icon :icon="fileTypeIcon" size="6x" class="m-auto preview-file-type" />
        </div>
    </div>
</template>
<script>
import { MimeType, getPartDownloadUrl } from "@bluemind/email";
import { BmIcon } from "@bluemind/styleguide";
import { AttachmentStatus } from "~/model/attachment";

export default {
    name: "AttachmentPreview",
    components: {
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
        }
    },
    data() {
        return {
            fitPreviewImage: false
        };
    },
    computed: {
        hasPreview() {
            return (
                MimeType.previewAvailable(this.attachment.mime) && this.attachment.status === AttachmentStatus.UPLOADED
            );
        },
        fileTypeIcon() {
            return MimeType.matchingIcon(this.attachment.extra.mime || this.attachment.mime);
        },
        previewUrl() {
            return getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.attachment);
        }
    }
};
</script>
<style lang="scss">
.attachment-preview {
    & > img {
        max-width: 100%;
    }
}
</style>
