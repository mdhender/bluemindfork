<template>
    <div v-if="attachments.length > 0" class="mail-attachments-block p-2 bg-extra-light">
        <div class="d-flex align-items-center">
            <bm-button
                variant="inline-dark"
                :aria-label="$t('common.toggleAttachments')"
                :title="$t('common.toggleAttachments')"
                @click.prevent="toggleExpand"
            >
                <bm-icon :icon="isExpanded ? 'caret-down' : 'caret-right'" />
            </bm-button>
            <mail-attachments-header :attachments="attachments" :message="message" />
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
    </div>
</template>

<script>
import { BmButton, BmCol, BmIcon, BmRow } from "@bluemind/styleguide";

import MailAttachmentItem from "./MailAttachmentItem";
import MailAttachmentsHeader from "./MailAttachmentsHeader";

export default {
    name: "MailAttachmentsBlock",
    components: {
        BmButton,
        BmCol,
        BmIcon,
        BmRow,
        MailAttachmentItem,
        MailAttachmentsHeader
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        expanded: {
            type: Boolean,
            default: false
        },
        attachments: {
            type: Array,
            required: true
        }
    },
    data() {
        return { isExpanded: this.expanded };
    },
    computed: {
        hasMoreThan3Attachments() {
            return this.attachments.length > 3;
        },
        seeMoreAttachments() {
            return !this.isExpanded && this.hasMoreThan3Attachments;
        }
    },
    watch: {
        "message.key"() {
            this.isExpanded = this.expanded;
        }
    },
    methods: {
        async toggleExpand() {
            this.isExpanded = !this.isExpanded;
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
