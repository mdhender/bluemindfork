<template>
    <bm-button-toolbar class="bm-choice-group" key-nav>
        <bm-button-group>
            <bm-button
                v-for="option in options"
                :key="option.value"
                :pressed="selectedOption == option.value"
                :disabled="option.disabled"
                :href="option.href"
                :to="option.to"
                variant="text"
                role="button"
                class="text-truncate px-4"
                :class="{
                    'bottom-selector': bottomSelectorPosition == option.value,
                    backward: selectionChangeDirectionClass == 'backward'
                }"
                @click="select(option)"
            >
                {{ option.text }}
            </bm-button>
        </bm-button-group>
    </bm-button-toolbar>
</template>
<script>
import BmButtonToolbar from "./buttons/BmButtonToolbar";
import BmButtonGroup from "./buttons/BmButtonGroup";
import BmButton from "./buttons/BmButton";
export default {
    name: "BmChoiceGroup",
    components: {
        BmButtonToolbar,
        BmButtonGroup,
        BmButton
    },
    props: {
        options: {
            type: Array,
            required: true
        },
        selected: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            selectedOption: this.selected.value,
            selectionChangeDirectionClass: null,
            bottomSelectorPosition: this.selected.value
        };
    },
    watch: {
        selected() {
            if (this.selectedOption !== this.selected.value) {
                this.handleBottomSelector(this.selectedOption, this.selected.value);
            }
            this.selectedOption = !this.selected.disabled ? this.selected.value : this.selectedOption;
        }
    },
    methods: {
        select(option) {
            if (option.value !== this.selectedOption) {
                const selectedIndex = this.options.map(o => o.value).indexOf(option.value);
                this.$emit("select", selectedIndex);
            }
        },
        /** Handle bottom selector (blue line) animation by dynamically changing button classes. */
        handleBottomSelector(previousOptionValue, newOptionValue) {
            // compute the direction change: forward (to the right) or backward (to the left)
            let previousOptionIndex = -1;
            let newOptionIndex = -1;
            this.options.forEach((option, index) => {
                if (option.value === previousOptionValue) {
                    previousOptionIndex = index;
                }
                if (option.value === newOptionValue) {
                    newOptionIndex = index;
                }
            });
            this.selectionChangeDirectionClass = newOptionIndex - previousOptionIndex > 0 ? "forward" : "backward";

            // prepare the move of the selector, from the previous selection to the new one, through ones in between
            if (this.selectionChangeDirectionClass === "forward") {
                let j = 0;
                for (let i = previousOptionIndex; i <= newOptionIndex; i++) {
                    setTimeout(this.addBottomSelectorClass, 125 * j++, i);
                }
            } else {
                let j = 0;
                for (let i = previousOptionIndex; i >= newOptionIndex; i--) {
                    setTimeout(this.addBottomSelectorClass, 125 * j++, i);
                }
            }
        },
        addBottomSelectorClass(index) {
            this.bottomSelectorPosition = this.options[index].value;
        }
    }
};
</script>
<style lang="scss">
@import "../css/_variables.scss";

.bm-choice-group .btn-group {
    // need this in order 'text-truncate' works
    min-width: 0;
}

.bm-choice-group {
    .btn {
        border: none !important;
    }
    .btn.active {
        color: $primary-fg !important;
        background: none !important;
    }
    .btn::after {
        content: "";
        height: 3px;
        background: $secondary-fg none;
        bottom: 0px;
        position: absolute;
        display: inline-block;
        animation-duration: 0.125s;
        animation-timing-function: linear;
    }
    .btn.bottom-selector::after {
        width: 100%;
        animation-name: width-grow;
    }
    .btn:not(.bottom-selector)::after {
        width: 0%;
        animation-name: width-shrink;
    }
    .btn.bottom-selector:not(.backward)::after,
    .btn:not(.bottom-selector).backward::after {
        left: 0px;
    }
    .btn.bottom-selector.backward::after,
    .btn:not(.bottom-selector):not(.backward)::after {
        right: 0px;
    }
}
</style>
