import Vue from "vue";

import router from "@bluemind/router";
import ApplicationWrapper from "./ApplicationWrapper.vue";

Vue.component("calendar-wrapper", ApplicationWrapper);
router.addRoutes([
    {
        name: "wrapper:root",
        path: "/index.html",
        component: ApplicationWrapper
    }
]);
