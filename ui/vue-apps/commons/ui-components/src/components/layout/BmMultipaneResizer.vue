<template>
    <multipane-resizer class="bm-multipane-resizer" v-bind="[$attrs, $props]" v-on="$listeners" />
</template>

<script>
import { MultipaneResizer } from "@bluemind/vue-multipane";

export default {
    name: "BmMultipaneResizer",
    components: { MultipaneResizer },
    props: {
        ...MultipaneResizer.props
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables.scss";

.multipane-resizer.bm-multipane-resizer {
    flex: none;
    background: none;
    z-index: 2; // multipane divs have z-index: 1
}

@mixin bm-multipane-style($dimension, $start, $end) {
    #{$start}: 0;
    #{$dimension}: $resizer-size !important;
    margin-#{$start}: 0;
    margin-#{$end}: -$resizer-size;

    border-#{$start}: 1px solid $separator-color;
    &:hover {
        border-#{$start}-color: $resizer-hover-color;
    }
    &:active {
        border-width: 2px;
        border-#{$start}-color: $resizer-active-color;
    }
}

.layout-v > .multipane-resizer.bm-multipane-resizer {
    @include bm-multipane-style(width, left, right);
}

.layout-h > .multipane-resizer.bm-multipane-resizer {
    @include bm-multipane-style(height, top, bottom);
}
</style>
