<template>
    <main v-if="!anonymousPage" class="flex-fill d-lg-flex flex-column">
        <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
        <jitsi v-if="shown" :domain="domain" :options="meetOptions"></jitsi>
    </main>
    <main v-else class="flex-fill d-lg-flex flex-column justify-content-center">
        <bm-row id="anonymouschoice">
            <bm-col class="centered">
                <bm-button variant="primary" @click="redirectLogin">{{ $t("visio.ask_login") }}</bm-button>
            </bm-col>
            <bm-col class="centered">
                <bm-button variant="secondary" @click="enterAnonymous">{{ $t("visio.join_as_guest") }}</bm-button>
            </bm-col>
        </bm-row>
    </main>
</template>
<script>
import { inject } from "@bluemind/inject";
import Jitsi from "./components/Jitsi.vue";
import router from "@bluemind/router";
import { BmButton, BmCol, BmRow } from "@bluemind/styleguide";
import VisioAppL10N from "../l10n/";

export default {
    name: "BlueMindVisioApp",
    components: {
        Jitsi,
        BmButton,
        BmCol,
        BmRow
    },
    componentI18N: { messages: VisioAppL10N },
    data() {
        return {
            shown: false,
            anonymousPage: true,
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
        this.anonymousPage = !userSession.userId;
        if (userSession.userId) {
            this.enterAuthenticated();
        }
    },
    methods: {
        redirectLogin() {
            window.location.href = "/login/index.html?askedUri=/visio/" + this.currentRoom();
        },
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
