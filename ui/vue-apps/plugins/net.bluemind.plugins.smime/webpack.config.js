const path = require("path");
const { merge } = require("webpack-merge");
const prod = require("./node_modules/@bluemind/conf/webpack.prod.js");
const dev = require("./node_modules/@bluemind/conf/webpack.dev.js");

const conf = {
    entry: {
        "js/net.bluemind.smime": "./src/index.js",
        "smime.service-worker": "./src/service-worker/service-worker.js"
    },
    output: {
        path: path.resolve(__dirname, "./web-resources"),
        filename: "[name].js"
    }
};

module.exports = mode => {
    if (mode.production) {
        return merge(prod, conf);
    }
    return merge(dev, conf);
};
