<template>
    <div class="preview-overlay d-flex align-center-items">
        <div class="backdrop" />
        <bm-icon :icon="icon" size="3xl" class="m-auto p-1" />
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";

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
@import "~@bluemind/ui-components/src/css/variables";

.preview-overlay {
    min-height: 100%;
    position: relative;

    .backdrop {
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
        background-color: $fill-primary-bg;
        opacity: 0.5;
    }

    .bm-icon {
        color: $lightest;
        z-index: 1;
    }
}
</style>
