'use strict';

import React from 'react'
//import PropTypes from "prop-types"

import { scaleTime } from 'd3-scale'
//import { utcDay } from "d3-time";
import { format } from "d3-format";
import { timeFormat } from "d3-time-format";

import { ChartCanvas, Chart } from 'react-stockcharts'
import { LineSeries, AreaSeries } from 'react-stockcharts/lib/series'
import { XAxis, YAxis } from 'react-stockcharts/lib/axes'
//import { fitWidth } from "react-stockcharts/lib/helper";
import { last } from 'react-stockcharts/lib/utils'
import { createVerticalLinearGradient, hexToRGBA } from 'react-stockcharts/lib/utils'
import { curveMonotoneX } from "d3-shape";
import { sma } from "react-stockcharts/lib/indicator";
import {
	CrossHairCursor,
	EdgeIndicator,
	CurrentCoordinate,
	MouseCoordinateX,
	MouseCoordinateY,
} from "react-stockcharts/lib/coordinates";


const canvasGradient = createVerticalLinearGradient([
	{ stop: 0, color: hexToRGBA("#b5d0ff", 0.2) },
	{ stop: 0.7, color: hexToRGBA("#6fa4fc", 0.4) },
	{ stop: 1, color: hexToRGBA("#4286f4", 0.8) },
]);



 const GraphCanvas = (props) => {
    
    //const { type, width, data, ratio } = props;
    const sma20 = sma()
    .options({ windowSize: 20 })
    .merge((d, c) => { d.sma20 = c; })
    .accessor(d => d.sma20)
    .stroke("red");

    const calculatedData  = sma20(props.data);
    const xAccessor = d => d.date;
    const xExtents = [
      xAccessor(last(calculatedData)),
      xAccessor(calculatedData[calculatedData.length - 100])
    ];
    
    return (
<ChartCanvas ratio={props.ratio} width={props.width} height={600}
        margin={{ left: 60, right: 70, top:10, bottom: 30 }}
        seriesName="MSFT"
        data={calculatedData} type="svg"
        xAccessor={xAccessor} xScale={scaleTime()}
        displayXAccessor={xAccessor}
        xExtents={xExtents}>
    <CrossHairCursor stroke="#00000000"/>
    <Chart id={0} yExtents={ [ (d) => d.close, sma20.accessor() ] }>
        <XAxis axisAt="bottom" orient="bottom" ticks={6}/>
		<YAxis axisAt="left" orient="left" />
        <MouseCoordinateX rectWidth={70} at="bottom" orient="bottom" displayFormat={timeFormat("%Y:%m:%d")} />
        <MouseCoordinateY rectWidth={80} at="right" orient="right" displayFormat={format(".2f")}/>
        <EdgeIndicator rectWidth={80} itemType="last" orient="right" edgeAt="right" yAccessor={d => d.close} fill={d => d.close > d.open ? "#6BA583" : "#ff0000"} />                        
        <CurrentCoordinate yAccessor={d => d.close} fill="#0a1d9b" />
        <AreaSeries yAccessor={(d) => d.close} strokeWidth={2} interpolation={curveMonotoneX} canvasGradient={canvasGradient}/>
        <LineSeries yAccessor={sma20.accessor()} stroke={sma20.stroke()}/>
    </Chart>
</ChartCanvas>
    );  
  }
  
export default GraphCanvas;