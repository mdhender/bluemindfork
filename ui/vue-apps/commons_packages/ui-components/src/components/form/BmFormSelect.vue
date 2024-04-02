<template>
    <bm-dropdown
        ref="dropdown"
        class="bm-form-select"
        :class="{
            shown: isShown,
            outline: variant === 'outline',
            underline: variant === 'underline',
            inline: isInline
        }"
        :variant="dropdownVariant"
        :disabled="disabled"
        :boundary="boundary"
        :no-flip="noFlip"
        :right="right"
        size="lg"
        :menu-class="dropdownMenuClasses"
        @show="
            $emit('click');
            isShown = true;
        "
        @shown="scrollToSelected"
        @hidden="isShown = false"
    >
        <template #button-content>
            <div class="width-limits-control" :style="autoMinWidth ? { 'min-width': dropdownDefaultWidth + 'em' } : {}">
                <slot :selected="selected" name="selected">
                    <span v-if="selectedText" class="content selected-text">
                        {{ selectedText }}
                    </span>
                    <span v-else class="content placeholder">
                        {{ placeholder }}
                    </span>
                </slot>
            </div>
        </template>
        <slot name="header" />
        <bm-dropdown-item
            v-for="(item, index) in options_"
            :key="index"
            ref="optionItem"
            :active="isEqual(value, item.value)"
            @click="onSelect(item)"
        >
            <slot :item="item" name="item">{{ item.text }}</slot>
        </bm-dropdown-item>
    </bm-dropdown>
</template>

<script>
import isEqual from "lodash.isequal";
import BmDropdown from "../dropdown/BmDropdown";
import BmDropdownItem from "../dropdown/BmDropdownItem";

export default {
    name: "BmFormSelect",
    components: {
        BmDropdown,
        BmDropdownItem
    },
    props: {
        options: {
            type: Array,
            required: true
        },
        value: {
            type: [String, Number, Boolean, Object],
            default: undefined
        },
        disabled: {
            type: Boolean,
            default: false
        },
        variant: {
            type: String,
            default: "outline",
            validator: function (value) {
                return ["outline", "underline", "inline", "inline-on-fill-primary"].includes(value);
            }
        },
        scrollbar: {
            type: Boolean,
            default: false
        },
        placeholder: {
            type: String,
            default: ""
        },
        boundary: {
            type: [String, Object, Comment],
            default: "scrollParent"
        },
        autoMinWidth: {
            type: Boolean,
            default: true
        },
        noFlip: {
            type: Boolean,
            default: false
        },
        right: {
            type: Boolean,
            default: false
        },
        menuClass: {
            type: String,
            default: ""
        }
    },
    data() {
        return { isShown: false, options_: this.options.map(option => normalize(option)) };
    },
    computed: {
        isInline() {
            return this.variant.startsWith("inline");
        },
        dropdownVariant() {
            if (this.isInline) {
                return this.variant.endsWith("on-fill-primary") ? "text-on-fill-primary" : "text";
            }
            return "outline";
        },
        selected() {
            const selected = this.options_.find(item => isEqual(item.value, this.value));
            if (!selected && this.value !== undefined) {
                this.$emit("input", undefined);
            }
            return selected;
        },
        selectedText() {
            return this.selected?.text;
        },
        dropdownDefaultWidth() {
            return this.options_.reduce((a, b) => (a?.text?.length > b.text.length ? a : b)).text.length * 0.7;
        },
        dropdownMenuClasses() {
            const classes = "mt-0 border border-secondary " + this.menuClass;
            return this.scrollbar ? classes + " scrollbar scroller-y" : classes;
        }
    },
    watch: {
        options() {
            this.options_ = this.options.map(option => normalize(option));
        }
    },
    methods: {
        onSelect(item) {
            this.$emit("input", item.value);
        },
        scrollToSelected() {
            const selectedOption = this.options.findIndex(option => isEqual(this.value, option.value));
            if (this.scrollbar && selectedOption !== -1) {
                const optionsList = this.$refs.dropdown.menu();
                const selectedItem = this.$refs["optionItem"][selectedOption];
                optionsList.scrollTop =
                    selectedItem.$el.offsetTop - optionsList.offsetHeight * 0.5 + selectedItem.$el.offsetHeight * 0.5;
            }
        },
        isEqual,
        focus() {
            this.$refs["dropdown"].$el.children[0].focus();
        }
    }
};
const isPlainObject = obj => Object.prototype.toString.call(obj) === "[object Object]";

