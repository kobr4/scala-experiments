'use strict';

function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td>{props.field}</td></tr>;
}

class ApiResponse extends React.Component {

  constructor(props) {
    super(props);

    this.state = { responseFields : [] }
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

ReactDOM.render(
  <ApiResponse />,
  document.getElementById('toto')
);

