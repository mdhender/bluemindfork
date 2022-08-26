<template>
    <div class="preview-file-header text-truncate">
        <bm-icon :icon="getMatchingIcon(file.mime)" class="mx-5" />
        <div :title="file.name" class="bold text-truncate">{{ file.name }}</div>
    </div>
</template>
<script>
import { BmIcon } from "@bluemind/styleguide";
import { MimeType } from "@bluemind/email";

export default {
    name: "PreviewFileHeader",
    components: { BmIcon },
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    methods: {
        getMatchingIcon(mime) {
            return MimeType.matchingIcon(mime);
        }
    }
};
</script>
<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.preview-file-header {
    display: flex;
    align-items: center;
    background-color: $neutral-bg;
}
</style>
