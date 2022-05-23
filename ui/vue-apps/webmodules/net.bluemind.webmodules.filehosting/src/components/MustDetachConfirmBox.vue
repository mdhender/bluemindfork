<template>
    <fh-confirm-box :attachments="attachments" class="fh-must-detach-confirm-box">
        <template #text>
            <i18n path="mail.filehosting.threshold.hit">
                <template v-slot:hit>
                    {{ $tc("mail.filehosting.threshold.size", allAttachmentsCount) }}
                </template>
                <template v-slot:size>
                    <strong class="font-weight-bold">{{ displaySize(sizeLimit) }}</strong>
                </template>
            </i18n>
            <br />
            {{ $tc("mail.filehosting.share.start", attachments.length) }} ?
        </template>
    </fh-confirm-box>
</template>

<script>
import { computeUnit } from "@bluemind/file-utils";
import FhConfirmBox from "./ConfirmBox";

export default {
    name: "FhMustDetachConfirmBox",
    components: { FhConfirmBox },
    props: {
        attachments: {
            type: Array,
            required: true
        },
        sizeLimit: {
            type: Number,
            required: true
        },
        allAttachmentsCount: {
            type: Number,
            required: true
        }
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        }
    }
};
</script>
<style lang="scss" scoped>
@import "@bluemind/styleguide/css/_variables.scss";

.fh-must-detach-confirm-box {
    .progress {
        background: $secondary;
    }
    .font-size-h1 {
        font-size: $h1-font-size;
    }
}
</style>
