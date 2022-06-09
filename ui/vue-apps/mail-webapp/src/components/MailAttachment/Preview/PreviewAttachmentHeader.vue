<template>
    <div class="preview-attachment-header px-3 align-items-center">
        <bm-icon :icon="fileTypeIcon" size="2x" class="align-bottom" />
        <span :title="part.fileName" class="font-weight-bold ml-2">{{ part.fileName }} </span>
    </div>
</template>
<script>
import { BmIcon } from "@bluemind/styleguide";
import { MimeType } from "@bluemind/email";

export default {
    name: "PreviewAttachementHeader",
    components: { BmIcon },
    props: {
        part: {
            type: Object,
            required: true
        }
    },

    computed: {
        fileTypeIcon() {
            return MimeType.matchingIcon(this.part.mime);
        }
    }
};
</script>
<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.preview-attachement-header {
    display: flex;
    background-color: $neutral-bg;
}
</style>
