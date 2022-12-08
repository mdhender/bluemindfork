<template>
    <bm-form-checkbox v-bind="$props" class="bm-check" v-on="$listeners">
        <slot />
    </bm-form-checkbox>
</template>

<script>
import BmFormCheckbox from "./form/BmFormCheckbox";

/**
 * The BmCheck component aims to show and set a selected state on an element or a list of elements. <br>
 *      It must not be confused with BmFormCheckbox which is meant to be used inside a form. <br>
 *      BmCheck has 3 states :Checked, Unchecked, Indeterminate. <br>
 *      BmCheck has the same properties as BmFormCheckbox.
 */
export default {
    name: "BmCheck",
    components: {
        BmFormCheckbox
    },
    extends: BmFormCheckbox
};
</script>
<style lang="scss">
@import "../css/_mixins.scss";
@import "../css/_variables.scss";

.bm-check.custom-checkbox .custom-control-input {
    & ~ .custom-control-label::before {
        border-radius: 1em;
        border-color: $input-border-color;
        background-clip: padding-box;
    }

    &:checked ~ .custom-control-label::before,
    &:indeterminate ~ .custom-control-label::before {
        border-color: $fill-secondary-bg;
    }
}

.bm-check.custom-checkbox .custom-control-input:disabled {
    & ~ .custom-control-label::before {
        border-color: $neutral-fg-disabled;
    }

    &:checked ~ .custom-control-label::before,
    &:indeterminate ~ .custom-control-label::before {
        border-color: $fill-neutral-bg-disabled;
        background-color: $fill-neutral-bg-disabled;
    }
}

.bm-check.custom-checkbox:hover .custom-control-input:not(:disabled) {
    & ~ .custom-control-label::before {
        border-color: $neutral-fg-hi1;
    }

    &:checked ~ .custom-control-label::before,
    &:indeterminate ~ .custom-control-label::before {
        border-color: $fill-secondary-bg-hi1;
        background-color: $fill-secondary-bg-hi1;
    }

    & ~ .custom-control-label::after {
        cursor: pointer;
    }
}

.bm-check.custom-checkbox .custom-control-input:focus {
    & ~ .custom-control-label::before {
        box-shadow: none;
        border-color: $neutral-fg;
        @include default-focus($neutral-fg, true);
    }
    &:checked ~ .custom-control-label::before,
    &:indeterminate ~ .custom-control-label::before {
        border-color: $fill-secondary-bg-hi1;
        background-color: $fill-secondary-bg-hi1;
        @include default-focus($fill-secondary-bg-hi1, true);
    }
}
</style>
