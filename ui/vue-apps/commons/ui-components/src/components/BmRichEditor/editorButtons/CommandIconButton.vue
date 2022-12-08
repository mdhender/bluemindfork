<template>
    <bm-icon-button
        v-if="isActive === null"
        variant="compact"
        size="lg"
        :icon="icon"
        :disabled="disabled"
        :title="title"
        @click="action"
    />
    <bm-toggleable-icon-button
        v-else
        variant="compact"
        size="lg"
        :icon="icon"
        :pressed="isActive"
        :disabled="disabled"
        :title="title"
        @click="action"
    />
</template>
<script>
import BmIconButton from "../../buttons/BmIconButton";
import BmToggleableIconButton from "../../buttons/BmToggleableIconButton";

export default {
    name: "CommandIconButton",
    components: { BmIconButton, BmToggleableIconButton },
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
        return { icon: "", title: "" };
    },
    computed: {
        isActive: () => null
    },
    methods: {
        action: () => {}
    }
};
</script>
