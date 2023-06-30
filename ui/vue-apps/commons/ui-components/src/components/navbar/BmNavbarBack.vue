<template>
    <div class="bm-navbar-back">
        <bm-icon-button variant="compact-on-fill-primary" size="lg" icon="arrow-back" class="h-100" v-on="$listeners" />
        <div class="separator" />
    </div>
</template>

<script>
import BmIconButton from "../buttons/BmIconButton";

export default {
    name: "BmNavbarBack",
    components: { BmIconButton }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables";

.bm-navbar-back {
    flex: none;
    display: flex;
    align-items: stretch;
    height: base-px-to-rem(40);
    .separator {
        width: $input-border-width;
        border-right: $input-border-width solid $fill-primary-fg-lo2;
    }
}
</style>
