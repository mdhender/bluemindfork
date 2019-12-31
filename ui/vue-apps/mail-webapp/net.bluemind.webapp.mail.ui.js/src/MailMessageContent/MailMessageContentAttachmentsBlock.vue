<template>
    <bm-container v-if="hasAttachments" class="mail-message-content-attachments-block p-2 bg-extra-light">
        <bm-row>
            <bm-col cols="12" class="pl-2">
                <button
                    v-bm-tooltip.ds500
                    class="btn p-0 bg-transparent border-0 caret-btn align-text-bottom"
                    :aria-label="$t('common.toggleAttachments')"
                    :title="$t('common.toggleAttachments')"
                >
                    <bm-icon :icon="isExpanded ? 'caret-down' : 'caret-right'" @click="toggleExpand" />
                </button>
                <bm-icon icon="paper-clip" class="mx-1" size="lg" />
                <span class="font-weight-bold pr-2">
                    {{ $tc("common.attachments", attachments.length, { count: attachments.length }) }}
                </span>
            </bm-col>
        </bm-row>
        <bm-row v-if="seeMoreAttachments" class="ml-3 mr-1">
            <bm-col cols="4">
                <mail-message-content-attachment-item :attachment="attachments[0]" @save="save(0)" />
            </bm-col>
            <bm-col cols="4">
                <mail-message-content-attachment-item :attachment="attachments[1]" @save="save(1)" />
            </bm-col>
            <bm-col cols="4" class="pt-2 border-transparent">
                <bm-button
                    v-bm-tooltip.ds500
                    variant="outline-secondary"
                    class="w-100 h-100 mail-message-content-attachments-block-toggle py-2"
                    :title="$t('common.toggleAttachments')"
                    :aria-label="$t('common.toggleAttachments')"
                    @click="toggleExpand"
                >
                    + {{ $tc("common.attachments", attachments.length - 2, { count: attachments.length - 2 }) }}
                </bm-button>
            </bm-col>
        </bm-row>
        <bm-row v-else class="ml-3 mr-1">
            <bm-col v-for="(attachment, index) in attachments" :key="attachment.address" cols="4">
                <mail-message-content-attachment-item
                    :attachment="attachment"
                    :is-expanded="isExpanded"
                    @save="save(index)"
                />
            </bm-col>
        </bm-row>
        <a 
            ref="download-attachment-link"
            class="d-none"
            :download="downloadAttachmentFilename"
            :href="downloadAttachmentBlob"
        />
        <!-- Save all button with i18n, please dont delete it 
            <bm-button
            variant="outline-secondary"
            class="mr-2 align-self-center"
            size="sm"
            @click="$emit('saveAllAttachments')"
        >
            {{ $t("common.save_all") }}
        </bm-button> -->
    </bm-container>
</template>

<script>
import { BmButton, BmCol, BmContainer, BmIcon, BmRow, BmTooltip } from "@bluemind/styleguide";
import { mapActions, mapState, mapGetters } from "vuex";
import { MimeType } from "@bluemind/email";
import MailMessageContentAttachmentItem from "./MailMessageContentAttachmentItem";

export default {
    name: "MailMessageContentAttachmentsBlock",
    components: {
        BmButton,
        BmCol,
        BmContainer,
        BmIcon,
        BmRow,
        MailMessageContentAttachmentItem
    },
    directives: { BmTooltip },
    data() {
        return {
            isExpanded: false,
            attachmentsContentFetched: false,
            downloadAttachmentFilename: "",
            downloadAttachmentBlob: ""
        };
    },
    computed: {
        ...mapGetters("mail-webapp", { attachments: "currentMessageAttachments" }),
        ...mapState("mail-webapp", ["currentMessageKey"]),
        hasAttachments() {
            return this.attachments.length > 0;
        },
        hasMoreThan3Attachments() {
            return this.attachments.length > 3;
        },
        seeMoreAttachments() {
            return !this.isExpanded && this.hasMoreThan3Attachments;
        },
        hasAnyAttachmentWithPreview() {
            return this.attachments.some(a => this.hasPreview(a));
        }
    },
    watch: {
        currentMessageKey() {
            this.isExpanded = false;
            this.attachmentsContentFetched = false;
        }
    },
    methods: {
        ...mapActions("mail-webapp/messages", ["fetch"]),
        toggleExpand() {
            if (!this.isExpanded && this.hasAnyAttachmentWithPreview && !this.attachmentsContentFetched) {
                let promises = this.attachments
                    .filter(a => MimeType.previewAvailable(a.mime))
                    .map(attachment =>
                        // need to fetch content (for attachments where preview is available)
                        // before render expanded mode
                        this.fetch({
                            messageKey: this.currentMessageKey,
                            part: attachment,
                            isAttachment: true
                        })
                    );
                Promise.all(promises)
                    .then(() => {
                        this.attachmentsContentFetched = true;
                        this.isExpanded = !this.isExpanded;
                    })
                    .catch(() => console.error("fail to fetch attachment content"));
            } else {
                this.isExpanded = !this.isExpanded;
            }
        },
        hasPreview(attachment) {
            return MimeType.previewAvailable(attachment.mime);
        },
        save(index) {
            //FIXME
            const attachment = this.attachments[index];
            // attachment content may be already fetched (if its preview has been displayed)
            if (attachment.content !== undefined) {
                this.triggerDownload(index);
            } else {
                this.fetch({
                    messageKey: this.currentMessageKey,
                    part: attachment,
                    isAttachment: true
                }).then(() => this.triggerDownload(index));
            }
        },
        triggerDownload(index) {
            const attachment = this.attachments[index];
            const attachmentBlob = new Blob([attachment.content], { type : attachment.mime });
            
            this.downloadAttachmentFilename = attachment.filename;
            this.downloadAttachmentBlob = URL.createObjectURL(attachmentBlob);

            this.$nextTick(() => this.$refs["download-attachment-link"].click());
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-message-content-attachments-block .col-4 {
    padding-right: $sp-1 !important;
    padding-left: $sp-1 !important;
}

.mail-message-content-attachments-block .caret-btn:focus {
    box-shadow: unset;
}

.border-transparent {
    border: 1px solid transparent !important;
}
</style>
