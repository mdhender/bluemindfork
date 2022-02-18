<template>
    <div v-if="message.composing" class="mail-attachments-header">
        <bm-icon icon="paper-clip" class="mr-1 ml-2" :class="paperClipColor" size="lg" />
        <span :class="isTooHeavy ? 'text-danger font-weight-bold' : ''">
            {{
                $tc("common.attachments", attachments.length, {
                    count: attachments.length
                })
            }}
            <span class="attachements-weigth">
                ({{ displaySize(attachmentsWeight) }} / {{ displaySize(attachmentsMaxWeight) }})</span
            >
            <bm-icon v-if="isTooHeavy" icon="exclamation-circle" />
        </span>
        <bm-progress
            :value="attachmentsWeight"
            :max="attachmentsMaxWeight"
            height="2px"
            class="pl-1 align-self-center"
            :variant="attachmentsWeightColor"
        />
    </div>
    <div v-else>
        <bm-icon icon="paper-clip" class="mr-1 ml-2" size="lg" />
        <span class="font-weight-bold pr-2">
            {{ $tc("common.attachments", attachments.length, { count: attachments.length }) }}
        </span>
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmIcon, BmProgress } from "@bluemind/styleguide";
import { displayWithUnit } from "@bluemind/file-utils";

export default {
    name: "MailAttachmentsHeader",
    components: { BmProgress, BmIcon },
    props: {
        attachments: {
            type: Array,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", { attachmentsMaxWeight: ({ messageCompose }) => messageCompose.maxMessageSize }),

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
    methods: {
        displaySize(size) {
            size = size < 100000 ? 100000 : size;
            return displayWithUnit(size, 6, this.$i18n);
        }
    }
};
</script>
