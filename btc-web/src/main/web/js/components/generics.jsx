'use strict';

import React from 'react'
import ReactDOM from 'react-dom'

export function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td><pre>{props.value}</pre></td></tr>;
}

export function ResponseTable(props) {
  return (
       <table className="table table-bordered table-hover">
       <tbody>
       <tr><th>Name</th><th>Value</th></tr>
       {
         props.responseFields.map(field => field)
       }
       </tbody>
       </table>
  );
}

export function FormRadioField(props) {
  return <span><input type="radio" name={props.name} value={props.value} checked={props.current === props.value} onChange={props.handleRadioChange}/>{props.value}</span>;
}

export function FormRadioFieldList(props) {
    var rlist = []
    props.values.map(value => {
        var radioField = <FormRadioField name={props.name} value={value} handleRadioChange={props.handleRadioChange} current={props.current} key={props.name+value}/>;
        rlist.push(radioField);
    });
    return <label>{props.name}:{rlist}</label>;
}

export function FormListField(props) {
  return <label>{props.name}<input type="text" value={props.value} name={props.name} onChange={props.handleTextChange} /></label>;
}

export function FormContainer(props) {
  return (
     <form onSubmit={props.handleSubmit}>
        {props.children}
       <input type="submit" value="Submit" />
     </form>
  );
}