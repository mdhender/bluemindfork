<template>
    <bm-container class="mail-attachments-block p-2 bg-extra-light">
        <div class="d-flex align-items-center">
            <bm-button
                variant="inline-dark"
                :aria-label="$t('common.toggleAttachments')"
                :title="$t('common.toggleAttachments')"
                @click.prevent="toggleExpand"
            >
                <bm-icon :icon="isExpanded ? 'caret-down' : 'caret-right'" />
            </bm-button>
            <template v-if="message.composing">
                <bm-icon icon="paper-clip" class="mr-1 ml-2" :class="paperClipColor" size="lg" />
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
                <bm-icon icon="paper-clip" class="mr-1 ml-2" size="lg" />
                <span class="font-weight-bold pr-2">
                    {{ $tc("common.attachments", attachments.length, { count: attachments.length }) }}
                </span>
            </template>
        </div>
        <bm-row v-if="seeMoreAttachments" class="ml-3 mr-1">
            <bm-col lg="4" cols="12">
                <mail-attachment-item :attachment="attachments[0]" :message="message" :compact="true" />
            </bm-col>
            <bm-col lg="4" cols="12">
                <mail-attachment-item :attachment="attachments[1]" :message="message" :compact="true" />
            </bm-col>
            <bm-col lg="4" cols="12" class="pt-2 border-transparent">
                <bm-button
                    variant="outline-secondary"
                    class="w-100 h-100 py-2"
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
            <bm-col v-for="attachment in attachments" :key="attachment.address" lg="4" cols="12" :compact="!isExpanded">
                <mail-attachment-item :attachment="attachment" :message="message" :compact="!isExpanded" />
            </bm-col>
        </bm-row>
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
import { mapMutations, mapState } from "vuex";

import { displayWithUnit } from "@bluemind/file-utils";
import { BmButton, BmCol, BmContainer, BmIcon, BmRow, BmProgress } from "@bluemind/styleguide";

import MailAttachmentItem from "./MailAttachmentItem";
import { SET_ATTACHMENT_CONTENT_URL } from "~mutations";

export default {
    name: "MailAttachmentsBlock",
    components: {
        BmButton,
        BmCol,
        BmContainer,
        BmIcon,
        BmProgress,
        BmRow,
        MailAttachmentItem
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        expanded: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return { isExpanded: this.expanded };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail-webapp", { attachmentsMaxWeight: "maxMessageSize" }),
        attachments() {
            return this.message.attachments;
        },
        hasMoreThan3Attachments() {
            return this.attachments.length > 3;
        },
        seeMoreAttachments() {
            return !this.isExpanded && this.hasMoreThan3Attachments;
        },
        attachmentsWeight() {
            return this.attachments.map(attachment => attachment.size).reduce((total, size) => total + size, 0);
        },
        attachmentsWeightInPercent() {
            return (this.attachmentsWeight * 100) / this.attachmentsMaxWeight;
        },
        attachmentsWeightColor() {
            let color = "success";
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
        }
    },
    destroyed() {
        this.attachments.forEach(attachment => {
            if (attachment.contentUrl) {
                URL.revokeObjectURL(attachment.contentUrl);
                this.SET_ATTACHMENT_CONTENT_URL({
                    messageKey: this.message.key,
                    address: attachment.address,
                    url: null
                });
            }
        });
    },
    methods: {
        ...mapMutations("mail", { SET_ATTACHMENT_CONTENT_URL }),
        async toggleExpand() {
            this.isExpanded = !this.isExpanded;
        },
        displaySize(size) {
            size = size < 100000 ? 100000 : size;
            return displayWithUnit(size, "Mo");
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-attachments-block .col-4,
.mail-attachments-block .col-lg-4 {
    padding-right: $sp-1 !important;
    padding-left: $sp-1 !important;
}

.border-transparent {
    border: 1px solid transparent !important;
}
</style>
