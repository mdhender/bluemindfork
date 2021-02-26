export default {
    methods: {
        scrollTo(id) {
            document.getElementById(id).scrollIntoView();
        },

        categoryId(sectionCode, categoryCode) {
            return "preferences-" + sectionCode + (categoryCode ? "-" + categoryCode : "");
        },

        sectionId(section) {
            return this.categoryId(section.code, section.categories[0].code);
        },

        categoryPath(sectionCode, categoryCode) {
            return "#" + this.categoryId(sectionCode, categoryCode);
        },

        sectionPath(section) {
            return this.categoryPath(section.code, section.categories[0].code);
        }
    }
};
