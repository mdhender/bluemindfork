const path = require("path");
const merge = require("webpack-merge");
const prod = require("./node_modules/@bluemind/conf/webpack.prod.js");
const dev = require("./node_modules/@bluemind/conf/webpack.dev.js");

const myConf = {
    entry: "./src/js/run.js",
    output: {
        path: path.resolve(__dirname, "./web-resources/js/compile"),
        filename: "net.bluemind.webmodules.webapp.js"
    }
};

module.exports = mode => {
    if (mode === "production") {
        return merge(prod, myConf);
    }
    return merge(dev, myConf);
};
