<template>
    <div class="preview-file-header px-3 align-items-center">
        <bm-icon :icon="getMatchingIcon(file.mime)" size="2x" class="align-bottom" />
        <span :title="file.name" class="font-weight-bold ml-2">{{ file.name }} </span>
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
    background-color: $neutral-bg;
}
</style>
