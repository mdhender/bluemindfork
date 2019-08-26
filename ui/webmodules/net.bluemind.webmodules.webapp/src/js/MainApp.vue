<template>
    <div class="main-app d-flex flex-column vh-100 bg-light">
        <bm-banner :applications="applications" :widgets="widgets" :user="user" :software="software" />
        <router-view />
    </div>
</template>

<script>
import BmBanner from "@bluemind/banner.ui.vuejs/components/BmBanner";
import CommonL10N from "@bluemind/l10n";
import "@bluemind/styleguide/css/bluemind.scss";
import injector from "@bluemind/inject";

export default {
    components: {
        BmBanner
    },
    i18n: { messages: CommonL10N },
    data() {
        const data = {};
        data.applications = [];
        data.widgets = [];
        window.bmExtensions_["net.bluemind.banner"].map(function(extension) {
            if (extension.application) {
                const entry = extension.application;
                data.applications.push({
                    icon: {
                        name: entry.children['icon-name'] && entry.children['icon-name'].body,
                        svg: entry.children['icon-svg'] && entry.children['icon-svg'].body,
                        url: entry.children['icon-url'] && entry.children['icon-url'].body
                    },
                    href: entry.href.match(/^\/webapp/)
                        ? entry.href.replace("/webapp", "")
                        : entry.href,
                    external: !entry.href.match(/^\/webapp/),
                    name: entry.name,
                    description: entry.description,
                    order: entry.order
                });
            }
            if (extension.widget) {
                data.widgets.push(extension.widget);
            }
            if (extension.notification) {
                data.notifications.push(extension.notification);
            }
        });
        data.applications.sort((a, b) => b.order - a.order);
        data.widgets.sort((a, b) => b.order - a.order);

        const userSession = injector.getProvider("UserSession").get();
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
    }
};
</script>