<template>
    <bm-dropdown
        ref="dropdown"
        class="bm-form-select"
        :class="{ shown: isShown, underline: variant === 'underline', inline: variant === 'inline' }"
        variant="outline"
        :disabled="disabled"
        :boundary="boundary"
        :no-flip="noFlip"
        size="lg"
        :menu-class="dropdownMenuClasses"
        @show="isShown = true"
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
                return ["outline", "underline", "inline"].includes(value);
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
        menuClass: {
            type: String,
            default: ""
        }
    },
    data() {
        return { isShown: false, options_: this.options.map(option => normalize(option)) };
    },
    computed: {
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
@import "../../css/mixins/_buttons.scss";
@import "../../css/mixins/_focus.scss";
@import "../../css/_type.scss";
@import "../../css/_variables.scss";

.bm-form-select {
    line-height: $line-height-sm;

    $padding-x: calc(#{$sp-5} - #{$input-border-width});
    $padding-x-dimmed: calc(#{$sp-5} - #{2 * $input-border-width});

    .btn.dropdown-toggle {
        width: 100%;
        height: $input-height;
        justify-content: space-between;
        padding: 0 $padding-x;
        gap: 0;
        outline: none;

        @include bm-button-variant(
            $normal-text: $neutral-fg,
            $normal-stroke: $neutral-fg-lo1,
            $hovered-text: $neutral-fg-hi1,
            $hovered-stroke: $neutral-fg-hi1,
            $disabled-text: $neutral-fg-lo1
        );
    }

    &.shown .btn.dropdown-toggle,
    .btn.dropdown-toggle.focus,
    .btn.dropdown-toggle:focus {
        border: 2 * $input-border-width solid $secondary-fg !important;
        padding: 0 $padding-x-dimmed;
        box-shadow: none !important;
    }

    &.underline {
        $padding-x: $sp-4;

        .btn.dropdown-toggle {
            @include bm-button-variant(
                $normal-text: $neutral-fg,
                $normal-stroke: $neutral-fg-lo2,
                $hovered-text: $neutral-fg-hi1,
                $hovered-stroke: $neutral-fg,
                $disabled-text: $neutral-fg-lo1
            );

            border-radius: 0 !important;
            border-left: none !important;
            border-right: none !important;
            border-top-color: transparent !important;
            padding: 0 $padding-x !important;
        }
    }

    &.inline {
        $padding-x: $sp-4;

        .btn.dropdown-toggle {
            height: calc(#{$input-height} - #{2 * $input-border-width});
            border: none !important;
            padding: 0 $padding-x !important;
        }

        .btn.dropdown-toggle.focus,
        .btn.dropdown-toggle:focus {
            @include default-focus($neutral-fg);
            &.hover,
            &:hover {
                @include default-focus($neutral-fg-hi1);
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

    .content {
        @extend %regular;
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
}
</style>
