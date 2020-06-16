<template>
    <div class="mail-viewer d-flex flex-column pb-2 flex-grow-1 bg-surface">
        <mail-component-alert
            v-if="!message.ics.isEmpty && !currentEvent && !isIcsAlertBlocked"
            icon="exclamation-circle"
            @close="isIcsAlertBlocked = true"
        >
            {{ $t("mail.content.alert.ics.dont_exist") }}
        </mail-component-alert>
        <bm-row class="px-lg-5 px-4 pt-2">
            <bm-col cols="12">
                <mail-viewer-toolbar />
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5 px-4">
            <bm-col cols="12">
                <h1>{{ subject }}</h1>
            </bm-col>
        </bm-row>
        <bm-row class="d-flex px-lg-5 px-4">
            <bm-col cols="8" class="d-flex">
                <mail-viewer-from :dn="message.from.dn" :address="message.from.address" />
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
                <mail-viewer-recipient v-if="to" :recipients="to">{{ $t("mail.content.to") }} </mail-viewer-recipient>
            </bm-col>
        </bm-row>
        <bm-row class="pb-2 px-lg-5 px-4">
            <bm-col cols="12">
                <mail-viewer-recipient v-if="cc" :recipients="cc">{{ $t("mail.content.copy") }} </mail-viewer-recipient>
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
                <ics-viewer v-if="!message.ics.isEmpty && currentEvent" />
                <parts-viewer v-else />
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { mapState, mapActions, mapGetters } from "vuex";
import { BmCol, BmRow } from "@bluemind/styleguide";
import IcsViewer from "./IcsViewer";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailComponentAlert from "../MailComponentAlert";
import MailViewerFrom from "./MailViewerFrom";
import MailViewerRecipient from "./MailViewerRecipient";
import MailViewerToolbar from "./MailViewerToolbar";
import PartsViewer from "./PartsViewer/PartsViewer";

export default {
    name: "MailViewer",
    components: {
        BmCol,
        BmRow,
        IcsViewer,
        MailAttachmentsBlock,
        MailComponentAlert,
        MailViewerFrom,
        MailViewerRecipient,
        MailViewerToolbar,
        PartsViewer
    },
    data() {
        return {
            isIcsAlertBlocked: false
        };
    },
    computed: {
        ...mapGetters("mail-webapp/currentMessage", ["message"]),
        ...mapState("mail-webapp", ["currentEvent"]),
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
                if (this.isIcsAlertBlocked) {
                    this.isIcsAlertBlocked = false;
                }
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
.mail-viewer {
    z-index: 20;
}
</style>
