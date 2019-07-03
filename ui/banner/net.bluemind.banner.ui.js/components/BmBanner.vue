<template>
    <bm-navbar type="dark" variant="info-dark" toggleable="lg" class="bm-banner p-0 align-items-stretch">
        <bm-navbar-toggle target="collapse-applications" />
        <bm-navbar-brand :title="$t('banner.main.brand', software)" href="#" class="p-1">
            <img src="images/logo-bluemind.png">
        </bm-navbar-brand>
        <bm-collapse id="collapse-applications" is-nav class="order-1 order-lg-0 align-items-end">
            <bm-banner-applications :applications="applications" />
        </bm-collapse>
        <component :is="widget.component" v-for="widget in widgets" :key="widget.component" />
        <bm-banner-user :user="user" />
    </bm-navbar>
</template>

<script>
import BannerL10N from "@bluemind/banner.l10n";
import BmBannerApplications from "./BmBannerApplications";
import BmBannerUser from "./BmBannerUser";
import BmCollapse from "@bluemind/styleguide/components/BmCollapse";
import BmNavbar from "@bluemind/styleguide/components/navbar/BmNavbar";
import BmNavbarBrand from "@bluemind/styleguide/components/navbar/BmNavbarBrand";
import BmNavbarToggle from "@bluemind/styleguide/components/navbar/BmNavbarToggle";
import BmNavItem from "@bluemind/styleguide/components/nav/BmNavItem";

export default {
    name: "BmBanner",
    components: {
        BmBannerApplications,
        BmBannerUser,
        BmCollapse,
        BmNavbar,
        BmNavbarBrand,
        BmNavItem,
        BmNavbarToggle
    },
    i18n: { messages: BannerL10N },
    props: {
        applications: {
            required: true,
            type: Array
        },
        widgets: {
            required: true,
            type: Array
        },
        software: {
            type: Object,
            required: true
        },
        user: {
            required: true,
            type: Object
        }
    }
};
</script>