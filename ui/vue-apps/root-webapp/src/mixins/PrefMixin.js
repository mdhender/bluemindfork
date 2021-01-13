export default {
    methods: {
        scrollTo(id) {
            document.getElementById(id).scrollIntoView();
        },

        sectionId(appCode, categoryCode) {
            return "section-" + appCode + (categoryCode ? "-" + categoryCode : "");
        },

        sectionPath(appCode, categoryCode) {
            return "#" + this.sectionId(appCode, categoryCode);
        }
    }
};
