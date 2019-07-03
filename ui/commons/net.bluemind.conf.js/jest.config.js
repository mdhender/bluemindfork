module.exports = {
    rootDir: process.cwd()+"/",
    verbose: true,
    moduleFileExtensions: ["js", "json", "vue"],
    transform: {
        "^.+\\.js$": "babel-jest",
        ".+\\.(css|styl|less|sass|scss|png|jpg|ttf|woff|woff2)$": "jest-transform-stub",
        ".*\\.(vue)$": "vue-jest"
    },
    transformIgnorePatterns: [
        "/node_modules/(?!@bluemind/email|@bluemind/html-utils|@bluemind/date|@bluemind/i18n)",
    ],
    testURL: "http://localhost"
};
