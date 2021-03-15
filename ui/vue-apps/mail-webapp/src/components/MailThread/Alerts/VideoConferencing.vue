<template>
    <a :href="payload" target="_blank">{{ $t(path) }}</a>
</template>
<script>
import { AlertMixin } from "@bluemind/alert.store";

export default {
    name: "BlockedRemoteContent",
    mixins: [AlertMixin]
};
</script>
