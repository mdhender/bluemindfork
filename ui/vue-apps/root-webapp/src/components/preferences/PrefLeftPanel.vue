<template>
    <bm-col class="pref-left-panel text-white d-lg-block" cols="12" lg="2">
        <div class="p-3">
            <h2 class="d-none d-lg-block">
                <bm-label-icon icon="preferences">{{ $t("common.preference") }}</bm-label-icon>
            </h2>
            <div class="d-lg-none">
                <bm-button variant="inline-light" class="btn-sm mr-auto" @click="$emit('close')">
                    <bm-icon icon="arrow-back" size="2x" />
                </bm-button>
                <h2 class="d-inline align-middle">{{ $t("common.preference") }}</h2>
            </div>
            <div class="text-center mt-4">
                <h1><bm-avatar :alt="user.displayname" /> <br />{{ user.displayname }}</h1>
                <a href="/preferences/" class="text-primary font-weight-bold">
                    {{ $t("preferences.manage.account") }}
                </a>
            </div>
        </div>
        <pref-left-panel-nav :sections="sections" />
    </bm-col>
</template>

<script>
import { BmButton, BmCol, BmIcon, BmLabelIcon, BmAvatar } from "@bluemind/styleguide";
import PrefLeftPanelNav from "./PrefLeftPanelNav";

export default {
    name: "PrefLeftPanel",
    components: {
        BmAvatar,
        BmButton,
        BmIcon,
        BmLabelIcon,
        BmCol,
        PrefLeftPanelNav
    },
    props: {
        sections: {
            required: true,
            type: Array
        },
        user: {
            required: true,
            type: Object
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-left-panel {
    background-color: $info-dark;

    .bm-avatar {
        font-size: 3rem;
    }
    .bm-label-icon {
        color: $white;
    }
}
</style>
