<template>
    <bm-dropdown-item-button :icon="icon" :disabled="disabled" :title="title" @click="action">
        {{ text }}
    </bm-dropdown-item-button>
</template>
<script>
import BmDropdownItemButton from "../../dropdown/BmDropdownItemButton";

export default {
    name: "CommandDropdownItemButton",
    components: { BmDropdownItemButton },
    props: {
        disabled: {
            type: Boolean,
            default: false
        },
        editor: {
            type: Object,
            required: true
        }
    },
    data() {
        return { icon: "", title: "", text: "" };
    },
    methods: {
        action: () => {}
    }
};
</script>
