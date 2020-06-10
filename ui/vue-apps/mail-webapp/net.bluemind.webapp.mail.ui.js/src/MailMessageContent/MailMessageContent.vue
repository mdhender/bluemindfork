<template>
    <div class="mail-message-content d-flex flex-column py-2 flex-grow-1 bg-surface">
        <bm-row class="px-lg-5 px-4">
            <bm-col cols="12">
                <mail-message-content-toolbar />
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5 px-4">
            <bm-col cols="12">
                <h1>{{ subject }}</h1>
            </bm-col>
        </bm-row>
        <bm-row class="d-flex px-lg-5 px-4">
            <bm-col cols="8" class="d-flex">
                <mail-message-content-from :dn="message.from.dn" :address="message.from.address" />
            </bm-col>
            <bm-col cols="4" class="align-self-center text-right">
                {{ $d(message.date, "full_date") }}
                {{ $t("mail.content.date.at") }}
                {{ $d(message.date, "short_time") }}
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5">
            <bm-col cols="12">
                <hr class="my-2" />
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5 px-4">
            <bm-col cols="12">
                <mail-message-content-recipient v-if="to" :recipients="to">
                    {{ $t("mail.content.to") }}
                </mail-message-content-recipient>
            </bm-col>
        </bm-row>
        <bm-row class="pb-2 px-lg-5 px-4">
            <bm-col cols="12">
                <mail-message-content-recipient v-if="cc" :recipients="cc">
                    {{ $t("mail.content.copy") }}
                </mail-message-content-recipient>
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5">
            <bm-col cols="12">
                <hr class="bg-dark my-0" />
                <mail-attachments-block :attachments="parts.attachments" />
            </bm-col>
        </bm-row>
        <bm-row ref="scrollableContainer" class="pt-1 flex-fill px-lg-5 px-4">
            <bm-col col>
                <parts-viewer />
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { mapState, mapActions, mapGetters } from "vuex";
import { BmCol, BmRow } from "@bluemind/styleguide";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailMessageContentFrom from "./MailMessageContentFrom";
import MailMessageContentRecipient from "./MailMessageContentRecipient";
import MailMessageContentToolbar from "./MailMessageContentToolbar";
import PartsViewer from "./PartsViewer/PartsViewer";

export default {
    name: "MailMessageContent",
    components: {
        BmCol,
        BmRow,
        MailAttachmentsBlock,
        MailMessageContentFrom,
        MailMessageContentRecipient,
        MailMessageContentToolbar,
        PartsViewer
    },
    computed: {
        ...mapGetters("mail-webapp/currentMessage", ["message"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key", parts: "parts" }),
        to() {
            if (this.message.to.length > 0) {
                return this.message.to.map(dest => (dest.dn ? dest.dn : dest.address));
            }
            return "";
        },
        cc() {
            if (this.message.cc.length > 0) {
                return this.message.cc.map(dest => (dest.dn ? dest.dn : dest.address));
            }
            return "";
        },
        subject() {
            return this.message.subject || "(No subject)"; // FIXME i18n
        }
    },
    watch: {
        currentMessageKey: {
            handler: function() {
                this.resetScroll();
                this.markAsRead([this.currentMessageKey]);
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead"]),
        resetScroll() {
            this.$nextTick(() => {
                this.$refs.scrollableContainer.scrollTop = 0;
                this.$refs.scrollableContainer.scrollLeft = 0;
            });
        },
        saveAttachments() {
            // not implemented yet
        }
    }
};
</script>

<style scoped>
.mail-message-content {
    z-index: 20;
}
</style>
