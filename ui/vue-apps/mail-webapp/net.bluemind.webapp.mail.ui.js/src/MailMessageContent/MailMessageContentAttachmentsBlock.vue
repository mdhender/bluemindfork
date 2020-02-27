<template>
    <bm-container v-if="hasAttachments" class="mail-message-content-attachments-block p-2 bg-extra-light">
        <bm-row>
            <bm-col cols="12" class="pl-2 d-flex">
                <button
                    v-bm-tooltip.ds500
                    class="btn p-0 bg-transparent border-0 caret-btn align-text-bottom"
                    :aria-label="$t('common.toggleAttachments')"
                    :title="$t('common.toggleAttachments')"
                    @click.prevent="toggleExpand"
                >
                    <bm-icon :icon="isExpanded ? 'caret-down' : 'caret-right'" />
                </button>
                <template v-if="editable">
                    <bm-icon icon="paper-clip" class="mx-1" :class="paperClipColor" size="lg" />
                    <span :class="isTooHeavy ? 'text-danger font-weight-bold' : ''">
                        {{
                            $tc("common.attachments", attachments.length, {
                                count: attachments.length
                            })
                        }}
                        ({{ displaySize(attachmentsWeight) }} / {{ displaySize(attachmentsMaxWeight) }})
                        <bm-icon v-if="isTooHeavy" icon="exclamation-circle" />
                    </span>
                    <bm-progress
                        :value="attachmentsWeight"
                        :max="attachmentsMaxWeight"
                        height="2px"
                        class="flex-fill d-flex pl-1 align-self-center"
                        :variant="attachmentsWeightColor"
                    />
                </template>
                <template v-else>
                    <bm-icon icon="paper-clip" class="mx-1" size="lg" />
                    <span class="font-weight-bold pr-2">
                        {{ $tc("common.attachments", attachments.length, { count: attachments.length }) }}
                    </span>
                </template>
            </bm-col>
        </bm-row>
        <bm-row v-if="seeMoreAttachments" class="ml-3 mr-1">
            <bm-col cols="4">
                <mail-message-content-attachment-item
                    :attachment="attachments[0]"
                    :is-expanded="isExpanded"
                    :is-removable="editable"
                    :is-downloadable="!editable"
                    @save="save(0)"
                    @remove="removeAttachment(attachments[0].uid)"
                />
            </bm-col>
            <bm-col cols="4">
                <mail-message-content-attachment-item
                    :attachment="attachments[1]"
                    :is-expanded="isExpanded"
                    :is-removable="editable"
                    :is-downloadable="!editable"
                    @save="save(1)"
                    @remove="removeAttachment(attachments[1].uid)"
                />
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
                    +
                    {{
                        $tc("common.attachments", attachments.length - 2, {
                            count: attachments.length - 2
                        })
                    }}
                </bm-button>
            </bm-col>
        </bm-row>
        <bm-row v-else class="ml-3 mr-1">
            <bm-col v-for="(attachment, index) in attachments" :key="attachment.address" cols="4">
                <mail-message-content-attachment-item
                    :attachment="attachment"
                    :is-expanded="isExpanded"
                    :is-removable="editable"
                    :is-downloadable="!editable"
                    @save="save(index)"
                    @remove="removeAttachment(attachment.uid)"
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
        </bm-button>-->
    </bm-container>
</template>

<script>
import { BmButton, BmCol, BmContainer, BmIcon, BmRow, BmTooltip, BmProgress } from "@bluemind/styleguide";
import { displayWithUnit } from "@bluemind/file-utils";
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
        BmProgress,
        BmRow,
        MailMessageContentAttachmentItem
    },
    directives: { BmTooltip },
    props: {
        attachments: {
            type: Array,
            default: () => []
        },
        editable: {
            type: Boolean,
            required: false,
            default: false
        },
        expanded: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            isExpanded: this.expanded,
            attachmentsContentFetched: false,
            downloadAttachmentFilename: "",
            downloadAttachmentBlob: ""
        };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail-webapp", { attachmentsMaxWeight: "maxMessageSize" }),
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
        },
        attachmentsWeight() {
            return this.attachments.map(attachment => attachment.size).reduce((total, size) => total + size, 0);
        },
        attachmentsWeightInPercent() {
            return (this.attachmentsWeight * 100) / this.attachmentsMaxWeight;
        },
        attachmentsWeightColor() {
            let color = "primary";
            if (this.attachmentsWeightInPercent > 100) {
                color = "danger";
            } else if (this.isHeavy) {
                color = "warning";
            }
            return color;
        },
        paperClipColor() {
            if (this.isTooHeavy) {
                return "text-danger";
            } else if (this.isHeavy) {
                return "text-warning";
            }
            return "";
        },
        isTooHeavy() {
            return this.attachmentsWeight > this.attachmentsMaxWeight;
        },
        isHeavy() {
            return this.attachmentsWeightInPercent > 50 && this.attachmentsWeightInPercent <= 100;
        }
    },
    watch: {
        currentMessageKey() {
            this.isExpanded = this.expanded;
            this.attachmentsContentFetched = false;
        }
    },
    methods: {
        ...mapActions("mail-webapp/messages", ["fetch"]),
        ...mapActions("mail-webapp", ["removeAttachment"]),
        ...mapGetters("mail-webapp/messages", ["getPartContent"]),
        toggleExpand() {
            if (!this.isExpanded && this.hasAnyAttachmentWithPreview && !this.attachmentsContentFetched) {
                let promises = this.attachments
                    .filter(a => MimeType.previewAvailable(a.mime))
                    .map(attachment => this.loadContentIfMissing(attachment));
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
            const attachment = this.attachments[index];
            // attachment content may be already fetched (if its preview has been displayed)
            if (attachment.content !== undefined) {
                this.triggerDownload(index);
            } else {
                this.loadContentIfMissing(attachment).then(() => this.triggerDownload(index));
            }
        },
        triggerDownload(index) {
            const attachment = this.attachments[index];

            this.downloadAttachmentFilename = attachment.filename;
            this.downloadAttachmentBlob = URL.createObjectURL(attachment.content);

            this.$nextTick(() => this.$refs["download-attachment-link"].click());
        },
        loadContentIfMissing(attachment) {
            if (!attachment.content) {
                // need to fetch content (for attachments where preview is available)
                // before render expanded mode
                return this.fetch({
                    messageKey: this.currentMessageKey,
                    part: attachment,
                    isAttachment: true
                }).then(() => {
                    attachment.content = this.getPartContent()(this.currentMessageKey, attachment.address);
                });
            } else {
                return Promise.resolve();
            }
        },
        displaySize(size) {
            return displayWithUnit(size, "Mo");
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
