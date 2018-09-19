'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch, Layout } from 'react-router-dom'

function HelloWorld(props) {
    return <div>Hello World !</div>;
}

ReactDOM.render(<HelloWorld />,
  document.getElementById('toto')
);