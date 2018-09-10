'use strict';

function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td>{props.value}</td></tr>;
}

class ApiResponse extends React.Component {

  constructor(props) {
    super(props);
    this.state = { responseFields : [] }
  }

  updateState(fields) {
    this.setState( { responseFields : fields });
  }

  updateTimed(target) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
         if (this.readyState == 4 && this.status == 200) {
             var jsonResponse = JSON.parse(this.responseText)
             var fields = [];
             Object.keys(jsonResponse.result).forEach(function (key) {
               var value = JSON.stringify(jsonResponse.result[key]);
                var field = <ApiResponseField name={key} value={value} key={key} />
                fields.push(field);
             });
             target.updateState(fields);
         }
    };
    xhttp.open("GET", "http://localhost:8080/btc-api/getblockchaininfo", true);
    xhttp.setRequestHeader("Content-type", "application/json");
    xhttp.send();
  }

  componentDidMount() {
    this.interval = setInterval(() => this.updateTimed(this), 5000);
  }

  render() {
    return (
       <table>
       <tbody>
       <tr><th>Name</th><th>Value</th></tr>
       {
         this.state.responseFields.map(field => field)
       }
       </tbody>
       </table>
    );
  }
}

ReactDOM.render(
  <ApiResponse />,
  document.getElementById('toto')
);



