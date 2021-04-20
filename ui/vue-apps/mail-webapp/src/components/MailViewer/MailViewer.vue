<template>
    <section
        class="mail-viewer d-flex flex-column flex-grow-1 bg-surface overflow-auto"
        :aria-label="$t('mail.application.region.messagedetails')"
    >
        <bm-row class="px-lg-5 px-4 pt-2">
            <bm-col cols="12">
                <mail-viewer-toolbar class="d-none d-lg-flex" />
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5 px-4">
            <bm-col cols="12">
                <h1 class="subject">{{ subject }}</h1>
            </bm-col>
        </bm-row>
        <bm-row class="d-flex px-lg-5 px-4">
            <bm-col cols="8" class="d-flex">
                <mail-viewer-from :contact="message.from" />
            </bm-col>
            <bm-col cols="4" class="align-self-center text-right">
                {{ $d(message.date, "full_date_time_short") }}
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
                <event-viewer v-if="containsEvent" :message="message" />
                <parts-viewer v-else :message="message" />
            </bm-col>
        </bm-row>
        <mail-viewer-toolbar class="d-flex d-lg-none" />
    </section>
</template>

<script>
import { mapState, mapActions, mapGetters } from "vuex";
import { BmCol, BmRow } from "@bluemind/styleguide";
import { inject } from "@bluemind/inject";
import EventViewer from "./EventViewer";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailViewerFrom from "./MailViewerFrom";
import MailViewerRecipient from "./MailViewerRecipient";
import MailViewerToolbar from "./MailViewerToolbar";
import PartsViewer from "./PartsViewer/PartsViewer";

import { MESSAGE_LIST_UNREAD_FILTER_ENABLED } from "~getters";
import { MARK_MESSAGE_AS_READ } from "~actions";

export default {
    name: "MailViewer",
    components: {
        BmCol,
        BmRow,
        EventViewer,
        MailAttachmentsBlock,
        MailViewerFrom,
        MailViewerRecipient,
        MailViewerToolbar,
        PartsViewer
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        ...mapGetters("mail", { MESSAGE_LIST_UNREAD_FILTER_ENABLED }),
        ...mapState("mail", ["folders"]),
        subject() {
            return this.message.subject || this.$t("mail.viewer.no.subject");
        },
        containsEvent() {
            return inject("UserSession").roles.includes("hasCalendar") && this.message.hasICS;
        }
    },
    watch: {
        "message.key": {
            handler: function () {
                this.resetScroll();
                if (!this.MESSAGE_LIST_UNREAD_FILTER_ENABLED && this.folders[this.message.folderRef.key].writable) {
                    this.MARK_MESSAGE_AS_READ([this.message]);
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

    .subject {
        word-break: break-word;
    }
}
</style>
