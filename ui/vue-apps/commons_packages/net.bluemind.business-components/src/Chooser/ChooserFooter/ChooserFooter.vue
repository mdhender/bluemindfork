<template>
    <div class="chooser-footer">
        <div class="selected-files">
            {{ $tc("common.files.selected", filesCount, { count: filesCount }) }}
            <span v-if="filesCount > 0">({{ totalSizeUnits }})</span>
            <span v-if="isAboveDetachmentLimit" class="text-danger ml-1">
                <bm-icon icon="exclamation-circle-fill" class="mr-1" />
                {{ $t("chooser.too_heavy.link") }}
            </span>
            <span v-else-if="filesCount > 50" class="text-warning ml-1">
                <bm-icon icon="exclamation-circle-fill" class="mr-1" />
                {{ $t("chooser.many_files") }}
            </span>
        </div>

        <div class="toolbars">
            <chooser-option-toolbar
                :disabled-as-links="isAboveDetachmentLimit"
                :disabled-as-attachments="isAboveAutoDetachmentLimit || isTooHeavyAsAttachments"
                @insert-as-attachment="insertAsAttachment"
                @insert-as-link="insertAsLink"
            />
            <chooser-main-toolbar
                :disabled-insert="isAboveDetachmentLimit || filesCount < 1"
                @insert="$emit('insert')"
                @cancel="$emit('cancel')"
            />
        </div>
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmIcon } from "@bluemind/ui-components";
import { computeUnit } from "@bluemind/file-utils";
import { INSERT_AS_ATTACHMENT, INSERT_AS_LINK } from "../store/mutations";
import ChooserMainToolbar from "./ChooserMainToolbar/ChooserMainToolbar";
import ChooserOptionToolbar from "./ChooserOptionToolbar/ChooserOptionToolbar";

export default {
    name: "ChooserFooter",
    components: { ChooserMainToolbar, ChooserOptionToolbar, BmIcon },
    props: {
        maxAttachmentsSize: {
            type: Number,
            required: true
        }
    },
    data() {
        return {
            autoDetachmentLimit: null,
            detachmentSizeLimit: null
        };
    },
    computed: {
        ...mapState("chooser", ["selectedFiles"]),
        filesCount() {
            return this.selectedFiles.length;
        },
        totalSize() {
            return this.selectedFiles.reduce((totalSize, next) => totalSize + next.size, 0);
        },
        totalSizeUnits() {
            return computeUnit(this.totalSize, this.$i18n);
        },
        isAboveAutoDetachmentLimit() {
            return !!(
                this.autoDetachmentLimit && this.selectedFiles.some(({ size }) => size > this.autoDetachmentLimit)
            );
        },
        isTooHeavyAsAttachments() {
            return !!(this.totalSize > this.maxAttachmentsSize);
        },
        isAboveDetachmentLimit() {
            return !!(
                this.detachmentSizeLimit && this.selectedFiles.some(({ size }) => size > this.detachmentSizeLimit)
            );
        }
    },
    watch: {
        isTooHeavyAsAttachments() {
            this.insertAsLink();
        },
        isAboveAutoDetachmentLimit() {
            this.insertAsLink();
        }
    },
    async beforeCreate() {
        const { autoDetachmentLimit, maxFilesize } = await this.$store.dispatch(`mail/GET_CONFIGURATION`);
        this.autoDetachmentLimit = autoDetachmentLimit;
        this.detachmentSizeLimit = maxFilesize;
    },
    methods: {
        insertAsAttachment() {
            this.$store.commit(`chooser/${INSERT_AS_ATTACHMENT}`);
        },
        insertAsLink() {
            this.$store.commit(`chooser/${INSERT_AS_LINK}`);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.chooser-footer {
    width: 100%;

    .selected-files {
        padding-bottom: $sp-5;
        text-align: end;
        color: $neutral-fg;
    }

    .toolbars {
        display: flex;
        flex: 1 1 auto;
        justify-content: space-between;

        @include until-lg {
            display: block;
        }
    }
}
</style>
