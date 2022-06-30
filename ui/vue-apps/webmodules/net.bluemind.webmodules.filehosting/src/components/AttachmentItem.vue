<template>
    <div class="fh-attachment-item">
        <div class="d-flex text-neutral">
            <div><bm-icon icon="file" size="2x" class="mt-2" /></div>
            <div class="d-flex ml-2 mt-2 justify-content-between flex-fill">
                <div class="label">
                    <h2 class="text-truncate">{{ attachment.fileName }}</h2>
                    <span class="text-neutral text-right">
                        <span v-if="!hasErrorStatus">
                            {{ displaySize(attachment.progress.loaded) }} / {{ displaySize(attachment.progress.total) }}
                        </span>
                    </span>
                    <bm-label-icon v-if="isLarge" icon="exclamation-circle-fill" class="text-warning">
                        {{ $t("mail.filehosting.very_large_file") }}
                    </bm-label-icon>
                </div>
                <div><slot name="item-actions" /></div>
            </div>
        </div>
        <bm-progress
            :value="attachment.progress.loaded"
            :max="attachment.progress.total"
            :variant="hasErrorStatus ? 'danger' : 'secondary'"
        />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { BmLabelIcon, BmProgress, BmIcon } from "@bluemind/styleguide";
import { computeUnit } from "@bluemind/file-utils";
import { attachmentUtils } from "@bluemind/mail";

const { AttachmentStatus } = attachmentUtils;

const VERY_LARGE_FILE_SIZE = 500 * 1024 * 1024;

export default {
    name: "FhAttachmentItem",
    components: { BmLabelIcon, BmProgress, BmIcon },
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
