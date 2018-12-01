'use strict';

import React from 'react'
//import PropTypes from "prop-types"

import { scaleTime } from 'd3-scale'
//import { utcDay } from "d3-time";

import { ChartCanvas, Chart } from 'react-stockcharts'
import { LineSeries, AreaSeries } from 'react-stockcharts/lib/series'
import { XAxis, YAxis } from 'react-stockcharts/lib/axes'
//import { fitWidth } from "react-stockcharts/lib/helper";
import { last } from 'react-stockcharts/lib/utils'
import { createVerticalLinearGradient, hexToRGBA } from 'react-stockcharts/lib/utils'
import { curveMonotoneX } from "d3-shape";
import { sma } from "react-stockcharts/lib/indicator";

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
        margin={{ left: 50, right: 50, top:10, bottom: 30 }}
        seriesName="MSFT"
        data={calculatedData} type="svg"
        xAccessor={xAccessor} xScale={scaleTime()}
        xExtents={xExtents}>
    <Chart id={0} yExtents={ [ (d) => d.close, sma20.accessor() ] }>
        <XAxis axisAt="bottom" orient="bottom" ticks={6}/>
		<YAxis axisAt="left" orient="left" />
        <AreaSeries yAccessor={(d) => d.close} 
        strokeWidth={2}
        interpolation={curveMonotoneX}
        canvasGradient={canvasGradient}/>
        <LineSeries yAccessor={sma20.accessor()} stroke={sma20.stroke()}/>
    </Chart>
</ChartCanvas>
    );  
  }
  
export default GraphCanvas;