<template>
    <div class="contextual-bar d-flex align-items-center">
        <bm-icon-button
            variant="compact-on-fill-primary"
            size="lg"
            class="d-lg-none"
            icon="arrow-back"
            @click="$emit('back')"
        />
        <div class="slot-wrapper">
            <slot />
        </div>
    </div>
</template>

<script>
import { BmIconButton } from "@bluemind/ui-components";

export default {
    name: "ContextualBar",
    components: {
        BmIconButton
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.contextual-bar {
    & > .bm-icon-button {
        border-right: $input-border-width solid $fill-primary-fg-lo2;
    }
    & > .slot-wrapper {
        padding-left: $sp-5;
    }
}
</style>
