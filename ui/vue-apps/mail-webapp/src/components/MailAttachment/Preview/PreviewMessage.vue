<template>
    <mail-viewer-content class="preview-message" :message="message">
        <template v-slot:attachments-block="scope">
            <div class="mail-attachments">
                <mail-attachments-header :attachments="scope.attachments" :message="scope.message" />
                <mail-attachment-item
                    v-for="attachment in scope.attachments"
                    :key="attachment.address"
                    :attachment="attachment"
                    :message="scope.message"
                    :compact="!isViewable(attachment)"
                    :class="attachment.address === activePart.address ? 'active' : ''"
                />
            </div>
        </template>
    </mail-viewer-content>
</template>

<script>
import { isViewable } from "~/model/part";
import MailViewerContent from "../../MailViewer/MailViewerContent";
import MailAttachmentItem from "../MailAttachmentItem";
import MailAttachmentsHeader from "../MailAttachmentsHeader";

export default {
    name: "PreviewMessage",
    components: { MailViewerContent, MailAttachmentItem, MailAttachmentsHeader },
    props: {
        message: {
            type: Object,
            required: true
        },
        activePart: {
            type: Object,
            required: true
        }
    },
    methods: {
        isViewable
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.preview-message.mail-viewer-content {
    padding-top: $sp-4;
    display: flex;
    flex-direction: column;
    .from {
        display: none;
    }
    * .date {
        text-align: left;
        white-space: nowrap;
        font-weight: $font-weight-bold;
    }
    .mail-viewer-splitter {
        order: 1;
    }

    .mail-inlines-block {
        padding: 0 $sp-4;
    }
    .mail-sender-splitter {
        display: none;
    }
    .mail-viewer-recipients {
        display: none;
    }
    .sender,
    .mail-viewer-recipients {
        order: 0;
        margin: 0;
    }
    .body-viewer {
        order: 2;
        padding: 0;
    }
    .sender,
    .mail-viewer-recipients,
    .subject {
        padding-left: $sp-4;
        padding-right: $sp-4;
    }
    & > hr {
        margin: $sp-3 0 0 0;
    }
    .bm-contact .address {
        display: none;
    }
    .mail-viewer-recipient {
        flex-wrap: nowrap;
        white-space: nowrap;
    }
    .mail-attachments {
        padding: $sp-2 $sp-4;
        background-color: $light;
        .mail-attachment-item {
            border-width: 2px !important;
        }

        .active .mail-attachment-item {
            border-color: $primary !important;
        }
    }
}
</style>
