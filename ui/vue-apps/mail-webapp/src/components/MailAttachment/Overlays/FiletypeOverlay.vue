<template>
    <div class="action-overlay d-flex align-center-items" :class="matchingIcon">
        <bm-icon :icon="getHoverIcon(file)" size="6x" class="m-auto p-1" />
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";
import { MimeType } from "@bluemind/email";
import { PreviewMixin } from "~/mixins";

export default {
    name: "FiletypeOverlay",
    components: { BmIcon },
    mixins: [PreviewMixin],
    computed: {
        matchingIcon() {
            return MimeType.matchingIcon(this.file.mime);
        }
    },
    methods: {
        getHoverIcon(file) {
            return !this.isAllowedToPreview ? "download" : this.matchingPreviewIcon(file);
        },
        matchingPreviewIcon(file) {
            return MimeType.isAudio(file) || MimeType.isVideo(file) ? "play" : "eye";
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_fileTypeIconsColors.scss";
@import "@bluemind/styleguide/css/_variables.scss";

.action-overlay {
    @each $file-type, $color in $file-type-icons-colors {
        &.#{$file-type} {
            background-color: $color;
            color: $lowest;
        }
    }
    min-height: 100%;
}
</style>
