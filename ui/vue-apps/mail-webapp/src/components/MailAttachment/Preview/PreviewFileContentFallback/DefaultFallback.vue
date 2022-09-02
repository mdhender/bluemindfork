<template>
    <div class="default-fallback">
        <div><bm-icon :icon="icon" size="3xl" /></div>
        <span class="text">{{ text }}</span>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "DefaultFallback",
    components: { BmIcon },
    props: {
        text: {
            type: String,
            default: ""
        },
        icon: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.default-fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    color: $fill-neutral-fg-lo1;

    .text {
        color: $fill-neutral-fg;
    }
}
</style>
