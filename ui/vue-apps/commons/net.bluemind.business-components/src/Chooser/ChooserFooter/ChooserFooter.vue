<template>
    <div class="chooser-footer">
        <div class="pt-2 pb-3">
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
import { inject } from "@bluemind/inject";
import { BmIcon } from "@bluemind/styleguide";
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
        const attService = inject("AttachmentPersistence");
        const { autoDetachmentLimit, maxFilesize } = await attService.getConfiguration();
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
@import "~@bluemind/styleguide/css/_variables";
@import "~@bluemind/styleguide/css/mixins/_responsiveness";

.chooser-footer {
    min-width: 100%;

    .toolbars {
        display: flex;
        flex-direction: row-reverse;
        flex: 1 1 auto;
        justify-content: space-between;

        @include until-lg {
            display: block;
        }
    }
}
</style>
