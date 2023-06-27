<template>
    <b-button-close class="bm-button-close" :disabled="disabled" :class="`btn-close-${size}`" v-on="$listeners" />
</template>

<script>
import { BButtonClose } from "bootstrap-vue";

export default {
    name: "BmButtonClose",
    components: { BButtonClose },
    props: {
        size: {
            type: String,
            default: "md",
            validator(value) {
                return ["xs", "sm", "md", "lg"].includes(value);
            }
        },
        disabled: {
            type: Boolean,
            default: false
        }
    }
};
</script>
