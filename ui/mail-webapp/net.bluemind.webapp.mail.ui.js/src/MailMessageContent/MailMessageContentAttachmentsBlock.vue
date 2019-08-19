<template>
    <bm-container v-if="hasAttachments" class="mail-message-content-attachments-block pt-2 pb-0 bg-extra-light">
        <bm-row class="mb-2">
            <bm-col cols="12" class="pl-2">
                <button class="btn p-0 bg-transparent border-0" :aria-label="$t('common.toggleAttachments')">
                    <bm-icon 
                        v-if="hasMoreThan3Attachments"
                        :icon="isExpanded ? 'caret-down' : 'caret-right'"
                        @click="toggleExpand"
                    />
                </button>
                <bm-icon icon="paper-clip" class="mr-1" size="lg" :class="hasMoreThan3Attachments ? 'ml-0': 'ml-3'" />
                <span class="font-weight-bold pr-2">
                    {{ $tc("common.attachments", attachments.length, { count: attachments.length }) }}
                </span> 
            </bm-col>
        </bm-row>
        <bm-row v-if="shouldProposeExpandedMode" class="ml-3 mr-1">
            <bm-col cols="4">
                <mail-message-content-attachment-item :attachment="attachments[0]" />
            </bm-col>
            <bm-col cols="4">
                <mail-message-content-attachment-item :attachment="attachments[1]" />
            </bm-col>
            <bm-col cols="4" class="mb-2">
                <bm-row class="border border-transparent">
                    <bm-button 
                        variant="outline-secondary"
                        class="w-100 mail-message-content-attachments-block-toggle py-2"
                        :class="hasPreview(attachments[0]) || hasPreview(attachments[1]) ? '' : 'h-100'"
                        :aria-label="$t('common.toggleAttachments')"
                        @click="toggleExpand"
                    >
                        + {{ $tc("common.attachments", attachments.length - 2, { count: attachments.length - 2 }) }}
                    </bm-button>
                </bm-row>
            </bm-col>
        </bm-row>
        <bm-row v-else class="ml-3 mr-1">
            <bm-col
                v-for="attachment in attachments"
                :key="attachment.address"
                cols="4"
                class=""
            >
                <mail-message-content-attachment-item :attachment="attachment" />
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
        </bm-button> -->
    </bm-container>
</template>

<script>
import { BmButton, BmCol, BmContainer, BmIcon, BmRow } from "@bluemind/styleguide";
import MailMessageContentAttachmentItem from "./MailMessageContentAttachmentItem";
import { MimeType } from "@bluemind/email";

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
    props: {
        attachments: {
            type: Array,
            required: true
        }
    },
    data() {
        return {
            isExpanded: false
        };
    },
    computed: {
        hasAttachments() {
            return this.attachments.length > 0;
        },
        hasMoreThan3Attachments() {
            return this.attachments.length > 3;
        },
        shouldProposeExpandedMode() {
            return !this.isExpanded && this.hasMoreThan3Attachments;
        }
    },
    watch: {
        attachments() {
            this.isExpanded = false;
        }
    },
    methods: {
        toggleExpand() {
            this.isExpanded = !this.isExpanded;
        },
        hasPreview(attachment) {
            return MimeType.previewAvailable(attachment.mime);
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-message-content-attachments-block .col-4 {
    padding-right: map-get($spacers, 1) !important;
    padding-left: map-get($spacers, 1) !important;
}

.mail-message-content-attachments-block-toggle {
    line-height: #{$line-height-base*2} !important;
}

.border-transparent {
    border-color: transparent;
}
</style>
