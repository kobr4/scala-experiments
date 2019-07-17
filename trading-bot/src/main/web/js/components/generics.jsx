'use strict';

import React from 'react'
import ReactDOM from 'react-dom'

export function ApiResponseField(props) {
  return <tr><td>{props.name}</td><td><pre>{props.value}</pre></td></tr>;
}

export function ApiResponseSpanField(props) {
  return <tr><td colSpan='2'>{props.value}</td></tr>;
}

export function ResponseTable(props) {
  return (
       <table className="table table-bordered table-hover">
       <tbody>
       <tr><th>{props.first}</th><th>{props.second}</th></tr>
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

export function FormTextField(props) {
  return <input type="text" value={props.value} className="form-control" placeholder={props.placeholder} name={props.name} onChange={props.handleTextChange} />;
}

export function FormEmailField(props) {
  return <input type="email" value={props.value} className="form-control form-control-lg" placeholder={props.placeholder} name={props.name} onChange={props.handleTextChange} />;
}


export function FormPasswordField(props) {
  return <input type="password" value={props.value} className="form-control" placeholder={props.placeholder} name={props.name} onChange={props.handleTextChange} />;
}

export function FormInputField(props) {
  return (
    <input type="submit" className="btn btn-primary" value={props.submit} />
  );
}

export function FormButton(props) {
  return <button onClick={(event) => {event.preventDefault();props.handleClick(event)}}>{props.text}</button>
}

export function FormOption(props) {
  return (
    <select name={props.name} onChange={props.onChange} value={props.value}>
    {
      props.values.map(value => <option value={value[0]} key={value[0]}>{value[1]}</option>)
    }
    </select>
  )
}


export function FormContainer(props) {
  return (
     <form onSubmit={props.handleSubmit}>
        {props.children}
        <input type="submit" class="btn btn-primary" value={props.submit} />
     </form>
  );
}

export function FormTable(props) {
  return (
    <table>
      <tbody>
      {props.children}
      </tbody>
    </table>
  );
}

export function FormGroup(props) {
  return (
    <div className="form-group">{props.children}</div>
  );
}

export function FormRow(props) {
  return (
    <tr>{ props.label && <td>{props.label}</td>}<td>{props.children}</td></tr>
  );
}

export function Panel(props) {
  return (
    <div className='row'>
      <div className='card w-100 m-3'>
        <div className='card-header'>
          {props.title}
        </div>
        <div className='card-body'>
          {props.children}
        </div>
      </div>
    </div>
  );
}

export function PanelTable(props) {
  var headers = [];
  
  if (props.headers)
    for (let h of props.headers) {
    headers.push(<th>{h}</th>);
  }

  return (
    <div className='table-responsive'>
    <table className='table table-striped table-bordered table-hover'>
    { headers.length > 0 &&
      <thead>
        <tr>
          {headers}
        </tr>
      </thead>
    }
    {props.children}
    </table>
    </div>
  );
}