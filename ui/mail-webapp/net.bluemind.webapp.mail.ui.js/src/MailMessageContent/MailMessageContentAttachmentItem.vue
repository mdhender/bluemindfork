<template>
    <bm-container
        class="mail-message-content-attachment-item bg-white border border-light text-condensed py-1 px-2 mt-2"
    >
        <bm-row v-if="isExpanded" class="pt-1">
            <bm-col cols="12" class="px-1">
                <img v-if="hasPreview" :src="preview" class="w-100 preview mb-1" :alt="$tc('common.attachmentPreview')">
                <div v-else class="preview w-100 d-flex align-items-center mb-1 bg-light p-1">
                    <bm-icon :icon="fileTypeIcon" size="6x" class="m-auto bg-white preview-file-type" />
                </div>
            </bm-col>
        </bm-row>
        <bm-row class="no-gutters">
            <bm-col class="col-auto">
                <bm-icon :icon="fileTypeIcon" size="2x" class="align-bottom pt-1" />
            </bm-col>
            <bm-col class="text-nowrap text-truncate flex-grow-1 px-1">
                <span 
                    v-bm-tooltip.ds500
                    :title="attachment.filename"
                    class="font-weight-bold"
                >
                    {{ filename }}
                </span>
                <br>
                {{ fileSize }}
            </bm-col>
            <bm-col class="col-auto py-1">
                <bm-button 
                    v-bm-tooltip.ds500
                    variant="light"
                    class="p-0"
                    size="md"
                    :title="$tc('common.downloadAttachment')"
                    :aria-label="$tc('common.downloadAttachment')" 
                    @click="$emit('save')"
                >
                    <bm-icon icon="download" size="2x" class="p-1" />
                </bm-button>
            </bm-col>
        </bm-row>
    </bm-container>
</template>

<script>
import { BmButton, BmCol, BmContainer, BmIcon, BmRow, BmTooltip } from "@bluemind/styleguide";
import { MimeType } from "@bluemind/email";

function roundTo1Decimal(number) {
    return Math.round(number * 10) / 10;
}

export default {
    name: "MailMessageContentAttachmentItem",
    components: {
        BmButton,
        BmCol,
        BmContainer,
        BmIcon,
        BmRow
    },
    directives: { BmTooltip },
    props: {
        attachment: {
            type: Object,
            required: true
        },
        isExpanded: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        fileTypeIcon() {
            return MimeType.matchingIcon(this.attachment.mime);
        },
        filename() {
            return this.attachment.filename ? 
                this.attachment.filename : this.$t("mail.attachment.untitled", { mimeType: this.attachment.mime});
        },
        fileSize() {
            let size = this.attachment.size;
            if ((size / Math.pow(10, 9)) >=  1) {
                return roundTo1Decimal(size / Math.pow(10, 9)) + " Go";
            } else if ((size / Math.pow(10, 6)) >=  1) {
                return roundTo1Decimal(size / Math.pow(10, 6)) + " Mo";
            } else if (((size / Math.pow(10, 3)) >=  1)) {
                return roundTo1Decimal(size / Math.pow(10, 3)) + " Ko";
            } else {
                return size + " o";
            }
        },
        hasPreview() {
            return MimeType.previewAvailable(this.attachment.mime);
        },
        preview() {
            return URL.createObjectURL(this.attachment.content);
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-message-content-attachment-item .preview {
    height: 7em;
}

 .preview-file-type {
     color: $light !important;
}
</style>
