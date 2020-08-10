<template>
    <div class="bm-settings position-absolute w-100 h-100 px-5 py-3">
        <bm-container fluid class="h-100 bg-white">
            <bm-row class="h-100">
                <bm-col class="bm-settings-left-sidebar text-white" cols="2">
                    <div class="p-3">
                        <h2>
                            <bm-label-icon icon="preferences">{{ $t("common.settings") }} </bm-label-icon>
                        </h2>
                        <div class="text-center mt-4">
                            <h1><bm-avatar :alt="user.displayname" /> <br />{{ user.displayname }}</h1>
                            <a href="/settings/" class="text-primary font-weight-bold">
                                {{ $t("settings.manage.account") }}
                            </a>
                        </div>
                    </div>

                    <bm-list-group class="mt-3">
                        <bm-list-group-item
                            v-for="app in availableApps"
                            :key="app.href"
                            class="text-primary bg-transparent"
                            active
                        >
                            <h2>
                                <bm-banner-app-icon :icon-app="app.icon" class="px-3" />
                                <span class="align-middle">{{ app.name }}</span>
                            </h2>
                        </bm-list-group-item>
                    </bm-list-group>
                </bm-col>
                <bm-col cols="10" class="d-flex flex-column">
                    <bm-button-close class="align-self-end mt-3 mx-3" @click="$emit('close')" />
                    <div class="bm-settings-navbar">
                        <bm-banner-app-icon :icon-app="selectedApp.icon" class="px-4 d-inline-block pt-2" />
                        <h2 class="border-bottom border-primary d-inline-block text-primary py-2">
                            <bm-label-icon icon="wrench" icon-size="lg"> {{ $t("common.general") }} </bm-label-icon>
                        </h2>
                    </div>
                    <div class="border-bottom border-secondary" />
                    <div class="pl-5 py-4">
                        <h2 class="pb-4">{{ $t("settings.mail.thread") }}</h2>
                        <div class="d-flex">
                            <label class="d-flex flex-column">
                                <!-- eslint-disable-next-line vue/no-v-html -->
                                <svg v-html="threadSettingImageOn" />
                                <input
                                    type="radio"
                                    name="thread"
                                    class="my-2"
                                    :aria-label="$t('settings.mail.thread.enable')"
                                />
                                <div class="text-center">{{ $t("settings.mail.thread.enable") }}</div>
                            </label>
                            <label class="d-flex flex-column">
                                <!-- eslint-disable-next-line vue/no-v-html -->
                                <svg v-html="threadSettingImageOff" />
                                <input
                                    type="radio"
                                    class="my-2"
                                    name="thread"
                                    :aria-label="$t('settings.mail.thread.disable')"
                                />
                                <div class="text-center">{{ $t("settings.mail.thread.disable") }}</div>
                            </label>
                        </div>
                    </div>
                    <div class="d-flex mt-auto pl-5 py-3 border-top border-secondary">
                        <bm-button type="submit" variant="primary" :disabled="!hasChanged" @click.prevent="doSave">
                            {{ $t("common.save") }}
                        </bm-button>
                        <bm-button variant="simple-dark" class="ml-3" :disabled="!hasChanged" @click.prevent="doCancel">
                            {{ $t("common.cancel") }}
                        </bm-button>
                    </div>
                </bm-col>
            </bm-row>
        </bm-container>
    </div>
</template>

<script>
import threadSettingImageOn from "../../assets/setting-thread-on.svg";
import threadSettingImageOff from "../../assets/setting-thread-off.svg";
import SettingsL10N from "../../l10n/settings/";
import {
    BmButton,
    BmButtonClose,
    BmContainer,
    BmCol,
    BmLabelIcon,
    BmRow,
    BmAvatar,
    BmListGroupItem,
    BmListGroup
} from "@bluemind/styleguide";

import BmBannerAppIcon from "./BmBannerAppIcon";

export default {
    name: "BmSettings",
    components: {
        BmButton,
        BmButtonClose,
        BmContainer,
        BmCol,
        BmLabelIcon,
        BmRow,
        BmAvatar,
        BmListGroupItem,
        BmListGroup,
        BmBannerAppIcon
    },
    props: {
        applications: {
            required: true,
            type: Array
        },
        user: { required: true, type: Object }
    },
    componentI18N: { messages: SettingsL10N },
    data() {
        return {
            selectedApp: this.applications.find(app => app.href === "/mail/"),
            threadSettingImageOn,
            threadSettingImageOff
        };
    },
    computed: {
        availableApps() {
            return this.applications.filter(app => app.href === "/mail/");
        },
        hasChanged() {
            // TODO
            return true;
        }
    },
    mounted() {
        const selector = document.querySelector(".bm-settings-navbar .bm-banner-app-icon svg");
        selector.setAttribute("height", 21);
        selector.setAttribute("width", 30);
    },
    methods: {
        doSave() {
            //TODO
        },
        doCancel() {
            // TODO
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.bm-settings {
    z-index: 500;
    background-color: rgba(47, 47, 47, 0.15);

    .bm-settings-left-sidebar {
        background-color: $info-dark;

        .bm-label-icon {
            color: $white;
        }
        .list-group-item.active {
            border: none !important;
            background-color: darken($info-dark, 10%) !important;
            color: $primary;
        }
    }
    .bm-avatar {
        font-size: 3rem;
    }
    .bm-settings-navbar > * {
        border-bottom-width: 3px !important;
    }
    .bm-settings-navbar .bm-banner-app-icon svg {
        height: 2rem;
    }
}

//checkboxes
.bm-settings {
    .custom-control-label::before,
    .custom-control-label::after {
        position: initial;
    }

    .custom-control-label {
        display: flex;
        flex-direction: column;
        align-items: center;
    }
}
</style>
