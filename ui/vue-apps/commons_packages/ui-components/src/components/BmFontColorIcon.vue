<template>
    <bm-icon
        class="bm-font-color-icon"
        v-bind="[$attrs]"
        :stacked="[{ icon: 'font-color-neutral' }, { icon: 'font-color-indicator', customStyle: { color } }]"
    />
</template>

<script>
import BmIcon from "./BmIcon";

export default {
    name: "BmFontColorIcon",
    components: { BmIcon },
    props: {
        color: {
            type: String,
            default: ""
        }
    }
};
</script>
