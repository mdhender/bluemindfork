<template>
    <bm-col class="bm-settings-left-sidebar text-white d-lg-block" cols="12" lg="2">
        <div class="p-3">
            <h2 class="d-none d-lg-block">
                <bm-label-icon icon="preferences">{{ $t("common.settings") }}</bm-label-icon>
            </h2>
            <div class="d-lg-none">
                <bm-button variant="inline-light" class="btn-sm mr-auto" @click="$emit('close')">
                    <bm-icon icon="arrow-back" size="2x" />
                </bm-button>
                <h2 class="d-inline align-middle">{{ $t("common.settings") }}</h2>
            </div>
            <div class="text-center mt-4">
                <h1><bm-avatar :alt="user.displayname" /> <br />{{ user.displayname }}</h1>
                <a href="/settings/" class="text-primary font-weight-bold">
                    {{ $t("settings.manage.account") }}
                </a>
            </div>
        </div>

        <nav class="mt-3" :aria-label="$t('settings.menu.apps')">
            <ul class="m-0 p-0">
                <li
                    v-for="app in availableApps"
                    :key="app.href"
                    class="text-primary app-item p-2"
                    role="button"
                    tabindex="0"
                    :class="selectedApp && selectedApp.href === app.href ? 'active' : ''"
                    @click="$emit('change', app)"
                    @keydown.enter="$emit('change', app)"
                >
                    <h2>
                        <bm-app-icon :icon-app="app.icon" class="px-3" />
                        <span class="align-middle">{{ app.name }}</span>
                    </h2>
                </li>
            </ul>
        </nav>
    </bm-col>
</template>

<script>
import { BmButton, BmCol, BmIcon, BmLabelIcon, BmAvatar } from "@bluemind/styleguide";
import BmAppIcon from "../BmAppIcon";

export default {
    name: "BmSettingsLeftSidebar",
    components: {
        BmAvatar,
        BmButton,
        BmIcon,
        BmLabelIcon,
        BmCol,
        BmAppIcon
    },
    props: {
        availableApps: {
            required: true,
            type: Array
        },
        selectedApp: {
            type: Object,
            default: null
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

.bm-settings-left-sidebar .app-item {
    border-bottom: 0 !important;
    background-color: $info-dark;
    list-style: none;

    &:focus,
    &:hover,
    &.active {
        background-color: darken($info-dark, 10%);
    }
}

.bm-settings-left-sidebar {
    background-color: $info-dark;

    .bm-avatar {
        font-size: 3rem;
    }
    .bm-label-icon {
        color: $white;
    }
    .list-group-item.active {
        border: none !important;
        background-color: darken($info-dark, 10%) !important;
        color: $primary;
    }
}
</style>
