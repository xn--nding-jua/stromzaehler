/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.phoenixstudios.stromzaehler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.graphics.Color;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * Display meter values.
 */
public class MeterTCPUpdaterTask extends TCPUpdaterTask
{
   // Definitions for graph
   private boolean ResetGraph = true;
   private boolean DoXScaleScroll = true;
   private final LineGraphSeries<DataPoint> value_180;
   private final LineGraphSeries<DataPoint> value_280;
   private final LineGraphSeries<DataPoint> power;

   public MeterTCPUpdaterTask(final Activity activity)
   {
      super(activity);

      // styling series
      value_180 = new LineGraphSeries<>();
      value_280 = new LineGraphSeries<>();
      power = new LineGraphSeries<>();

      value_180.setTitle("1.8.0 [kWh]");
      value_280.setTitle("2.8.0 [kWh]");
      power.setTitle("Leistung [kW]");

      value_180.setColor(Color.rgb(255,0,0));
      value_180.setDrawDataPoints(false);
      value_180.setDataPointsRadius(10);
      value_180.setThickness(8);
      value_180.setDrawBackground(false); // has problems with negative values
      value_180.setBackgroundColor(Color.argb(100, 255,0,0));

      value_280.setColor(Color.rgb(0,192,0));
      value_280.setDrawDataPoints(false);
      value_280.setDataPointsRadius(10);
      value_280.setThickness(8);
      value_280.setDrawBackground(false); // has problems with negative values
      value_280.setBackgroundColor(Color.argb(100, 0,192,0));

      power.setColor(Color.rgb(0,0,0));
      power.setDrawDataPoints(false);
      power.setDataPointsRadius(10);
      power.setThickness(8);

      /*
      series1.setDrawBackground(true);
      series1.setAnimated(true);
      series1.setColor(Color.GREEN);
      series1.setDrawDataPoints(true);
      series1.setDataPointsRadius(10);
      series1.setThickness(8);
      */

      /*
      // custom paint to make a dotted line
      Paint paint = new Paint();
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(10);
      paint.setPathEffect(new DashPathEffect(new float[]{8, 5}, 0));
      series1.setCustomPaint(paint);
      */
      GraphView graph = (GraphView) activity.findViewById(R.id.graph);

      graph.addSeries(power);
      graph.addSeries(value_180);
      graph.addSeries(value_280);

/*
      graph.getSecondScale().addSeries(Voltage1);
      graph.getSecondScale().addSeries(Voltage2);
      graph.getSecondScale().addSeries(Voltage3);
*/

      graph.getLegendRenderer().setVisible(true);
      //graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP); // Legend on the upper right site
      graph.getLegendRenderer().setFixedPosition(0,0); // legend on the upper left side


      // set date label formatter
      //DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
      SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
      graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt));
      graph.getGridLabelRenderer().setNumHorizontalLabels(6); // only 6 vertical lines because of the space
      graph.getGridLabelRenderer().setNumVerticalLabels(8); // 8 horizontal lines
      graph.getGridLabelRenderer().setHumanRounding(true);
      //graph.getGridLabelRenderer().setVerticalAxisTitle("Leistung [W] / Energie [Wh]");

