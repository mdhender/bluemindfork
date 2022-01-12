<template>
    <div class="mail-viewer-content">
        <bm-row class="px-lg-5 px-4">
            <bm-col cols="12">
                <h1 class="subject">{{ subject }}</h1>
            </bm-col>
        </bm-row>
        <bm-row class="d-flex px-lg-5 px-4">
            <bm-col cols="8" class="d-flex">
                <mail-viewer-from :contact="message.from" />
            </bm-col>
            <bm-col cols="4" class="align-self-center text-right text-secondary">
                {{ $d(message.date, "full_date_time_short") }}
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5">
            <bm-col cols="12">
                <hr class="my-2" />
            </bm-col>
        </bm-row>
        <mail-viewer-recipients :message="message" class="px-lg-5 px-4" />
        <bm-row class="px-lg-5">
            <bm-col cols="12">
                <hr class="mail-viewer-splitter my-0" />
                <mail-attachments-block v-if="message.attachments.length > 0" :message="message" />
            </bm-col>
        </bm-row>
        <bm-row ref="scrollableContainer" class="pt-1 flex-fill px-lg-5 px-4">
            <bm-col col>
                <body-viewer :message="message" />
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { BmCol, BmRow } from "@bluemind/styleguide";

import BodyViewer from "./BodyViewer";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailViewerFrom from "./MailViewerFrom";
import MailViewerRecipients from "./MailViewerRecipients";

export default {
    name: "MailViewerContent",
    components: {
        BmCol,
        BmRow,
        BodyViewer,
        MailAttachmentsBlock,
        MailViewerFrom,
        MailViewerRecipients
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        subject() {
            return this.message.subject || this.$t("mail.viewer.no.subject");
        }
    },
    watch: {
        "message.key": {
            handler: function () {
                this.resetScroll();
            }
        }
    },
    methods: {
        resetScroll() {
            this.$nextTick(() => {
                this.$refs.scrollableContainer.scrollTop = 0;
                this.$refs.scrollableContainer.scrollLeft = 0;
            });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.mail-viewer-content {
    .row {
        min-height: fit-content;
    }

    .mail-viewer-splitter {
        border-top-color: $alternate-light;
    }

    .subject {
        word-break: break-word;
    }
}
</style>
