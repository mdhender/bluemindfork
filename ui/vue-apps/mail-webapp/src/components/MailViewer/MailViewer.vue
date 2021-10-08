<template>
    <section class="mail-viewer d-flex flex-column flex-grow-1 bg-surface overflow-auto">
        <bm-extension id="webapp.mail" path="viewer.header" :message="message" />
        <bm-row class="px-lg-5 px-4 pt-2">
            <bm-col cols="12">
                <mail-viewer-toolbar
                    v-if="conversation"
                    class="d-none d-lg-flex"
                    :message="message"
                    :conversation="conversation"
                />
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
        <mail-viewer-toolbar
            v-if="conversation"
            class="d-flex d-lg-none"
            :message="message"
            :conversation="conversation"
        />
    </section>
</template>

<script>
import { mapState, mapActions, mapGetters } from "vuex";
import { BmExtension } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";
import BmRoles from "@bluemind/roles";
import BodyViewer from "./BodyViewer";
import { BmCol, BmRow } from "@bluemind/styleguide";

import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailViewerFrom from "./MailViewerFrom";
import MailViewerRecipients from "./MailViewerRecipients";
import MailViewerToolbar from "./MailViewerToolbar";
import { CONVERSATION_LIST_UNREAD_FILTER_ENABLED } from "~/getters";
import { MARK_MESSAGE_AS_READ } from "~/actions";

export default {
    name: "MailViewer",
    components: {
        BmCol,
        BmExtension,
        BmRow,
        BodyViewer,
        MailAttachmentsBlock,
        MailViewerFrom,
        MailViewerRecipients,
        MailViewerToolbar
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", {
            currentEvent: state => state.consultPanel.currentEvent,
            conversationByKey: ({ conversations }) => conversations.conversationByKey
        }),
        ...mapGetters("mail", { CONVERSATION_LIST_UNREAD_FILTER_ENABLED }),
        ...mapState("mail", ["folders"]),
        subject() {
            return this.message.subject || this.$t("mail.viewer.no.subject");
        },
        containsEvent() {
            return inject("UserSession").roles.includes(BmRoles.HAS_CALENDAR) && this.message.hasICS;
        },
        conversation() {
            return this.conversationByKey[this.message.conversationRef?.key];
        }
    },
    watch: {
        "message.key": {
            handler: function () {
                this.resetScroll();
                if (
                    !this.CONVERSATION_LIST_UNREAD_FILTER_ENABLED &&
                    this.folders[this.message.folderRef.key].writable
                ) {
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
