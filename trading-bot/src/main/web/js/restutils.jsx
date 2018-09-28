'use strict';

class RestUtils {
    static performRestReq(updateCallback, path, params = []) {
        var xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4 && this.status == 200) {
                var jsonResponse = JSON.parse(this.responseText)
                updateCallback(jsonResponse);
            }
        };
        var protocol = location.protocol;
        var slashes = protocol.concat("//");
        var host = slashes.concat(window.location.hostname+(window.location.port==8080?":8080":""));
        var paramsString = params.map(field => field[0]+"="+field[1]+"&").join('')
        xhttp.open("GET", host+path+"?"+paramsString, true);
        xhttp.setRequestHeader("Content-type", "application/json");
        xhttp.send();
    }
  
  static performRestPriceReq(updateCallback, path, params = []) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
         if (this.readyState == 4 && this.status == 200) {
             var jsonResponse = JSON.parse(this.responseText)
             updateCallback(jsonResponse);
         }
    };
    var protocol = location.protocol;
    var slashes = protocol.concat("//");
    var host = slashes.concat(window.location.hostname+(window.location.port==8080?":8080":""));
    var paramsString = params.map(field => field[0]+"="+encodeURIComponent(field[1])+"&").join('')
    xhttp.open("GET", host+path+"?"+paramsString, true);
    xhttp.setRequestHeader("Content-type", "application/json");
    xhttp.send();
  }
  
  static performRestPriceReqWithPromise(path, params = []) {
    return new Promise( (resolve, reject) =>  {
      performRestPriceReq( (jsonResponse) => resolve(jsonResponse), path, params);
    })
  }
    
}

export default RestUtils;
