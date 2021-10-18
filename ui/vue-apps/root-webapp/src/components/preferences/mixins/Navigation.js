export default {
    methods: {
        scrollTo(id) {
            document.getElementById(toPath(typeof id === "object" ? id.id : id))?.scrollIntoView();
        },
        anchor(zone, link = false) {
            return (link ? "#" : "") + toPath(zone.id);
        }
    }
};

const toPath = id => `preferences-${id.replaceAll(".", "-")}`;
