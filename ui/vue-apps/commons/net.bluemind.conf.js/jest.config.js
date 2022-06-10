const path = require("path");

module.exports = {
    rootDir: path.resolve(__dirname, "../../"),
    projects: [
        "<rootDir>/mail-webapp/jest.config.js",
        "<rootDir>/root-webapp/jest.config.js",
        "<rootDir>/commons/net.bluemind.extensions.vue.js",
        "<rootDir>/commons/net.bluemind.extensions.js"
    ]
};
