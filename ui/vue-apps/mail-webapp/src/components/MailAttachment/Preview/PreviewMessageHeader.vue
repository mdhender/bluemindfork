<template>
    <bm-button class="preview-message-header px-5 d-none d-lg-flex" variant="text" :icon="icon"> Message </bm-button>
</template>
<script>
import { BmButton } from "@bluemind/ui-components";

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
@import "~@bluemind/ui-components/src/css/variables";

.preview-message-header.btn {
    justify-content: start;
    height: 100%;
    outline-offset: -1px;
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
