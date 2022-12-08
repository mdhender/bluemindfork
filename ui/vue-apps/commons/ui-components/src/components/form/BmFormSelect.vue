<template>
    <bm-dropdown
        ref="dropdown"
        class="bm-form-select"
        :class="{ shown: isShown, 'form-select-inline': variant === 'inline' }"
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
                    <span class="selected-text font-weight-normal">
                        {{ selectedText || placeholder }}
                    </span>
                </slot>
            </div>
        </template>
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
                return ["outline", "inline"].includes(value);
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
            return this.scrollbar ? classes + " scrollbar" : classes;
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
@import "../../css/mixins/_focus.scss";
@import "../../css/_variables.scss";

.bm-form-select {
    $btn-padding: base-px-to-rem(9);

    line-height: $line-height-sm;

    .btn.dropdown-toggle {
        width: 100%;
        justify-content: space-between;
        padding: $btn-padding;
        gap: 0;
        outline: none;
    }

    &.shown .btn.dropdown-toggle,
    .btn.dropdown-toggle.focus,
    .btn.dropdown-toggle:focus {
        border: 2 * $input-border-width solid $secondary-fg;
        padding: calc(#{$btn-padding} - #{$input-border-width});
        box-shadow: none !important;
    }

    &.form-select-inline {
        .btn.dropdown-toggle {
            border: none !important;
        }

        &.shown .btn.dropdown-toggle {
            padding: $btn-padding;
        }
        .btn.dropdown-toggle.focus,
        .btn.dropdown-toggle:focus {
            padding: $btn-padding;
            @include default-focus($neutral-fg);
            &.hover,
            &:hover {
                @include default-focus($neutral-fg-hi1);
            }
        }
    }

    .scrollbar {
        max-height: 25vh;
        overflow: auto;
    }

    .dropdown-menu {
        min-width: 100%;
        top: -2 * $input-border-width !important;
    }
    &:not(.form-select-inline) .dropdown-menu {
        padding: 0;
        border-width: 2 * $input-border-width !important;
    }
    &.form-select-inline .dropdown-menu {
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

    .selected-text {
        float: left;
        margin-right: $sp-2;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
    }
}
</style>
