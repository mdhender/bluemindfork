<template>
    <div ref="visioContainer" style="height: 100vh; width: 100%"></div>
</template>

<script>
export default {
    name: "Jitsi",
    props: {
        domain: {
            type: String,
            default: "visio.bluemind.net"
        },
        options: {
            type: Object,
            default: () => ({})
        }
    },
    mounted() {
        this.load("https://" + this.domain + "/external_api.js", () => {
            if (!window.JitsiMeetExternalAPI) {
                throw new Error("Jitsi Meet API not loaded");
            }
            this.embedJitsiWidget();
        });
    },
    beforeDestroy() {
        this.removeJitsiWidget();
    },
    methods: {
        load(src, cb) {
            const el = document.createElement("script");
            el.async = 1;
            el.src = src;
            document.head.appendChild(el);
            el.addEventListener("load", cb);
        },
        embedJitsiWidget() {
            const options = {
                ...this.options,
                parentNode: this.$refs.visioContainer
            };
            this.jitsiApi = new window.JitsiMeetExternalAPI(this.domain, options);
        },
        executeCommand(command, ...value) {
            this.jitsiApi.executeCommand(command, ...value);
        },
        addEventListener(event, fn) {
            this.jitsiApi.on(event, fn);
        },
        removeJitsiWidget() {
            if (this.jitsiApi) {
                this.jitsiApi.dispose();
            }
        }
    }
};
</script>
