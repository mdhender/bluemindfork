<template>
    <div class="fh-attachment-item">
        <div class="d-flex justify-content-between align-items-center">
            <bm-label-icon icon="file" icon-size="2x" class="mt-2 label">
                <h2 class="text-neutral text-truncate">{{ attachment.fileName }}</h2>
            </bm-label-icon>
            <slot name="item-actions" />
        </div>
        <span class="text-neutral ml-4 text-right">
            <span v-if="!hasErrorStatus">
                {{ displaySize(attachment.progress.loaded) }} / {{ displaySize(attachment.progress.total) }}
            </span>
        </span>
        <bm-label-icon v-if="isLarge" icon="exclamation-circle-fill" class="text-warning">
            {{ $t("mail.filehosting.very_large_file") }}
        </bm-label-icon>
        <bm-progress
            :value="attachment.progress.loaded"
            :max="attachment.progress.total"
            :variant="hasErrorStatus ? 'danger' : 'secondary'"
        />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { BmLabelIcon, BmProgress } from "@bluemind/styleguide";
import { computeUnit } from "@bluemind/file-utils";
import { attachment } from "@bluemind/mail";

const { AttachmentStatus } = attachment;

const VERY_LARGE_FILE_SIZE = 500 * 1024 * 1024;

export default {
    name: "FhAttachmentItem",
    components: { BmLabelIcon, BmProgress },
    props: {
        attachment: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", ["GET_FH_ATTACHMENT"]),
        isLarge() {
            return this.attachment.progress.total > VERY_LARGE_FILE_SIZE;
        },
        hasErrorStatus() {
            return this.attachment.status === AttachmentStatus.ERROR;
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
@import "@bluemind/styleguide/css/_variables.scss";

.fh-attachment-item {
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
