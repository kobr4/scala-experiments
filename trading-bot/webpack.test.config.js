const path = require('path');

let entry = './js/test/firsttest.js';
let outputPath = path.join(__dirname, "/src/test/assets/");


module.exports = {
  mode: "development",
  context: path.join(__dirname, '/src/main/web'),
  entry: entry,
  output: {
    path: outputPath,
    filename: 'MainTest.js'
  },
  resolve: {
    extensions: ['.js', '.jsx']
  },
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        loader: 'babel-loader',
        exclude: /node_modules/
      },
      {
        test: /\.css$/,
        use: [ 'style-loader', 'css-loader' ]
      }
    ]
  }
};