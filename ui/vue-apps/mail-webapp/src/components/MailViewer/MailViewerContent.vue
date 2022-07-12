<template>
    <div class="mail-viewer-content">
        <h1 class="subject">{{ subject }}</h1>

        <bm-row class="sender">
            <bm-col class="from" cols="8">
                <mail-viewer-from :contact="message.from" />
            </bm-col>
            <bm-col cols="4" class="date">
                {{ $d(message.date, "full_date_time_short") }}
            </bm-col>
        </bm-row>
        <div v-if="hasRecipients" class="mail-sender-splitter"><hr /></div>

        <mail-viewer-recipients v-if="hasRecipients" :message="message" />
        <div class="mail-viewer-splitter pt-2"><hr /></div>
        <body-viewer :message="message" @remote-content="from => $emit('remote-content', from)">
            <template v-slot:attachments-block="scope">
                <slot name="attachments-block" v-bind="scope" />
            </template>

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
        }
    },
    computed: {
        subject() {
            return this.message.subject || this.$t("mail.viewer.no.subject");
        },
        hasRecipients() {
            return this.message.to.length || this.message.cc.length || this.message.bcc.length;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.mail-viewer-content {
    .body-viewer {
        .mail-inlines-block {
            padding: 0 $sp-4;
        }
        @include media-breakpoint-up(lg) {
            .mail-inlines-block {
                padding: 0;
            }
            padding: 0 $sp-5;
        }
    }

    .row {
        min-height: fit-content;
    }
    & > hr {
        &:last-of-type {
            border-top-color: $neutral-fg-lo2;
        }
        @include media-breakpoint-up(lg) {
            margin-left: $sp-5;
            margin-right: $sp-5;
        }
        margin-top: 0;
        margin-bottom: 0;
    }

    .subject {
        word-break: break-word;
    }
    .mail-viewer-recipients {
        margin-top: $sp-2;
    }

    .mail-viewer-recipients,
    .subject {
        padding: 0 $sp-4;
        @include media-breakpoint-up(lg) {
            padding: 0 $sp-5;
        }
    }

    .sender,
    .mail-sender-splitter,
    .mail-viewer-splitter {
        padding: 0 $sp-4;
        @include media-breakpoint-up(lg) {
            padding: 0 $sp-5;
        }
    }
    .mail-sender-splitter > hr {
        margin: $sp-2 0;
    }
    .mail-viewer-splitter > hr {
        border-color: $neutral-fg;
        margin: $sp-1 0 0 0;
    }

    .mail-viewer-splitter {
        padding-top: $sp-1;
    }

    .date {
        align-self: center;
        text-align: right;
        color: $neutral-fg;
    }
}
</style>
