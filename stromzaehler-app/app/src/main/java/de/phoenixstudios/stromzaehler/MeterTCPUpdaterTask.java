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
   private final LineGraphSeries<DataPoint> PowerOrEnergy1;
   private final LineGraphSeries<DataPoint> PowerOrEnergy2;
   private final LineGraphSeries<DataPoint> PowerOrEnergy3;
   private final LineGraphSeries<DataPoint> Current1;
   private final LineGraphSeries<DataPoint> Current2;
   private final LineGraphSeries<DataPoint> Current3;
   private final LineGraphSeries<DataPoint> Voltage1;
   private final LineGraphSeries<DataPoint> Voltage2;
   private final LineGraphSeries<DataPoint> Voltage3;

   public MeterTCPUpdaterTask(final Activity activity)
   {
      super(activity);

      // styling series
      value_180 = new LineGraphSeries<>();
      value_280 = new LineGraphSeries<>();
      PowerOrEnergy1 = new LineGraphSeries<>();
      PowerOrEnergy2 = new LineGraphSeries<>();
      PowerOrEnergy3 = new LineGraphSeries<>();
      Current1 = new LineGraphSeries<>();
      Current2 = new LineGraphSeries<>();
      Current3 = new LineGraphSeries<>();
      Voltage1 = new LineGraphSeries<>();
      Voltage2 = new LineGraphSeries<>();
      Voltage3 = new LineGraphSeries<>();

      value_180.setTitle("1.8.0 [kWh]");
      value_280.setTitle("2.8.0 [kWh]");
      PowerOrEnergy1.setTitle("P_L1 [kW]"); // as standard the live-graph is shown, so we need unit for power [kW] instead of energy [kWh]
      PowerOrEnergy2.setTitle("P_L2 [kW]"); // as standard the live-graph is shown, so we need unit for power [kW] instead of energy [kWh]
      PowerOrEnergy3.setTitle("P_L3 [kW]"); // as standard the live-graph is shown, so we need unit for power [kW] instead of energy [kWh]
      Current1.setTitle("I_L1 [A]");
      Current2.setTitle("I_L2 [A]");
      Current3.setTitle("I_L3 [A]");
      Voltage1.setTitle("U_L1 [dV]"); // we are using Deci-Volt, so that we are using the Y-axis better (otherwise we would have 230V)
      Voltage2.setTitle("U_L2 [dV]"); // we are using Deci-Volt, so that we are using the Y-axis better (otherwise we would have 230V)
      Voltage3.setTitle("U_L3 [dV]"); // we are using Deci-Volt, so that we are using the Y-axis better (otherwise we would have 230V)

      value_180.setColor(Color.rgb(0,0,0));
      value_180.setDrawDataPoints(false);
      value_180.setDataPointsRadius(10);
      value_180.setThickness(8);
      value_180.setDrawBackground(true);
      value_180.setBackgroundColor(Color.argb(100, 0,0,0));
      value_280.setColor(Color.rgb(255,128,0));
      value_280.setDrawDataPoints(false);
      value_280.setDataPointsRadius(10);
      value_280.setThickness(8);
      value_280.setDrawBackground(true);
      value_280.setBackgroundColor(Color.argb(100, 255,128,0));

      PowerOrEnergy1.setColor(Color.rgb(0,255,0));
      PowerOrEnergy1.setDrawDataPoints(false);
      PowerOrEnergy1.setDataPointsRadius(10);
      PowerOrEnergy1.setThickness(8);
      PowerOrEnergy2.setColor(Color.rgb(0,192,0));
      PowerOrEnergy2.setDrawDataPoints(false);
      PowerOrEnergy2.setDataPointsRadius(10);
      PowerOrEnergy2.setThickness(8);
      PowerOrEnergy3.setColor(Color.rgb(0,128,0));
      PowerOrEnergy3.setDrawDataPoints(false);
      PowerOrEnergy3.setDataPointsRadius(10);
      PowerOrEnergy3.setThickness(8);

      Current1.setColor(Color.rgb(255,0,0));
      Current1.setDrawDataPoints(false);
      Current1.setDataPointsRadius(10);
      Current1.setThickness(8);
      Current2.setColor(Color.rgb(192,0,0));
      Current2.setDrawDataPoints(false);
      Current2.setDataPointsRadius(10);
      Current2.setThickness(8);
      Current3.setColor(Color.rgb(128,0,0));
      Current3.setDrawDataPoints(false);
      Current3.setDataPointsRadius(10);
      Current3.setThickness(8);

      Voltage1.setColor(Color.rgb(0,0,255));
      Voltage1.setDrawDataPoints(false);
      Voltage1.setDataPointsRadius(10);
      Voltage1.setThickness(8);
      Voltage2.setColor(Color.rgb(0,0,192));
      Voltage2.setDrawDataPoints(false);
      Voltage2.setDataPointsRadius(10);
      Voltage2.setThickness(8);
      Voltage3.setColor(Color.rgb(0,0,128));
      Voltage3.setDrawDataPoints(false);
      Voltage3.setDataPointsRadius(10);
      Voltage3.setThickness(8);

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

      graph.addSeries(value_180);
      graph.addSeries(value_280);

      graph.addSeries(PowerOrEnergy1);
      graph.addSeries(PowerOrEnergy2);
      graph.addSeries(PowerOrEnergy3);

      graph.addSeries(Current1);
      graph.addSeries(Current2);
      graph.addSeries(Current3);
/*
      graph.getSecondScale().addSeries(Voltage1);
      graph.getSecondScale().addSeries(Voltage2);
      graph.getSecondScale().addSeries(Voltage3);
*/
      graph.addSeries(Voltage1);
      graph.addSeries(Voltage2);
      graph.addSeries(Voltage3);

      graph.getLegendRenderer().setVisible(true);
      graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);


      // set date label formatter
      //DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
      SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
      graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt));
      graph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 vertical lines because of the space
      graph.getGridLabelRenderer().setNumVerticalLabels(6); // 6 horizontal lines
      graph.getGridLabelRenderer().setHumanRounding(true);
      //graph.getGridLabelRenderer().setVerticalAxisTitle("Strom [A] / Spannung [dV] / Leistung [kW] / Energie [kWh]");

