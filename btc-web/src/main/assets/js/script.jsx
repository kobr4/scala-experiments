'use strict';

ReactDOM.render(
  <h1>Hello, world!</h1>,
  document.getElementById('toto')
);

function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td>{props.field}</td></tr>;
}

class ApiResponse extends React.Component {

  constructor(props) {
    super(props);

    this.state = { responseFields = [] }
  }

  render() {
    return (
       <table>
       <tr><th>Name</th><th>Value</th></tr>
       {
         this.state.responseFields.map(field => field)
       }
       </table>
    );
  }
}


