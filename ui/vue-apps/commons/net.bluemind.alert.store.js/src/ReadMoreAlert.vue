<template>
    <span>
        <default-alert v-bind="$attrs" :alert="alert" />
        <a target="_blank" :href="alert.link">{{ $t("common.read_more") }}</a>
    </span>
</template>

<script>
import DefaultAlert from "./DefaultAlert";
export default {
    name: "ReadMoreAlert",
    components: { DefaultAlert },
    props: {
        alert: {
            type: Object,
            required: true
        }
    }
};
</script>
