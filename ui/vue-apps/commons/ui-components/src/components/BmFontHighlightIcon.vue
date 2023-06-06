<template>
    <bm-icon
        class="bm-font-highlight-icon"
        v-bind="[$attrs]"
        :stacked="[{ icon: 'font-highlight-neutral' }, { icon: 'font-highlight-indicator', customStyle: { color } }]"
    />
</template>

<script>
import BmIcon from "./BmIcon";

export default {
    name: "BmFontHighlightIcon",
    components: { BmIcon },
    props: {
        color: {
            type: String,
            default: ""
        }
    }
};
</script>
