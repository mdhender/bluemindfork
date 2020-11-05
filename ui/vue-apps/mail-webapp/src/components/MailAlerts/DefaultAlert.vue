<template functional>
    <span>{{ parent.$t($options.i18n(props.alert, options)) }}</span>
</template>
<script>
// import alertTexts from "../../alertTexts";

export default {
    name: "DefaultAlert",
    props: {
        alert: {
            type: Object,
            default: () => ({})
        },
        options: {
            type: Object,
            required: false,
            default: () => ({})
        }
    },
    i18n({ name, type }) {
        return ["alert", name.toLowerCase(), type.toLowerCase()].join(".");
    }
};
</script>
