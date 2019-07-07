const path = require('path');

let entry = './js/script.jsx'
let outputPath = path.join(__dirname, '/src/main/public/bundles');

module.exports = {
  mode: "development",
  context: path.join(__dirname, '/src/main/web'),
  entry: entry,
  output: {
    path: outputPath,
    filename: 'bundle.js'
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