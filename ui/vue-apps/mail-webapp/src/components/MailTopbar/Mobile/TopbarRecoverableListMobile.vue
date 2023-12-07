<script setup>
import { BmNavbar, BmNavbarBack, BmNavbarTitle } from "@bluemind/ui-components";
const route = { name: "v:mail:home", params: { filter: null } };
</script>

<template>
    <bm-navbar class="topbar-recoverable-list-mobile">
        <bm-navbar-back :to="$router.relative(route, $route)" />
        <bm-navbar-title :title="$t('mail.list.recoverable.back')" />
    </bm-navbar>
</template>
