<template>
    <div class="pref-left-panel scroller-y">
        <div class="pref-title mobile-only">
            <bm-icon icon="preferences" />
            <div class="large-bold text-truncate">{{ $t("common.preference") }}</div>
        </div>
        <div class="identity">
            <bm-avatar :alt="userDisplayName" :urn="urn" size="xl" />
            <div class="large">{{ userDisplayName }}</div>
        </div>
        <pref-left-panel-nav :sections="sections" class="flex-grow-1" @categoryClicked="$emit('categoryClicked')" />
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmAvatar, BmIcon } from "@bluemind/ui-components";
import PrefLeftPanelNav from "./PrefLeftPanelNav";

export default {
    name: "PrefLeftPanel",
    components: { BmAvatar, BmIcon, PrefLeftPanelNav },
    props: {
        sections: {
            required: true,
            type: Array
        }
    },
    data() {
        const session = inject("UserSession");
        return {
            userDisplayName: session.formatedName,
            urn: session.userId + "@addressbook_" + session.domain
        };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-left-panel {
    display: flex;
    flex-direction: column;

    background-color: $fill-primary-bg;
    color: $fill-primary-fg;
    @include from-lg {
        padding: $sp-7 0 $sp-5;
    }
    max-height: 100%;
    flex: none;
    width: 100%;
    @include from-lg {
        width: 20%;
    }

    &::-webkit-scrollbar-track {
        background: $surface;
    }

    .identity {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        gap: $sp-5;
        margin-bottom: $sp-7;
    }

    .pref-title {
        display: flex;
        align-items: center;
        gap: $sp-4;
        padding-left: $sp-6;
        height: base-px-to-rem(48);
        flex: none;
        margin-bottom: $sp-6;
    }
}
</style>
