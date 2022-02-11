<template>
    <div class="preview-attachment-header bg-light px-3 d-flex align-items-center">
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
