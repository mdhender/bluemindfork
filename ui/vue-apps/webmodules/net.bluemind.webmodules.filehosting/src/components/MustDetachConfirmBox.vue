<template>
    <fh-confirm-box :files="files" class="fh-must-detach-confirm-box">
        <template #text>
            <i18n path="common.threshold.hit">
                <template v-slot:hit>
                    {{ $tc("filehosting.threshold.size", allFilesCount) }}
                </template>
                <template v-slot:size>
                    <strong class="font-weight-bold">{{ displaySize(sizeLimit) }}</strong>
                </template>
            </i18n>
            <br />
            {{ $tc("filehosting.share.start", files.length) }} ?
        </template>
    </fh-confirm-box>
</template>

<script>
import { computeUnit } from "@bluemind/file-utils";
import FilehostingL10N from "../l10n";
import FhConfirmBox from "./ConfirmBox";

export default {
    name: "FhMustDetachConfirmBox",
    components: { FhConfirmBox },
    componentI18N: { messages: FilehostingL10N },
    props: {
        files: {
            type: Array,
            required: true
        },
        sizeLimit: {
            type: Number,
            required: true
        },
        allFilesCount: {
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
@import "~@bluemind/ui-components/src/css/variables";

.fh-must-detach-confirm-box {
    .progress {
        background: $secondary;
    }
}
</style>
