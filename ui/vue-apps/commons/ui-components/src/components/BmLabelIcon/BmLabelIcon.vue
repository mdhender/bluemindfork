<template>
    <div class="bm-label-icon" :class="{ 'd-inline-flex align-items-center align-middle': inline }">
        <bm-icon :icon="icon" :size="iconSize" fixed-width class="mr-3" />
        <div :class="inline ? 'flex-fill' : 'd-inline'" :title="tooltip"><slot /></div>
    </div>
</template>

<script>
import BmIcon from "../BmIcon";

export default {
    name: "BmLabelIcon",
    components: {
        BmIcon
    },
    props: {
        icon: {
            type: String,
            required: true
        },
        iconSize: {
            type: String,
            default: "md"
        },
        tooltip: {
            type: String,
            default: null
        },
        inline: {
            type: Boolean,
            default: true
        }
    }
};
</script>
