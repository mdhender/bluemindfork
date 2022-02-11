<template>
    <div class="preview-message">
        <preview-message-header :icon="icon" @click="toggle" />
        <mail-viewer-content v-if="!hidden" :message="message" :expand-attachments="true" />
    </div>
</template>

<script>
import MailViewerContent from "../../MailViewer/MailViewerContent";
import PreviewMessageHeader from "./PreviewMessageHeader";

export default {
    name: "PreviewMessage",
    components: { MailViewerContent, PreviewMessageHeader },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { hidden: false, icon: "chevron" };
    },
    methods: {
        toggle() {
            this.hidden = !this.hidden;
            this.icon = this.icon === "chevron" ? "chevron-up" : "chevron";
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.preview-message {
    overflow: auto;

    .mail-viewer-content {
        display: flex;
        flex-direction: column;
        .from {
            display: none;
        }
        * .date {
            text-align: left;
            white-space: nowrap;
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
}
</style>
