var BmIMWidget = {
    name: "BmIMWidget",
    template:
        '<button type="button" v-if="show" class="btn btn-inline-on-fill-primary" @click="showIm"><svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="phone" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12" class="svg-inline--fa fa-phone fa-w-16 fa-lg"><path fill="currentColor" d="M6 1C4.20917 1 2.71193 1.3679 1.65071 2.1617C0.566887 2.9724 0 4.18103 0 5.70471C0 7.38013 0.696467 8.64092 1.95132 9.43022L1.6554 11.4267C1.62685 11.6193 1.71272 11.811 1.87546 11.9179C2.0382 12.0248 2.2482 12.0275 2.41365 11.9248L4.94341 10.3549C5.28551 10.3876 5.63823 10.4007 6 10.4007C7.78634 10.4007 9.2833 10.0327 10.3455 9.23918C11.4301 8.42886 12 7.22045 12 5.69599C12 4.17138 11.43 2.96486 10.3449 2.15671C9.28262 1.3656 7.78576 1 6 1ZM1 5.70471C1 4.45433 1.45004 3.5606 2.24968 2.96247C3.07193 2.34742 4.32468 2 6 2C7.67084 2 8.92398 2.34536 9.74757 2.95873C10.5483 3.55506 11 4.44653 11 5.69599C11 6.94558 10.5482 7.83953 9.74698 8.43805C8.9233 9.05339 7.67026 9.4007 6 9.4007C5.60298 9.4007 5.23172 9.38379 4.88369 9.34303C4.77139 9.32988 4.65795 9.35517 4.56187 9.41479L2.80272 10.5065L2.99026 9.24124C3.02128 9.03196 2.91722 8.8258 2.73042 8.72647C1.62077 8.13637 1 7.15876 1 5.70471ZM3.5 6.5C3.77614 6.5 4 6.27614 4 6C4 5.72386 3.77614 5.5 3.5 5.5C3.22386 5.5 3 5.72386 3 6C3 6.27614 3.22386 6.5 3.5 6.5ZM5.5 6.5C5.77614 6.5 6 6.27614 6 6C6 5.72386 5.77614 5.5 5.5 5.5C5.22386 5.5 5 5.72386 5 6C5 6.27614 5.22386 6.5 5.5 6.5ZM8 6C8 6.27614 7.77614 6.5 7.5 6.5C7.22386 6.5 7 6.27614 7 6C7 5.72386 7.22386 5.5 7.5 5.5C7.77614 5.5 8 5.72386 8 6Z" class=""></path></svg></button>',
    data: function () {
        return { show: window.bmcSessionInfos.roles.split(',').includes('hasIM') };
    },
    methods: {
        showIm: function () {
            var p = window.open(
                "",
                "IM",
                "height=500, width=700, top=100, left=100, toolbar=no, menubar=no, location=no, resizable=yes, scrollbars=no, status=no"
            );
            if (p.location == "about:blank") {
                p.location.href = "../im/#";
                p.focus();
            } else {
                p.focus();
            }
            localStorage.removeItem("bm-unread");
            return false;
        },
    },
};
window.Vue && window.Vue.component("BmIMWidget", BmIMWidget);
