<template>
    <div class="main-app d-flex flex-column vh-100 bg-light">
        <bm-banner :applications="applications" :widgets="widgets" :user="user" :software="software" />
        <bm-spinner v-if="appState == 'loading'" :size="2" class="d-flex flex-fill align-self-center" />
        <div
            v-else-if="appState == 'error'"
            class="text-danger text-center h2 d-flex flex-fill align-self-center align-items-center"
        >
            {{ $t("common.application.bootstrap.error") }}<br />
            {{ $t("common.application.bootstrap.error.solution") }}
        </div>
        <router-view v-else />
        <bm-application-alert :alerts="applicationAlerts" class="z-index-250 position-absolute">
            <template v-slot="slotProps">
                <component :is="slotProps.alert.renderer" :alert="slotProps.alert" />
            </template>
        </bm-application-alert>
    </div>
</template>

<script>
import { BmApplicationAlert, BmSpinner } from "@bluemind/styleguide";
import { mapState } from "vuex";
import "@bluemind/styleguide/css/bluemind.scss";
import CommonL10N from "@bluemind/l10n";
import injector from "@bluemind/inject";
import BmBanner from "./BmBanner";

export default {
    components: {
        BmApplicationAlert,
        BmBanner,
        BmSpinner
    },
    componentI18N: { messages: CommonL10N },
    data() {
        const data = {};
        data.applications = [];
        data.widgets = [];
        window.bmExtensions_["net.bluemind.banner"].map(function(extension) {
            if (extension.application) {
                const entry = extension.application;
                data.applications.push({
                    icon: {
                        name: entry.children["icon-name"] && entry.children["icon-name"].body,
                        svg: entry.children["icon-svg"] && entry.children["icon-svg"].body,
                        url: entry.children["icon-url"] && entry.children["icon-url"].body
                    },
                    href: entry.href.match(/^\/webapp/) ? entry.href.replace("/webapp", "") : entry.href,
                    external: !entry.href.match(/^\/webapp/),
                    name: entry.name,
                    description: entry.description,
                    order: entry.order,
                    role: entry.role
                });
            }
            if (extension.widget) {
                data.widgets.push(extension.widget);
            }
            if (extension.notification) {
                data.notifications.push(extension.notification);
            }
        });

        const userSession = injector.getProvider("UserSession").get();

        data.applications = data.applications
            .filter(app => userSession.roles.includes(app.role))
            .sort((a, b) => b.order - a.order);
        data.widgets.sort((a, b) => b.order - a.order);

        const user = {
            displayname: userSession["formatedName"],
            email: userSession["defaultEmail"]
        };
        const software = {
            version: userSession["bmVersion"],
            brand: userSession["bmBrandVersion"]
        };
        data.user = user;
        data.software = software;
        return data;
    },
    computed: {
        ...mapState({ applicationAlerts: state => state.alert.applicationAlerts }),
        ...mapState("root-app", ["appState"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.main-app .bm-application-alert {
    bottom: $sp-4;
    left: $sp-4;
}
</style>
