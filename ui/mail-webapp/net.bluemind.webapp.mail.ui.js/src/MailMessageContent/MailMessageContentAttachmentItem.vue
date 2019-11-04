<template>
    <bm-container class="mb-2 mail-message-content-attachment-item bg-white border border-light">
        <bm-row v-if="isExpanded" class="pt-1">
            <bm-col cols="12" class="px-1">
                <img v-if="hasPreview" :src="preview" class="w-100 preview">
                <div v-else class="preview w-100 d-flex align-items-center">
                    <bm-icon :icon="fileTypeIcon" size="7x" class="m-auto" />
                </div>
            </bm-col>
        </bm-row>
        <bm-row class="p-2">
            <bm-col class="col-auto pl-0">
                <bm-icon :icon="fileTypeIcon" size="lg" class="align-bottom" />
            </bm-col>
            <bm-col class="text-nowrap text-truncate flex-grow-1">
                <span 
                    v-bm-tooltip.hover.d500
                    :title="attachment.filename"
                    class="font-weight-bold"
                >
                    {{ attachment.filename }}
                </span>
                <br>
                {{ fileSize }}
            </bm-col>
            <bm-col class="col-auto pr-0">
                <bm-button 
                    variant="light"
                    class="p-2 h-100"
                    size="lg"
                    :aria-label="$tc('commons.downloadAttachement')" 
                    @click="$emit('save')"
                >
                    <bm-icon icon="download" size="lg" />
                </bm-button>
            </bm-col>
        </bm-row>
    </bm-container>
</template>

<script>
import { BmButton, BmCol, BmContainer, BmIcon, BmRow, BmTooltip } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
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
        ...mapGetters("mail-webapp", ["currentMessageAttachments"]),
        fileTypeIcon() {
            return MimeType.matchingIcon(this.attachment.mime);
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
            return "data:" + this.attachment.mime + ";base64, " 
                + this.currentMessageAttachments.find(a => this.attachment.address === a.address).content;
        }
    }
};
</script>

<style>
.mail-message-content-attachment-item .preview {
    height: 18vh;
}
</style>
