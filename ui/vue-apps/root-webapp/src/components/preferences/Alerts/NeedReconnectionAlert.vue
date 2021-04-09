<template>
    <div>
        {{ $t("alert.preferences.need_reconnection.warning") }}
        <bm-button variant="link" @click="triggerDisconnect">{{ $t("common.reconnect") }}</bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/styleguide";

export default {
    name: "NeedReconnectionAlert",
    components: { BmButton },
    methods: {
        triggerDisconnect() {
            location.href = "/bluemind_sso_logout";
        }
    }
};
</script>