//      graph.getSecondScale().setVerticalAxisTitle("Spannung [V]");
//      graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.BLACK);
      //graph.getGridLabelRenderer().setHorizontalAxisTitle("Uhrzeit");

      graph.getViewport().setScalable(true);  // activate horizontal zooming and scrolling
      graph.getViewport().setScrollable(true);  // activate horizontal scrolling
      //graph.getViewport().setScalableY(true);  // activate horizontal and vertical zooming and scrolling
      //graph.getViewport().setScrollableY(true);  // activate vertical scrolling
   }

   @Override
   protected void updateView(final AppData value, final Activity activity)
   {
      // update graph
      GraphView graph = (GraphView) activity.findViewById(R.id.graph);

      if (value.HistoryLevel==0) {
         // show realtime graph
         Date d1 = Calendar.getInstance().getTime();
         if (ResetGraph) {
            ResetGraph=false;

            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt));
            graph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 vertical lines because of the space

            value_180.resetData(new DataPoint[] {new DataPoint(d1, value.value_180_day/1000.0)});
            value_280.resetData(new DataPoint[] {new DataPoint(d1, -value.value_280_day/1000.0)});
            power.resetData(new DataPoint[] {new DataPoint(d1, value.power/1000.0)});

            double HighestYValue = Math.max(value_180.getHighestValueY(), Math.max(value_280.getHighestValueY(), power.getHighestValueY()));
            double LowestYValue = Math.min(value_180.getLowestValueY(), Math.min(value_280.getLowestValueY(), power.getLowestValueY()));

            // scale Y-axis
            graph.getViewport().setYAxisBoundsManual(true);
            if (LowestYValue<0){
               graph.getViewport().setMinY(LowestYValue * 1.1);
            }else {
               graph.getViewport().setMinY(LowestYValue * 0.9);
            }
            graph.getViewport().setMaxY(HighestYValue*1.1);
            /*
            graph.getSecondScale().setMinY(LowestVoltage*0.9);
            graph.getSecondScale().setMaxY(HighestVoltage*1.1);
            */

            /*
            value_180.setDrawDataPoints(true);
            value_280.setDrawDataPoints(true);
            power.setDrawDataPoints(true);
            */
         } else {
            value_180.appendData(new DataPoint(d1, value.value_180_day/1000.0), false, 300); // Wh: 0...30Wh
            value_280.appendData(new DataPoint(d1, -value.value_280_day/1000.0), false, 300); // Wh: 0...30Wh
            power.appendData(new DataPoint(d1, value.power/1000.0), false, 300); // kW: 0...15kW
         }

         // scale X-axis
         graph.getViewport().setXAxisBoundsManual(true);
         graph.getViewport().setMinX(value_180.getLowestValueX());
         graph.getViewport().setMaxX(value_180.getHighestValueX());
      }else if ((value.HistoryLevel==1) || (value.HistoryLevel==2)){
         // plot energies of the last 7 days (168 hours) as bar-graphs
         Calendar calendar = Calendar.getInstance(); // for bar-graphs to set only full hours
         Calendar calendar2 = Calendar.getInstance(); // for power which is minutes-based

         // set minutes and seconds to zero, as the array will be shifted each full hour
         SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
         // get minutes- and seconds-offset and compensate the current date
         int offset_minutes= Integer.parseInt(fmt.format(calendar.getTime()).substring(3,5));
         int offset_seconds= Integer.parseInt(fmt.format(calendar.getTime()).substring(6,8));
         calendar.add(Calendar.MINUTE, -offset_minutes);
         calendar.add(Calendar.SECOND, -offset_seconds);

         // go back the received number of elements + 1
//         calendar.add(Calendar.HOUR_OF_DAY, -value.history_value_180_hour.length+1);
//         Date d1 = calendar.getTime();
         calendar.add(Calendar.HOUR_OF_DAY, -value.history_value_180_hour.length+1); // go back amount of data
//         calendar.add(Calendar.MINUTE, 30); // center the bar graph in middle of hour
         Date d1 = calendar.getTime();

         calendar2.add(Calendar.HOUR_OF_DAY, -value.history_value_180_hour.length);
         calendar2.add(Calendar.SECOND, 30); // we are plotting minutes-based with mean-values, so add half of a minute
         Date d2 = calendar2.getTime();

         // reset the graph
         value_180.resetData(new DataPoint[] {new DataPoint(d1, value.history_value_180_hour[value.history_value_180_hour.length-1]/1000.0)});
         value_280.resetData(new DataPoint[] {new DataPoint(d1, -value.history_value_280_hour[value.history_value_180_hour.length-1]/1000.0)});
         if (value.HistoryLevel==1) {
            // we have no power-data in HistoryLevel 1
            power.resetData(new DataPoint[]{new DataPoint(d2, 0)});
         }else{
            // we have received full history
            power.resetData(new DataPoint[]{new DataPoint(d2, value.history_power_seconds[value.history_power_seconds.length-1]/1000.0)});
         }

         for (int i=(value.history_value_180_hour.length-2); i>=0; i--) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            //d1 = calendar.getTime();

            // append current data

            // energy-values are showing data on an hourly base. so we have to display bars instead of a curve
            // we could use the bar-graph-option, but then the graph will be a bit overdrawn, so stay as "bar-graphed"-line
            // draw last value a millisecond before the current time-index
            calendar.add(Calendar.MILLISECOND, -1);
            d1 = calendar.getTime();
            value_180.appendData(new DataPoint(d1, value.history_value_180_hour[i+1]/1000.0), false, 3000);
            value_280.appendData(new DataPoint(d1, -value.history_value_280_hour[i+1]/1000.0), false, 3000);

            // now draw the current value
            calendar.add(Calendar.MILLISECOND, 1);
            d1 = calendar.getTime();
            value_180.appendData(new DataPoint(d1, value.history_value_180_hour[i]/1000.0), false, 3000);
            value_280.appendData(new DataPoint(d1, -value.history_value_280_hour[i]/1000.0), false, 3000);
            // alternatively show data as regular bar
            //value_180.appendData(new DataPoint(d1, value.Values[i].value_180_hour), false, 300);
            //value_280.appendData(new DataPoint(d1, -value.Values[i].value_280_hour), false, 300);
         }

         // draw the current points for the energies again to the next full hour in the future (as outlook)
         calendar.add(Calendar.HOUR_OF_DAY, 1);
         d1 = calendar.getTime();
         value_180.appendData(new DataPoint(d1, value.history_value_180_hour[0]/1000.0), false, 3000);
         value_280.appendData(new DataPoint(d1, -value.history_value_280_hour[0]/1000.0), false, 3000);

         SimpleDateFormat fmt_axis = new SimpleDateFormat("HH:mm\ndd.MM.");
         graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt_axis));
         graph.getGridLabelRenderer().setNumHorizontalLabels(6); // only 6 vertical lines because of the space

         double HighestYValue = Math.max(value_180.getHighestValueY(), value_280.getHighestValueY());
         double LowestYValue = Math.min(value_180.getLowestValueY(), value_280.getLowestValueY());

         // scale X-axis
         calendar.add(Calendar.HOUR_OF_DAY, 1); // one hour to the future (there is no data, but for better viewing)
         d1 = calendar.getTime();
         graph.getViewport().setMaxX(d1.getTime());
         calendar.add(Calendar.HOUR_OF_DAY, -value.history_value_180_hour.length-2); // number of elements back -2 for headroom
         d1 = calendar.getTime();
         graph.getViewport().setMinX(d1.getTime());
         //graph.getViewport().setMinX(TemperatureCurve1.getLowestValueX());

         if (value.HistoryLevel==2) {
            // plot power of the last 7 days (second-values) as graph

            int NumberOfMeanValues=60; // seconds
            //int NumberOfMeanValues=3600; // seconds
            for (int i=(value.history_power_seconds.length-1); i>=0; i-=NumberOfMeanValues) {
               // append current data
               // go one point further in the time
               //calendar2.add(Calendar.SECOND, 1); // here we would plot 604.800 elements which would overstrain many devices
               calendar2.add(Calendar.MINUTE, 1); // we are plotting only on minutes-base about 10.080 elements
               //calendar2.add(Calendar.HOUR_OF_DAY, 1); // we are plotting only on hour-base about 168 elements
               d2 = calendar2.getTime();

               // calculate mean-value over x seconds
               double power_mean=0;
               for (int j=0; j<NumberOfMeanValues; j++) {
                  power_mean+=value.history_power_seconds[i-j];
               }
               power_mean/=(double)NumberOfMeanValues;

               power.appendData(new DataPoint(d2, power_mean/1000.0), false, 30000);
            }

            HighestYValue = Math.max(HighestYValue, power.getHighestValueY());
            LowestYValue = Math.min(LowestYValue, power.getLowestValueY());
         }

         // scale Y-axis
         graph.getViewport().setYAxisBoundsManual(true);
         if (LowestYValue<0){
            graph.getViewport().setMinY(LowestYValue * 1.1);
         }else {
            graph.getViewport().setMinY(LowestYValue * 0.9);
         }
         graph.getViewport().setMaxY(HighestYValue*1.1);
         /*
         graph.getSecondScale().setMinY(LowestVoltage*0.9);
         graph.getSecondScale().setMaxY(HighestVoltage*1.1);
         */
      }else{
         // plot long-history of all days as bar-graphs

         // get first and last elements:
         LongHistory.LongHistoryElement FirstElement = value.longHistory.getFirstElement();
         LongHistory.LongHistoryElement LastElement = value.longHistory.getLastElement();

         // calendar-object for the last element
         Calendar calendarLastElement = Calendar.getInstance();
         calendarLastElement.set(Calendar.YEAR, LastElement.Year);
         calendarLastElement.set(Calendar.MONTH, LastElement.Month-1);
         calendarLastElement.set(Calendar.DAY_OF_MONTH, LastElement.Day);
         calendarLastElement.set(Calendar.HOUR_OF_DAY, 0);
         calendarLastElement.set(Calendar.MINUTE, 0);
         calendarLastElement.set(Calendar.SECOND, 0);
         calendarLastElement.set(Calendar.MILLISECOND, 0);

         // calendar-object for current day
         Calendar calendar = Calendar.getInstance();
         calendar.set(Calendar.YEAR, FirstElement.Year);
         calendar.set(Calendar.MONTH, FirstElement.Month-1); // 0...11
         calendar.set(Calendar.DAY_OF_MONTH, FirstElement.Day);
         calendar.set(Calendar.HOUR_OF_DAY, 0);
         calendar.set(Calendar.MINUTE, 0);
         calendar.set(Calendar.SECOND, 0);

         Date DateFirstElement = new Date();
         DateFirstElement.setTime(calendar.getTime().getTime());

         // iterate through all days and add day-values to graph
         LongHistory.LongHistoryElement DayValue;

         // reset the graph and plot first element at 00:00:00
         Date d1 = calendar.getTime();
         DayValue = value.longHistory.get(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.YEAR));
         power.resetData(new DataPoint[]{new DataPoint(d1, 0)});
         value_180.resetData(new DataPoint[] {new DataPoint(d1, DayValue.value_180/1000.0)});
         value_280.resetData(new DataPoint[] {new DataPoint(d1, -DayValue.value_280/1000.0)});

         // plot the values at 23:59:59
         calendar.set(Calendar.HOUR_OF_DAY, 23);
         calendar.set(Calendar.MINUTE, 59);
         calendar.set(Calendar.SECOND, 59);
         d1 = calendar.getTime();
         value_180.appendData(new DataPoint(d1, DayValue.value_180/1000.0), false, 3000);
         value_280.appendData(new DataPoint(d1, -DayValue.value_280/1000.0), false, 3000);

         while(calendar.getTime().getTime()<calendarLastElement.getTime().getTime()) {
            // go one day further
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.HOUR_OF_DAY, 24); // go one day ahead

            // get day-values
            DayValue = value.longHistory.get(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.YEAR));

            // plot the values at 00:00:00
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            d1 = calendar.getTime();
            value_180.appendData(new DataPoint(d1, DayValue.value_180/1000.0), false, 3000);
            value_280.appendData(new DataPoint(d1, -DayValue.value_280/1000.0), false, 3000);

            // plot the values at 23:59:59
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            d1 = calendar.getTime();
            value_180.appendData(new DataPoint(d1, DayValue.value_180/1000.0), false, 3000);
            value_280.appendData(new DataPoint(d1, -DayValue.value_280/1000.0), false, 3000);
         }

         //do{
         //}while();

         SimpleDateFormat fmt_axis = new SimpleDateFormat("dd.MM.\nYYYY");
         graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt_axis));
         graph.getGridLabelRenderer().setNumHorizontalLabels(6); // only 6 vertical lines because of the space

         double HighestYValue = Math.max(value_180.getHighestValueY(), value_280.getHighestValueY());
         double LowestYValue = Math.min(value_180.getLowestValueY(), value_280.getLowestValueY());

         // scale X-axis
         d1 = calendar.getTime();
         graph.getViewport().setMaxX(d1.getTime());
         graph.getViewport().setMinX(DateFirstElement.getTime());
         //graph.getViewport().setMinX(TemperatureCurve1.getLowestValueX());

         // scale Y-axis
         graph.getViewport().setYAxisBoundsManual(true);
         if (LowestYValue<0){
            graph.getViewport().setMinY(LowestYValue * 1.1);
         }else {
            graph.getViewport().setMinY(LowestYValue * 0.9);
         }
         graph.getViewport().setMaxY(HighestYValue*1.1);
      }
   }

   public void reset()
   {
      ResetGraph = true;
      HelperFunctions.TCPCommandString="C:DATA=0"; // DATA=0 -> HistoryLevel=0 -> Request no history-data
   }

   public void showHistory(int HistoryLevel)
   {
      // DATA=2 -> HistoryLevel=2 -> Request 7-day history-data (with Power-history)
      // DATA=3 -> HistoryLevel=3 -> Request long-history-data (hour-based, additional functions via LongHistory-class)
      HelperFunctions.TCPCommandString="C:DATA="+HistoryLevel;
   }

   public void switchXY(Activity activity)
   {
      DoXScaleScroll = !DoXScaleScroll;

      GraphView graph = (GraphView) activity.findViewById(R.id.graph);

      // scale/scroll X-axis
      graph.getViewport().setScalable(DoXScaleScroll);  // activate horizontal zooming and scrolling
      graph.getViewport().setScrollable(DoXScaleScroll);  // activate horizontal scrolling
      graph.getViewport().setScalableY(!DoXScaleScroll);  // activate horizontal and vertical zooming and scrolling
      graph.getViewport().setScrollableY(!DoXScaleScroll);  // activate vertical scrolling
   }
}