function normalize(option) {
    if (isPlainObject(option)) {
        Object.assign({ value: option.text, text: option.value, disabled: false }, option);
        if (typeof option.text !== "string") {
            option.text = option.text.toString();
        }
        return option;
    } else {
        return { value: option, text: option.toString(), disabled: false };
    }
}
</script>

<style lang="scss">
@import "../../css/utils/buttons";
@import "../../css/utils/focus";
@import "../../css/utils/typography";
@import "../../css/utils/variables";

.bm-form-select {
    line-height: $line-height-sm;

    .btn.dropdown-toggle {
        width: 100%;
        height: $input-height;
        justify-content: space-between;
        gap: 0 !important;
    }

    &.outline {
        $padding-x: calc(#{$sp-5} - #{$input-border-width});
        $padding-x-dimmed: calc(#{$sp-5} - #{2 * $input-border-width});

        .btn.dropdown-toggle {
            @include bm-button-variant(
                $normal-text: $neutral-fg,
                $hovered-text: $neutral-fg-hi1,
                $disabled-text: $neutral-fg-disabled,
                $normal-stroke: $neutral-fg-lo1,
                $hovered-stroke: $neutral-fg-hi1,
                $disabled-stroke: $neutral-fg-disabled,
                $hovered-bg: $neutral-bg-lo1
            );
            outline: none;
            padding: 0 $padding-x;
        }
        &.shown .btn.dropdown-toggle,
        .btn.dropdown-toggle.focus,
        .btn.dropdown-toggle:focus {
            border: 2 * $input-border-width solid $secondary-fg !important;
            padding: 0 $padding-x-dimmed;
            box-shadow: none !important;
        }
    }

    &:not(.inline).underline {
        .btn.dropdown-toggle {
            @include bm-button-variant(
                $normal-text: $neutral-fg,
                $hovered-text: $neutral-fg-hi1,
                $disabled-text: $neutral-fg-disabled,
                $normal-stroke: $neutral-fg-lo2,
                $hovered-stroke: $neutral-fg,
                $disabled-stroke: transparent,
                $hovered-bg: $neutral-bg-lo1
            );
            border-radius: 0 !important;
            border-left: none !important;
            border-right: none !important;
            border-top-color: transparent !important;
            padding: 0 $sp-4 !important;
        }
        &.shown .btn.dropdown-toggle,
        .btn.dropdown-toggle.focus,
        .btn.dropdown-toggle:focus {
            border-bottom: 2 * $input-border-width solid $secondary-fg !important;
            box-shadow: none !important;
        }
    }

    &.inline {
        .btn.dropdown-toggle {
            padding: 0 $sp-4 !important;
        }
        &.dropdown-on-fill-primary {
            .btn.dropdown-toggle {
                @include bm-button-variant(
                    $normal-text: $fill-primary-fg,
                    $hovered-text: $fill-primary-fg-hi1,
                    $disabled-text: $fill-primary-fg-disabled,
                    $hovered-bg: $fill-primary-bg-hi1,
                    $focused-stroke: $fill-primary-fg,
                    $focused-hovered-stroke: $fill-primary-fg-hi1
                );
            }
        }
    }

    .scrollbar {
        max-height: 25vh;
    }

    .dropdown-menu {
        min-width: 100%;
    }
    &:not(.inline) .dropdown-menu {
        top: -2 * $input-border-width !important;
        padding: 0;
        border-width: 2 * $input-border-width !important;
    }
    &.inline .dropdown-menu {
        border: none !important;
    }

    .dropdown-item {
        padding-left: $sp-5;
        padding-right: $sp-5;
    }

    .width-limits-control {
        max-width: calc(100% - #{$sp-4} - 1em);
        padding-right: $sp-2;
    }

    &:not(.dropdown-on-fill-primary) .content {
        @include regular;
    }
    .content {
        float: left;
        margin-right: $sp-2;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .placeholder {
        color: $neutral-fg-lo1;
    }
    .disabled .placeholder {
        color: $neutral-fg-disabled !important;
    }
    &.dropdown-on-fill-primary {
        .placeholder {
            color: $fill-primary-fg-lo1;
        }
        .disabled .placeholder {
            color: $fill-primary-fg-disabled !important;
        }
    }
}
</style>
