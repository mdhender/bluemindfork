<template>
    <section
        class="mail-viewer d-flex flex-column flex-grow-1 bg-surface"
        :aria-label="$t('mail.application.region.messagedetails')"
    >
        <mail-component-alert
            v-if="message.hasICS && !currentEvent && !isIcsAlertBlocked"
            icon="exclamation-circle"
            @close="isIcsAlertBlocked = true"
        >
            {{ $t("mail.content.alert.ics.dont_exist") }}
        </mail-component-alert>
        <bm-row class="px-lg-5 px-4 pt-2">
            <bm-col cols="12">
                <mail-viewer-toolbar class="d-none d-lg-flex" />
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5 px-4">
            <bm-col cols="12">
                <h1>{{ subject }}</h1>
            </bm-col>
        </bm-row>
        <bm-row class="d-flex px-lg-5 px-4">
            <bm-col cols="8" class="d-flex">
                <mail-viewer-from :contact="message.from" />
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
                <mail-viewer-recipient v-if="message.to.length > 0" :recipients="message.to"
                    >{{ $t("mail.content.to") }}
                </mail-viewer-recipient>
            </bm-col>
        </bm-row>
        <bm-row class="pb-2 px-lg-5 px-4">
            <bm-col cols="12">
                <mail-viewer-recipient v-if="message.cc.length > 0" :recipients="message.cc"
                    >{{ $t("mail.content.copy") }}
                </mail-viewer-recipient>
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5">
            <bm-col cols="12">
                <hr class="mail-viewer-splitter my-0" />
                <mail-attachments-block v-if="message.attachments.length > 0" :message="message" />
            </bm-col>
        </bm-row>
        <bm-row ref="scrollableContainer" class="pt-1 flex-fill px-lg-5 px-4">
            <bm-col col>
                <template v-if="currentEvent && message.eventInfo.needsReply">
                    <reply-to-counter-proposal v-if="message.eventInfo.isCounterEvent" />
                    <reply-to-invitation v-else />
                </template>
                <event-viewer v-if="message.hasICS && currentEvent" :current-event="currentEvent" :message="message" />
                <parts-viewer v-else :message-key="message.key" />
            </bm-col>
        </bm-row>
        <mail-viewer-toolbar class="d-flex d-lg-none" />
    </section>
</template>

<script>
import { mapState, mapActions, mapGetters } from "vuex";
import { BmCol, BmRow } from "@bluemind/styleguide";
import EventViewer from "./EventViewer.vue";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailComponentAlert from "../MailComponentAlert";
import MailViewerFrom from "./MailViewerFrom";
import MailViewerRecipient from "./MailViewerRecipient";
import MailViewerToolbar from "./MailViewerToolbar";
import PartsViewer from "./PartsViewer/PartsViewer";
import ReplyToCounterProposal from "./ReplyToCounterProposal";
import ReplyToInvitation from "./ReplyToInvitation";
import { MESSAGE_LIST_UNREAD_FILTER_ENABLED } from "~getters";
import { MARK_MESSAGE_AS_READ } from "~actions";

export default {
    name: "MailViewer",
    components: {
        BmCol,
        BmRow,
        EventViewer,
        MailAttachmentsBlock,
        MailComponentAlert,
        MailViewerFrom,
        MailViewerRecipient,
        MailViewerToolbar,
        PartsViewer,
        ReplyToInvitation,
        ReplyToCounterProposal
    },
    props: {
        messageKey: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            isIcsAlertBlocked: false
        };
    },
    computed: {
        ...mapGetters("mail", { MESSAGE_LIST_UNREAD_FILTER_ENABLED }),
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        ...mapState("mail", ["messages"]),
        subject() {
            return this.message.subject || this.$t("mail.viewer.no.subject");
        },
        message() {
            return this.messages[this.messageKey];
        }
    },
    watch: {
        messageKey: {
            handler: function () {
                this.resetScroll();
                // FIXME: remove this if once https://forge.bluemind.net/jira/browse/FEATWEBML-1017 is fixed
                if (!this.MESSAGE_LIST_UNREAD_FILTER_ENABLED) {
                    this.MARK_MESSAGE_AS_READ([this.message]);
                }
                if (this.isIcsAlertBlocked) {
                    this.isIcsAlertBlocked = false;
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", { MARK_MESSAGE_AS_READ }),
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

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.mail-viewer {
    z-index: 20;

    .row {
        min-height: fit-content;
    }

    .mail-viewer-splitter {
        border-top-color: $alternate-light;
    }
}
</style>
