<template>
    <bm-toolbar-icon-button
        v-if="isActive === null"
        variant="compact"
        size="lg"
        :icon="icon"
        :text="text"
        :disabled="disabled"
        :title="title"
        @click="action"
    />
    <bm-toolbar-toggle-icon
        v-else
        variant="compact"
        size="lg"
        :icon="icon"
        :text="text"
        :pressed="isActive"
        :disabled="disabled"
        :title="title"
        @click="action"
    />
</template>
<script>
import BmIconButton from "../../buttons/BmIconButton";
import BmToolbarIconButton from "../../BmToolbar/BmToolbarIconButton";
import BmToolbarToggleIcon from "../../BmToolbar/BmToolbarToggleIcon";

export default {
    name: "CommandIconButton",
    components: { BmToolbarIconButton, BmToolbarToggleIcon },
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
    computed: {
        isActive: () => null
    },
    methods: {
        action: () => {}
    }
};
</script>
