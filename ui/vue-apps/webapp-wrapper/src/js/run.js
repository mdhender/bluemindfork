import Vue from "vue";

import router from "@bluemind/router";
import ApplicationWrapper from "./ApplicationWrapper.vue";

Vue.component("application-wrapper", ApplicationWrapper);
router.addRoutes([
    { path: "/", redirect: "/index.html" },
    {
        name: "wrapper:root",
        path: "/index.html",
        component: ApplicationWrapper
    }
]);
