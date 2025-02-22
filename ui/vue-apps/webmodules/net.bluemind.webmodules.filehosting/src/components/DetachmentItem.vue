<template>
    <div class="fh-file-item">
        <div class="d-flex text-neutral">
            <div><bm-icon icon="file" class="mt-4" /></div>
            <div class="d-flex ml-3 mt-4 justify-content-between flex-fill">
                <div class="label">
                    <div class="text-truncate bold">{{ file.name }}</div>
                    <span class="text-neutral text-right">
                        <span v-if="!hasErrorStatus">
                            {{ displaySize(progress.loaded) }} / {{ displaySize(progress.total) }}
                        </span>
                    </span>
                    <bm-label-icon v-if="isLarge" icon="exclamation-circle-fill" class="text-warning">
                        {{ $t("filehosting.very_large_file") }}
                    </bm-label-icon>
                </div>
                <slot name="item-actions" />
            </div>
        </div>
        <bm-progress
            :value="progress.loaded"
            :max="progress.total"
            :variant="hasErrorStatus ? 'danger' : 'secondary'"
        />
    </div>
</template>

<script>
import { BmLabelIcon, BmProgress, BmIcon } from "@bluemind/ui-components";
import { computeUnit } from "@bluemind/file-utils";
import { fileUtils } from "@bluemind/mail";

const { FileStatus, VERY_LARGE_FILE_SIZE } = fileUtils;

export default {
    name: "DetachmentItem",
    components: { BmLabelIcon, BmProgress, BmIcon },
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        isLarge() {
            return this.progress.total > VERY_LARGE_FILE_SIZE;
        },
        hasErrorStatus() {
            return this.file.status === FileStatus.ERROR;
        },
        progress() {
            return this.file.progress || {};
        }
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.fh-file-item {
    .progress {
        position: absolute;
        margin-bottom: $sp-2;
        top: 0;
        height: 0.125rem;
        width: 100%;
    }
    .label {
        min-width: 0;
    }
}
</style>
