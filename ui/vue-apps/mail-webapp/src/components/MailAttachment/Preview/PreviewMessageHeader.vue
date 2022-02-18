<template>
    <bm-button class="preview-message-header px-3 d-none d-lg-flex align-items-center" variant="simple-dark">
        <bm-icon :icon="icon" />
        <span class="font-weight-bold ml-2"> Message </span>
    </bm-button>
</template>
<script>
import { BmIcon, BmButton } from "@bluemind/styleguide";

export default {
    name: "PreviewMessageHeader",
    components: { BmIcon, BmButton },
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
@import "~@bluemind/styleguide/css/variables";

.preview-message-header {
    &:hover {
        background-color: $light;
    }
}
</style>
