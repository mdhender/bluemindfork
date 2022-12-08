<template>
    <bm-circular-progress v-if="circular" v-bind="$props" v-on="$listeners"><slot /></bm-circular-progress>
    <b-progress v-else v-bind="$props" v-on="$listeners"><slot /></b-progress>
</template>

<script>
import { BProgress } from "bootstrap-vue";
import BmCircularProgress from "./BmCircularProgress";

export default {
    name: "BmProgress",
    components: { BmCircularProgress, BProgress },
    extends: BProgress,
    props: {
        variant: {
            type: String,
            default: "secondary"
        },
        circular: {
            type: Boolean,
            default: false
        }
    }
};
</script>
