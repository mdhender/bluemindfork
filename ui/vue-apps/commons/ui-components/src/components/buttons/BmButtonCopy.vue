<template>
    <bm-toggleable-button
        v-bind="[$attrs, $props]"
        class="bm-button-copy"
        style="width: 7.5rem; flex: none;"
        :pressed="pressed"
        :icon="pressed ? 'check' : 'copy'"
        @click="action"
        v-on="$listeners"
    >
        {{ $t(pressed ? "common.copied" : "common.copy") }}
    </bm-toggleable-button>
</template>

<script>
import BmToggleableButton from "./BmToggleableButton";

export default {
    name: "BmButtonCopy",
    components: { BmToggleableButton },
    props: {
        text: { type: [String, Function], required: true }
    },
    data() {
        return {
            pressed: false
        };
    },
    methods: {
        action() {
            const text = this.text instanceof Function ? this.text() : this.text;
            navigator.clipboard.writeText(text);
            this.pressed = true;
            setTimeout(() => {
                this.pressed = false;
            }, 2000);
        }
    }
};
</script>

<style lang="scss">
@import "../../css/mixins/_buttons";

.bm-button-copy.bm-toggleable-button {
    &.active {
        &.btn-outline {
            @include bm-button-fill-variant("fill-success");
        }
        &.btn-text {
            @include bm-button-text-variant("success");
        }
    }
}
</style>