//      graph.getSecondScale().setVerticalAxisTitle("Spannung [V]");
//      graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.BLACK);
      //graph.getGridLabelRenderer().setHorizontalAxisTitle("Uhrzeit");

      graph.getViewport().setScalable(true);  // activate horizontal zooming and scrolling
      graph.getViewport().setScrollable(true);  // activate horizontal scrolling
      //graph.getViewport().setScalableY(true);  // activate horizontal and vertical zooming and scrolling
      //graph.getViewport().setScrollableY(true);  // activate vertical scrolling
   }

   @Override
   protected void updateView(final AppData value, final Activity activity) {
      // update graph
      GraphView graph = (GraphView) activity.findViewById(R.id.graph);

      if (HelperFunctions.ReceivedElements==1) {
         // show realtime graph
         Date d1 = Calendar.getInstance().getTime();
         if (ResetGraph) {
            ResetGraph=false;

            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt));
            graph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 vertical lines because of the space

            value_180.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].value_180_hour/1000.0)});
            value_280.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].value_280_hour/1000.0)});

            // show power instead of energy for the real-time-graph
            PowerOrEnergy1.resetData(new DataPoint[] {new DataPoint(d1, value.power_phase1/1000.0)});
            PowerOrEnergy2.resetData(new DataPoint[] {new DataPoint(d1, value.power_phase2/1000.0)});
            PowerOrEnergy3.resetData(new DataPoint[] {new DataPoint(d1, value.power_phase3/1000.0)});

            Current1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].current_phase1)});
            Current2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].current_phase2)});
            Current3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].current_phase3)});

            Voltage1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].voltage_phase1/10.0)});
            Voltage2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].voltage_phase2/10.0)});
            Voltage3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].voltage_phase3/10.0)});

            double HighestCurrent = Math.max(Current1.getHighestValueY(), Math.max(Current2.getHighestValueY(), Current3.getHighestValueY()));
            double HighestVoltage = Math.max(Voltage1.getHighestValueY(), Math.max(Voltage2.getHighestValueY(), Voltage3.getHighestValueY()));
            double HighestPower = Math.max(PowerOrEnergy1.getHighestValueY(), Math.max(PowerOrEnergy2.getHighestValueY(), PowerOrEnergy3.getHighestValueY()));
            double HighestEnergy = Math.max(value_180.getHighestValueY(), value_280.getHighestValueY());
            double HighestYValue = Math.max(HighestCurrent, Math.max(HighestVoltage, Math.max(HighestPower, HighestEnergy)));

            double LowestCurrent = Math.min(Current1.getLowestValueY(), Math.min(Current2.getLowestValueY(), Current3.getLowestValueY()));
            double LowestVoltage = Math.min(Voltage1.getLowestValueY(), Math.min(Voltage2.getLowestValueY(), Voltage3.getLowestValueY()));
            double LowestPower = Math.min(PowerOrEnergy1.getLowestValueY(), Math.min(PowerOrEnergy2.getLowestValueY(), PowerOrEnergy3.getLowestValueY()));
            double LowestEnergy = Math.max(value_180.getLowestValueY(), value_280.getLowestValueY());
            double LowestYValue = Math.min(LowestCurrent, Math.min(LowestVoltage, Math.min(LowestPower, LowestEnergy)));

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

            value_180.setDrawDataPoints(true);
            value_280.setDrawDataPoints(true);
            PowerOrEnergy1.setDrawDataPoints(true);
            PowerOrEnergy2.setDrawDataPoints(true);
            PowerOrEnergy3.setDrawDataPoints(true);
            Current1.setDrawDataPoints(true);
            Current2.setDrawDataPoints(true);
            Current3.setDrawDataPoints(true);
            Voltage1.setDrawDataPoints(true);
            Voltage2.setDrawDataPoints(true);
            Voltage3.setDrawDataPoints(true);

            PowerOrEnergy1.setTitle("P_L1 [kW]");
            PowerOrEnergy2.setTitle("P_L2 [kW]");
            PowerOrEnergy3.setTitle("P_L3 [kW]");
         } else {
            value_180.appendData(new DataPoint(d1, value.Values[0].value_180_hour/1000.0), false, 300); // kWh: 0...30kWh
            value_280.appendData(new DataPoint(d1, value.Values[0].value_280_hour/1000.0), false, 300); // kWh: 0...30kWh

            PowerOrEnergy1.appendData(new DataPoint(d1, value.power_phase1/1000.0), false, 300); // kW: 0...15kW
            PowerOrEnergy2.appendData(new DataPoint(d1, value.power_phase2/1000.0), false, 300); // kW: 0...15kW
            PowerOrEnergy3.appendData(new DataPoint(d1, value.power_phase3/1000.0), false, 300); // kW: 0...15kW

            Current1.appendData(new DataPoint(d1, value.Values[0].current_phase1), false, 300); // Ampere: 0...63A
            Current2.appendData(new DataPoint(d1, value.Values[0].current_phase2), false, 300); // Ampere: 0...63A
            Current3.appendData(new DataPoint(d1, value.Values[0].current_phase3), false, 300); // Ampere: 0...63A

            Voltage1.appendData(new DataPoint(d1, value.Values[0].voltage_phase1/10.0), false, 300); // dVolt: around 22.0..24.0V
            Voltage2.appendData(new DataPoint(d1, value.Values[0].voltage_phase2/10.0), false, 300); // dVolt: around 22.0..24.0V
            Voltage3.appendData(new DataPoint(d1, value.Values[0].voltage_phase3/10.0), false, 300); // dVolt: around 22.0..24.0V
         }

         // scale X-axis
         graph.getViewport().setXAxisBoundsManual(true);
         graph.getViewport().setMinX(PowerOrEnergy1.getLowestValueX());
         graph.getViewport().setMaxX(PowerOrEnergy1.getHighestValueX());
      }else{
         // show last 7 days
         Calendar calendar = Calendar.getInstance();

         // set minutes and seconds to zero, as the array will be shifted each full hour
         SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
         // get minutes- and seconds-offset and compensate the current date
         int offset_minutes= Integer.parseInt(fmt.format(calendar.getTime()).substring(3,5));
         int offset_seconds= Integer.parseInt(fmt.format(calendar.getTime()).substring(6,8));
         calendar.add(Calendar.MINUTE, -offset_minutes);
         calendar.add(Calendar.SECOND, -offset_seconds);

         // go back the received number of elements + 1
         calendar.add(Calendar.HOUR_OF_DAY, -HelperFunctions.ReceivedElements+1);
         Date d1 = calendar.getTime();

         // reset the graph
         value_180.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].value_180_hour/1000.0)});
         value_280.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].value_280_hour/1000.0)});

         PowerOrEnergy1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].energy_phase1/1000.0)});
         PowerOrEnergy2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].energy_phase2/1000.0)});
         PowerOrEnergy3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].energy_phase3/1000.0)});

         Current1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].current_phase1)});
         Current2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].current_phase2)});
         Current3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].current_phase3)});

         Voltage1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].voltage_phase1/10.0)});
         Voltage2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].voltage_phase2/10.0)});
         Voltage3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].voltage_phase3/10.0)});

         for (int i=(HelperFunctions.ReceivedElements-2); i>=0; i--) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            //d1 = calendar.getTime();

            // append current data

            // energy-values are showing data on an hourly base. so we have to display bars instead of a curve
            // we could use the bar-graph-option, but then the graph will be a bit overdrawn, so stay as "bar-graphed"-line
            // draw last value a millisecond before the current time-index
            calendar.add(Calendar.MILLISECOND, -1);
            d1 = calendar.getTime();
            value_180.appendData(new DataPoint(d1, value.Values[i+1].value_180_hour/1000.0), false, 300);
            value_280.appendData(new DataPoint(d1, value.Values[i+1].value_280_hour/1000.0), false, 300);
            PowerOrEnergy1.appendData(new DataPoint(d1, value.Values[i+1].energy_phase1/1000.0), false, 300);
            PowerOrEnergy2.appendData(new DataPoint(d1, value.Values[i+1].energy_phase2/1000.0), false, 300);
            PowerOrEnergy3.appendData(new DataPoint(d1, value.Values[i+1].energy_phase3/1000.0), false, 300);

            // now draw the current value
            calendar.add(Calendar.MILLISECOND, 1);
            d1 = calendar.getTime();
            value_180.appendData(new DataPoint(d1, value.Values[i].value_180_hour/1000.0), false, 300);
            value_280.appendData(new DataPoint(d1, value.Values[i].value_280_hour/1000.0), false, 300);
            PowerOrEnergy1.appendData(new DataPoint(d1, value.Values[i].energy_phase1/1000.0), false, 300);
            PowerOrEnergy2.appendData(new DataPoint(d1, value.Values[i].energy_phase2/1000.0), false, 300);
            PowerOrEnergy3.appendData(new DataPoint(d1, value.Values[i].energy_phase3/1000.0), false, 300);
            // alternatively show data as regular bar
            //value_180.appendData(new DataPoint(d1, value.Values[i].value_180_hour/1000.0), false, 300);
            //value_280.appendData(new DataPoint(d1, value.Values[i].value_280_hour/1000.0), false, 300);

            // draw all other spot-data
            Current1.appendData(new DataPoint(d1, value.Values[i].current_phase1), false, 300);
            Current2.appendData(new DataPoint(d1, value.Values[i].current_phase2), false, 300);
            Current3.appendData(new DataPoint(d1, value.Values[i].current_phase3), false, 300);
            Voltage1.appendData(new DataPoint(d1, value.Values[i].voltage_phase1/10.0), false, 300);
            Voltage2.appendData(new DataPoint(d1, value.Values[i].voltage_phase2/10.0), false, 300);
            Voltage3.appendData(new DataPoint(d1, value.Values[i].voltage_phase3/10.0), false, 300);
         }

         // draw the current points for the energies again to the next full hour in the future (as outlook)
         calendar.add(Calendar.HOUR_OF_DAY, 1);
         d1 = calendar.getTime();
         value_180.appendData(new DataPoint(d1, value.Values[0].value_180_hour/1000.0), false, 300);
         value_280.appendData(new DataPoint(d1, value.Values[0].value_280_hour/1000.0), false, 300);
         PowerOrEnergy1.appendData(new DataPoint(d1, value.Values[0].energy_phase1/1000.0), false, 300);
         PowerOrEnergy2.appendData(new DataPoint(d1, value.Values[0].energy_phase2/1000.0), false, 300);
         PowerOrEnergy3.appendData(new DataPoint(d1, value.Values[0].energy_phase3/1000.0), false, 300);

         SimpleDateFormat fmt_axis = new SimpleDateFormat("HH:mm\ndd.MM.");
         graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt_axis));
         graph.getGridLabelRenderer().setNumHorizontalLabels(6); // only 6 vertical lines because of the space

         double HighestCurrent = Math.max(Current1.getHighestValueY(), Math.max(Current2.getHighestValueY(), Current3.getHighestValueY()));
         double HighestVoltage = Math.max(Voltage1.getHighestValueY(), Math.max(Voltage2.getHighestValueY(), Voltage3.getHighestValueY()));
         double HighestEnergy1 = Math.max(PowerOrEnergy1.getHighestValueY(), Math.max(PowerOrEnergy2.getHighestValueY(), PowerOrEnergy3.getHighestValueY()));
         double HighestEnergy2 = Math.max(value_180.getHighestValueY(), value_280.getHighestValueY());
         double HighestYValue = Math.max(HighestCurrent, Math.max(HighestVoltage, Math.max(HighestEnergy1, HighestEnergy2)));

         double LowestCurrent = Math.min(Current1.getLowestValueY(), Math.min(Current2.getLowestValueY(), Current3.getLowestValueY()));
         double LowestVoltage = Math.min(Voltage1.getLowestValueY(), Math.min(Voltage2.getLowestValueY(), Voltage3.getLowestValueY()));
         double LowestEnergy1 = Math.min(PowerOrEnergy1.getLowestValueY(), Math.min(PowerOrEnergy2.getLowestValueY(), PowerOrEnergy3.getLowestValueY()));
         double LowestEnergy2 = Math.max(value_180.getLowestValueY(), value_280.getLowestValueY());
         double LowestYValue = Math.min(LowestCurrent, Math.min(LowestVoltage, Math.min(LowestEnergy1, LowestEnergy2)));

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
         // scale X-axis
         calendar.add(Calendar.HOUR_OF_DAY, 1); // one hour to the future (there is no data, but for better viewing)
         d1 = calendar.getTime();
         graph.getViewport().setMaxX(d1.getTime());
         calendar.add(Calendar.HOUR_OF_DAY, -HelperFunctions.ReceivedElements-2); // number of elements back -2 for headroom
         d1 = calendar.getTime();
         graph.getViewport().setMinX(d1.getTime());
         //graph.getViewport().setMinX(TemperatureCurve1.getLowestValueX());

         value_180.setDrawDataPoints(false);
         value_280.setDrawDataPoints(false);
         PowerOrEnergy1.setDrawDataPoints(false);
         PowerOrEnergy2.setDrawDataPoints(false);
         PowerOrEnergy3.setDrawDataPoints(false);
         Current1.setDrawDataPoints(false);
         Current2.setDrawDataPoints(false);
         Current3.setDrawDataPoints(false);
         Voltage1.setDrawDataPoints(false);
         Voltage2.setDrawDataPoints(false);
         Voltage3.setDrawDataPoints(false);

         PowerOrEnergy1.setTitle("E_L1 [kWh]");
         PowerOrEnergy2.setTitle("E_L2 [kWh]");
         PowerOrEnergy3.setTitle("E_L3 [kWh]");
      }
   }

   public void reset()
   {
      ResetGraph = true;
      HelperFunctions.TCPCommandString="C:DATA=1";
   }
   public void showHistory()
   {
      HelperFunctions.TCPCommandString="C:DATA=168";
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
