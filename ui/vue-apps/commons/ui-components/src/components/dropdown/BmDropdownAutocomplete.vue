<template>
    <bm-dropdown-group class="bm-dropdown-autocomplete">
        <bm-dropdown-form
            ref="item"
            @keydown.up.prevent="forwardEvent"
            @keydown.down.prevent="forwardEvent"
            @focusin="focus"
        >
            <bm-form-input
                ref="input"
                v-model="newInputValue"
                autocomplete="off"
                type="text"
                :icon="icon"
                resettable
                @input="onInput"
                @keydown.enter.prevent
                @reset="reset"
            />
        </bm-dropdown-form>
        <bm-dropdown-divider v-if="hasDividerUnderInput" />
        <template v-for="item in items_">
            <slot :item="item">
                {{ item }}
            </slot>
        </template>
    </bm-dropdown-group>
</template>

<script>
import AutocompleteMixin from "../../mixins/Autocomplete";
import BmDropdownDivider from "./BmDropdownDivider";
import BmDropdownForm from "./BmDropdownForm";
import BmDropdownGroup from "./BmDropdownGroup";
import BmFormInput from "../form/BmFormInput";

export default {
    name: "BmDropdownAutocomplete",
    components: {
        BmDropdownDivider,
        BmDropdownForm,
        BmDropdownGroup,
        BmFormInput
    },
    mixins: [AutocompleteMixin],
    props: {
        hasDividerUnderInput: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    methods: {
        focus() {
            this.$refs["input"].focus();
        },
        forwardEvent(e) {
            const el = this.$refs["item"].firstChild;
            if (e.target !== el) {
                e.stopPropagation();
                el.dispatchEvent(new KeyboardEvent(e.type, e));
            }
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables.scss";
@import "../../css/_mixins.scss";

.bm-dropdown-autocomplete {
    .dropdown-divider {
        border-color: $neutral-fg;
    }

    .dropdown-item-content {
        @include text-overflow;
    }
}
</style>
