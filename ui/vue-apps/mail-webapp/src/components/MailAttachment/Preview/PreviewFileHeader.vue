<template>
    <div class="preview-file-header text-truncate">
        <bm-file-icon :file="file" class="mx-5" />
        <div :title="file.name" class="bold text-truncate">{{ file.name }}</div>
    </div>
</template>
<script>
import { BmFileIcon } from "@bluemind/styleguide";

export default {
    name: "PreviewFileHeader",
    components: { BmFileIcon },
    props: {
        file: {
            type: Object,
            required: true
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
