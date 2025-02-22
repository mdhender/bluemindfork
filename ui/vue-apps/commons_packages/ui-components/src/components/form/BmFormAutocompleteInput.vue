<template>
    <div class="bm-form-autocomplete-input position-relative" @focusout="onFocusOut">
        <bm-form-input
            ref="input"
            v-model="newInputValue"
            v-bind="$attrs"
            autocomplete="off"
            type="text"
            :size="size"
            :icon="icon"
            :actionable-icon="actionableIcon"
            :disabled="disabled"
            :placeholder="placeholder"
            @input="onInput"
            @paste="$emit('paste', $event)"
            @keyup.esc="
                if (!closeAutocomplete) {
                    closeAutocomplete = true;
                    $event.stopPropagation();
                }
            "
            @keydown.up.prevent="goUp"
            @keydown.down.prevent="goDown"
            @keydown.enter.prevent.stop="submit"
            @keydown.tab="selectResult(-1, $event)"
            @focusin="onFocusIn"
            @reset="reset"
        />
        <bm-list-group
            v-show="showAutocomplete"
            ref="suggestions"
            class="suggestions shadow position-absolute list-no-borders scroller-y"
            tabindex="-1"
        >
            <bm-list-group-item
                v-for="(item, index) in items_"
                :key="index"
                ref="suggestion"
                active-class="active"
                :active="selectedResult_ === index"
                @click="selectResult(index, $event)"
            >
                <slot :item="item">{{ item }}</slot>
            </bm-list-group-item>
            <template v-if="$slots.extra">
                <bm-list-group-separator class="extra-separator py-0" />
                <bm-list-group-item ref="extra" :active="selectedResult_ === 'extra'" @click="$emit('submitExtra')">
                    <slot name="extra" :close="closeSuggestions" :focus="focus" :go-up="goUp" :go-down="goDown" />
                </bm-list-group-item>
            </template>
        </bm-list-group>
    </div>
</template>

<script>
import AutocompleteMixin from "../../mixins/Autocomplete";
import BmFormInput from "./BmFormInput";
import BmListGroup from "../lists/BmListGroup";
import BmListGroupItem from "../lists/BmListGroupItem";
import BmListGroupSeparator from "../lists/BmListGroupSeparator.vue";

export default {
    name: "BmFormAutocompleteInput",
    components: {
        BmFormInput,
        BmListGroup,
        BmListGroupItem,
        BmListGroupSeparator
    },
    mixins: [AutocompleteMixin],
    props: {
        selectedResult: { type: Number, default: 0 },
        actionableIcon: { type: Boolean, default: false },
        size: { type: String, default: "md" },
        disabled: { type: Boolean, default: false },
        placeholder: { type: String, default: undefined }
    },
    data() {
        return { selectedResult_: this.selectedResult };
    },
    computed: {
        showAutocomplete() {
            return (this.items.length > 0 || this.$slots.extra) && !this.closeAutocomplete;
        }
    },
    watch: {
        value() {
            if (!this.value) {
                this.selectedResult_ = 0;
            }
        },
        selectedResult() {
            this.selectedResult_ = this.selectedResult;
        },
        selectedResult_() {
            this.scrollToSelected();
        },
        closeAutocomplete() {
            this.scrollToSelected();
        },
        showAutocomplete(value) {
            value ? this.$emit("autocompleteShown") : this.$emit("autocompleteHidden");
        }
    },
    methods: {
        async scrollToSelected() {
            const suggestionItems = this.$refs["suggestion"];
            if (!this.closeAutocomplete && suggestionItems) {
                const selected =
                    this.selectedResult_ === "extra" ? this.$refs["extra"] : suggestionItems[this.selectedResult_];
                if (selected) {
                    const suggestions = this.$refs["suggestions"];
                    suggestions.scrollTop = selected.offsetTop - suggestions.offsetHeight * 0.5 + selected.offsetHeight;
                }
            }
        },
        selectResult(index, event) {
            let realIndex = index === -1 ? this.selectedResult_ : index;
            let selectedItem = this.items[realIndex];
            if (selectedItem && !this.closeAutocomplete) {
                this.selectedResult_ = realIndex;
                this.$emit("selected", selectedItem);
                this.closeSuggestions();
                event.preventDefault();
                return true;
            }
            return false;
        },
        submit(event) {
            if (this.selectedResult_ === "extra") {
                this.$emit("submitExtra");
            } else if (!this.selectResult(-1, event) && this.value !== "") {
                this.$emit("submit");
                this.closeSuggestions();
                event.preventDefault();
            }
        },
        goUp() {
            if (this.selectedResult_ > 0) {
                this.selectedResult_--;
            } else if (this.selectedResult_ === 0 && this.$scopedSlots.extra) {
                this.selectedResult_ = "extra";
                this.$emit("focusExtra");
            } else {
                this.selectedResult_ = Math.min(this.maxResults, this.items.length) - 1;
            }
        },
        goDown() {
            const max = Math.min(this.maxResults, this.items.length) - 1;
            if (this.selectedResult_ < max) {
                this.selectedResult_++;
            } else if ((this.selectedResult_ === max || max === -1) && this.$scopedSlots.extra) {
                this.selectedResult_ = "extra";
                this.$emit("focusExtra");
            } else {
                this.selectedResult_ = 0;
            }
        },
        focus() {
            this.$refs.input.focus();
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables.scss";

.bm-form-autocomplete-input {
    $max-nb-suggestions: 5;
    $suggestion-height: $input-height;

    .list-group-separator {
        border-color: $neutral-fg-lo2 !important;
    }

    .suggestions {
        z-index: $zindex-dropdown;
        cursor: pointer;
        background-color: $surface-hi2;
        width: 100%;
        border: 2 * $input-border-width solid $secondary-fg;
        max-height: max(33vh, #{$max-nb-suggestions * $suggestion-height});

        .list-group-item {
            &.hover,
            &:hover {
                color: $neutral-fg-hi1;
                background-color: $neutral-bg-lo1;
            }

            &.active,
            &:active {
                background-color: $secondary-bg-lo1;
                &.hover,
                &:hover {
                    background-color: $secondary-bg;
                }
            }
        }
    }

    .form-input-md + .suggestions {
        top: calc(#{$input-height} - #{2 * $input-border-width});
    }
    .form-input-sm + .suggestions {
        top: calc(#{$input-height-sm} - #{2 * $input-border-width});
    }
}
</style>
