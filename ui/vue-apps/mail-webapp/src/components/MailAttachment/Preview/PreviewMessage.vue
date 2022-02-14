<template>
    <mail-viewer-content class="preview-message" :message="message" :expand-attachments="true" />
</template>

<script>
import MailViewerContent from "../../MailViewer/MailViewerContent";

export default {
    name: "PreviewMessage",
    components: { MailViewerContent },
    props: {
        message: {
            type: Object,
            required: true
        }
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
    .mail-attachments-block {
        order: 0;
        & > .row {
            flex-direction: column;
            & > * {
                flex-basis: 100%;
                max-width: none;
            }
        }
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
}
</style>
