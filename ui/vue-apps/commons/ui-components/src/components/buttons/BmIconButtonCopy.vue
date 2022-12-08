<template>
    <bm-icon-button
        v-bind="[$attrs, $props]"
        class="bm-icon-button-copy"
        :pressed="pressed"
        :icon="pressed ? 'check' : 'copy'"
        variant="compact"
        @click="action"
        v-on="$listeners"
    >
        <slot />
    </bm-icon-button>
</template>

<script>
import BmIconButton from "./BmIconButton";

export default {
    name: "BmIconButtonCopy",
    components: { BmIconButton },
    props: {
        text: { type: [String, Function], required: true }
    },
    data() {
        return { pressed: false };
    },
    watch: {
        pressed(value) {
            this.$emit("pressed", value);
        }
    },
    methods: {
        action() {
            this.pressed = true;
            const text = this.text instanceof Function ? this.text() : this.text;
            navigator.clipboard.writeText(text);
            setTimeout(() => (this.pressed = false), 2000);
        }
    }
};
</script>

<style lang="scss">
@import "../../css/mixins/_buttons";

.bm-icon-button-copy.bm-icon-button {
    &.active {
        @include bm-icon-button-compact-variant("success");
    }
}
</style>
