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

    static performGetReqStatus(updateCallback, path, params = []) {
        var xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4) {
                updateCallback(this.status);
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

    static performRestReqWithPromise(path, params = []) {
        return new Promise((resolve, reject) => {
            RestUtils.performRestReq( (jsonResponse) => resolve(jsonResponse), path, params);
        })
    }

    static performRestPostReq(updateCallback, path, params = []) {
        var xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4 && this.status == 200) {
                var jsonResponse = JSON.parse(this.responseText)
                updateCallback(jsonResponse);
            }
        };
        let protocol = location.protocol;
        let slashes = protocol.concat("//");
        let host = slashes.concat(window.location.hostname+(window.location.port==8080?":8080":""));
        let paramObject = new Object();
        params.map(field => paramObject[field[0]] = field[1]);
        let paramsString = JSON.stringify(paramObject);
        xhttp.open("POST", host+path, true);
        xhttp.setRequestHeader("Content-type", "application/json");
        xhttp.send(paramsString);
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
        RestUtils.performRestPriceReq( (jsonResponse) => resolve(jsonResponse), path, params);
    })
  }
    
}

export default RestUtils;
