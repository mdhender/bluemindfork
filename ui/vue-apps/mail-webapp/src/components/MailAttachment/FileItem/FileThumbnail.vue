<template>
    <div
        class="text-center file-thumbnail overflow-hidden d-flex justify-content-center align-items-center mb-1 position-relative"
    >
        <div v-if="hasPreview">
            <div class="thumbnail">
                <img ref="thumbnail-image" :src="file.url" :alt="$tc('common.attachmentPreview')" />
            </div>
        </div>
        <div v-else class="default-preview">
            <bm-icon :icon="matchingIcon" size="4xl" class="m-auto preview-file-type" />
        </div>
        <div class="thumbnail-overlay position-absolute">
            <slot name="overlay" :has-preview="hasPreview" />
        </div>
    </div>
</template>
<script>
import { BmIcon } from "@bluemind/ui-components";
import { MimeType } from "@bluemind/email";
import { fileUtils } from "@bluemind/mail";

const { FileStatus } = fileUtils;

export default {
    name: "FileThumbnail",
    components: {
        BmIcon
    },
    props: {
        file: {
            type: Object,
            required: true
        },
        preview: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {
            fitPreviewImage: false
        };
    },
    computed: {
        hasPreview() {
            return (
                this.preview &&
                MimeType.previewAvailable(this.file.mime) &&
                [FileStatus.ONLY_LOCAL, FileStatus.UPLOADED].includes(this.file.status)
            );
        },
        matchingIcon() {
            return MimeType.matchingIcon(this.file.mime);
        }
    }
};
</script>
<style lang="scss">
@use "sass:map";
@import "~@bluemind/ui-components/src/css/utils/variables";

.file-thumbnail {
    aspect-ratio: 14 / 5;
    min-height: map-get($icon-sizes, "4xl");

    margin-top: $sp-3;

    background-color: $neutral-bg;

    .thumbnail > img {
        max-width: 100%;
    }

    .default-preview {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .preview-file-type {
        color: $neutral-bg;
        background-color: $surface;
    }

    .thumbnail-overlay {
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
    }
}
</style>
