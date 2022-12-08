<template>
    <div class="mail-viewer-content">
        <div class="title">{{ subject }}</div>

        <div class="sender-and-recipients">
            <div class="sender">
                <mail-viewer-from :contact="message.from" />
                <div class="date">
                    {{ $d(message.date, "full_date_time_short") }}
                </div>
            </div>
            <mail-viewer-recipients v-if="hasRecipients" :message="message" />
        </div>
        <body-viewer class="flex-fill" :message="message" @remote-content="from => $emit('remote-content', from)">
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
import BodyViewer from "./BodyViewer";
import MailViewerFrom from "./MailViewerFrom";
import MailViewerRecipients from "./MailViewerRecipients";

export default {
    name: "MailViewerContent",
    components: {
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
@import "~@bluemind/ui-components/src/css/variables";
@import "./_variables";

.mail-viewer-content {
    .title {
        margin-bottom: $sp-5;
        word-break: break-word;
    }

    .sender {
        display: flex;
        align-items: center;

        .bm-contact {
            flex: 1;
            .contact-main-part {
                margin-left: $single-mail-avatar-main-gap !important;
            }
        }
        .date {
            flex: none;
            color: $neutral-fg;
        }
    }

    .mail-viewer-recipients {
        margin-top: $sp-2;
        padding-left: $avatar-width + $single-mail-avatar-main-gap;
    }

    .sender-and-recipients {
        margin-bottom: $sp-5;
    }
}
</style>
