<template>
    <b-breadcrumb
        class="bm-breadcrumb"
        :class="{ 'on-fill-primary': onFillPrimary }"
        v-bind="childProps"
        v-on="$listeners"
    >
        <slot />
    </b-breadcrumb>
</template>

<script>
import omit from "lodash.omit";
import { BBreadcrumb } from "bootstrap-vue";

export default {
    name: "BmBreadcrumb",
    components: { BBreadcrumb },
    props: {
        ...BBreadcrumb.options.props,
        onFillPrimary: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        childProps() {
            return omit(this.$props, "onFillPrimary");
        }
    }
};
</script>

<style lang="scss">
@import "../css/utils/responsiveness";
@import "../css/utils/variables";

.breadcrumb.bm-breadcrumb {
    margin: 0;
    padding: 0;
    display: flex;
    flex-wrap: nowrap;
    min-width: 0;
    line-height: $breadcrumb-height;
}

.bm-breadcrumb .bm-breadcrumb-item:last-child {
    min-width: unset !important;
}

.bm-breadcrumb .bm-breadcrumb-item + .bm-breadcrumb-item {
    padding-left: 0;
    min-width: 2 * $sp-2 + map-get($icon-sizes, "md") + $breadcrumb-separator-width;
    @include from-lg {
        min-width: 2 * $sp-3 + map-get($icon-sizes, "md") + $breadcrumb-separator-width;
    }
    &::before {
        content: "/";
        padding: 0;
        float: none;
        flex: none;
        position: relative;
        top: base-px-to-rem(1);
        width: $breadcrumb-separator-width;
        text-align: center;
        color: $neutral-fg-lo1;
    }
}
.bm-breadcrumb.on-fill-primary .bm-breadcrumb-item + .bm-breadcrumb-item {
    &::before {
        color: $fill-primary-fg-lo1;
    }
}
</style>
