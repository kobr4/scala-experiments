'use strict';

import React from 'react'
import ReactDOM from 'react-dom'
import {ApiResponseField} from './generics'

class Helper {

  static rowsFromObjet(jsObj) {
    var fields = [];  
    Object.keys(jsObj).forEach(function (key) {
      fields.push(<tr><td>{key}</td><td>{jsObj[key]}</td></tr>);
    })
    return fields;
  }


  static buildResponseComponent(jsonResponse) {
    var fields = [];
    if (typeof jsonResponse.result === 'string' || typeof jsonResponse.result === 'number') {
       var field = <ApiResponseField name='result' value={jsonResponse.result} key='result' />
       fields.push(field);
    } if (Array.isArray(jsonResponse)) {
     jsonResponse.forEach(function(data){
       var field = <ApiResponseField name={data.date} value= {JSON.stringify(data.order)}/>
       fields.push(field);
     })
  
    } else {
        Object.keys(jsonResponse.result).forEach(function (key) {
          var value = JSON.stringify(jsonResponse.result[key], null, 2);
           var field = <ApiResponseField name={key} value={value} key={key} />
           fields.push(field);
        });
    }
    return fields;
  }
}

export default Helper;