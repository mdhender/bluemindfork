<template>
    <div class="preview-overlay d-flex align-center-items">
        <bm-icon :icon="icon" size="3xl" class="m-auto p-1" />
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "PreviewOverlay",
    components: { BmIcon },
    props: {
        icon: {
            type: String,
            default: "eye"
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.preview-overlay {
    background-color: rgba(10, 42, 134, 0.5);
    min-height: 100%;

    .bm-icon {
        color: $surface-bg;
    }
}
</style>
