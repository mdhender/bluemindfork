<template>
    <bm-button class="preview-message-header px-5 d-none d-lg-flex" variant="text" :icon="icon">
        Message
    </bm-button>
</template>
<script>
import { BmButton } from "@bluemind/styleguide";

export default {
    name: "PreviewMessageHeader",
    components: { BmButton },
    props: {
        expanded: { type: Boolean, required: true }
    },
    computed: {
        icon() {
            return this.expanded ? "chevron" : "chevron-right";
        }
    }
};
</script>

<style lang="scss">
@use "sass:map";
@import "~@bluemind/styleguide/css/variables";

.preview-message-header {
    .bm-icon {
        $size: map-get($icon-sizes, "xs");
        width: $size;
        height: $size;
    }
    &:hover {
        background-color: $neutral-bg-lo1;
    }
}
</style>
