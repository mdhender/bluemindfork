<template>
    <bm-container class="mb-2 mail-message-content-attachment-item bg-white border border-light">
        <bm-row v-if="hasPreview" class="pt-1">
            <bm-col cols="12" class="px-1">
                <img :src="preview" class="w-100 preview">
            </bm-col>
        </bm-row>
        <bm-row>
            <bm-col class="p-2 col-auto">
                <bm-icon :icon="fileTypeIcon" size="lg" />
            </bm-col>
            <bm-col class="p-2 text-nowrap text-truncate flex-grow-1">
                <span class="font-weight-bold">{{ attachment.filename }}</span>
                <br>
                {{ fileSize }}
            </bm-col>
            <bm-col class="py-1 px-2 col-auto my-auto">
                <bm-button variant="light" class="p-1" size="lg" @click="save">
                    <bm-icon icon="download" size="lg" />
                </bm-button>
            </bm-col>
        </bm-row>
    </bm-container>
</template>

<script>
import { BmButton, BmCol, BmContainer, BmIcon, BmRow } from "@bluemind/styleguide";
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
    props: {
        attachment: {
            type: Object,
            required: true
        }
    },
    computed: {
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
            return "data:" + this.attachment.mime + ";base64, " + this.attachment.content;
        }
    },
    methods: {
        save() {
            // TODO
        }
    }
};
</script>

<style>
.mail-message-content-attachment-item .preview {
    height: 18vh;
}
</style>
