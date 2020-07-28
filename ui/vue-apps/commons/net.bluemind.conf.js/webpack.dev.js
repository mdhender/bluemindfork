const merge = require("webpack-merge");
const common = require("./webpack.common.js");

module.exports = merge(common, {
    mode: "development",
    devServer: {
        historyApiFallback: true,
        overlay: true,
        port: 9180,
        host: "0.0.0.0",
        disableHostCheck: true
    },
    devtool: "#eval-source-map"
});
