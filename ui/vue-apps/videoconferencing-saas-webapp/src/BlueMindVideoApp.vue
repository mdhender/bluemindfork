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
import VideoAppL10N from "../l10n";

export default {
    name: "BlueMindVideoApp",
    components: {
        Jitsi
    },
    componentI18N: { messages: VideoAppL10N },
    data() {
        return {
            shown: false,
            defaultOptions: {
                configOverwrite: {
                    disableThirdPartyRequests: true,
                    subject: "BlueMind.Vidéo"
                },
                interfaceConfigOverwrite: {}
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
        this.setTitle();
        this.enterConference();
    },
    methods: {
        setTitle(title) {
            if (title) {
                document.title = "BlueMind.Vidéo - " + title;
            } else {
                document.title = "BlueMind.Vidéo";
            }
        },
        enterConference() {
            this.error = "";

            const tokenService = inject("VideoConferencingService");
            let room = this.currentRoom();

            tokenService.token(room).then(
                resp => {
                    this.error = resp.error;
                    this.meetOptions = {
                        ...this.defaultOptions,
                        jwt: resp.token,
                        roomName: room
                    };
                    if (resp.roomTitle) {
                        this.meetOptions.configOverwrite.subject = resp.roomTitle;
                        this.setTitle(resp.roomTitle);
                    }
                    // Not an authenticated user with JWT token, try to disable
                    // the display of the invite function. (Which would display the wrong address)
                    if (!resp.token) {
                        this.meetOptions.configOverwrite.disableInviteFunctions = true;
                    }
                    this.shown = resp.error ? false : true;
                },
                error => {
                    this.error = error;
                    this.shown = false;
                }
            );
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
.video-app {
    height: calc(100% - 40px);
}
</style>
