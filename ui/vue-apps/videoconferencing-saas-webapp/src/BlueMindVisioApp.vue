<template>
    <main class="flex-fill d-lg-flex flex-column">
        <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
        <jitsi v-if="shown" :domain="domain" :options="meetOptions"></jitsi>
    </main>
</template>
<script>
import { inject } from "@bluemind/inject";
import Jitsi from "./components/Jitsi.vue";
import router from "@bluemind/router";
import VisioAppL10N from "../l10n/";

export default {
    name: "BlueMindVisioApp",
    components: {
        Jitsi
    },
    componentI18N: { messages: VisioAppL10N },
    data() {
        return {
            shown: false,
            defaultOptions: {
                configOverwrite: {
                    disableThirdPartyRequests: true
                }
            },
            meetOptions: {},
            error: ""
        };
    },
    computed: {
        domain() {
            return window.BMJitsiSaasDomain;
        }
    },
    mounted() {
        const userSession = inject("UserSession");
        if (userSession.userId) {
            this.enterAuthenticated();
        } else {
            this.enterAnonymous();
        }
    },
    methods: {
        enterAnonymous() {
            this.anonymousPage = false;
            this.meetOptions = {
                ...this.defaultOptions,
                roomName: this.currentRoom()
            };
            this.shown = true;
        },
        enterAuthenticated() {
            const tokenService = inject("VideoConferencingService");
            let room = this.currentRoom();
            tokenService.token(room).then(resp => {
                this.error = resp.error;
                this.meetOptions = {
                    ...this.defaultOptions,
                    jwt: resp.token,
                    roomName: room
                };
                this.shown = resp.error ? false : true;
            });
        },
        currentRoom() {
            let route = router.currentRoute;
            if (route) {
                return route.params.room;
            } else {
                return "";
            }
        }
    }
};
</script>

<style>
.visio-app {
    height: calc(100% - 40px);
}

#anonymouschoice button {
    font-size: 2rem;
}

.centered {
    text-align: center;
}
</style>
