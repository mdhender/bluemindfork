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
        <hr class="mail-viewer-splitter my-0 mx-lg-5" />
        <body-viewer :message="message" :expand-attachments="expandAttachments" :expand-quotes="expandQuotes">
            <template v-for="(_, slot) of $scopedSlots" v-slot:[slot]="scope">
                <slot :name="slot" v-bind="scope" />
            </template>
        </body-viewer>
    </div>
</template>
<script>
import { BmCol, BmRow } from "@bluemind/styleguide";

import BodyViewer from "./BodyViewer";
import MailViewerFrom from "./MailViewerFrom";
import MailViewerRecipients from "./MailViewerRecipients";

export default {
    name: "MailViewerContent",
    components: {
        BmCol,
        BmRow,
        BodyViewer,
        MailViewerFrom,
        MailViewerRecipients
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        expandAttachments: {
            type: Boolean,
            default: false
        },
        expandQuotes: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        subject() {
            return this.message.subject || this.$t("mail.viewer.no.subject");
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.mail-viewer-content {
    .body-viewer {
        @include media-breakpoint-up(lg) {
            & > * {
                padding: 0 $sp-5;
            }
        }
        .mail-inlines-block {
            padding: 0 $sp-4;
        }
    }

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
