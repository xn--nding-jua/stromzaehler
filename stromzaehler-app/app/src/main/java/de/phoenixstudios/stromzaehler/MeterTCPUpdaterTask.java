/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.phoenixstudios.stromzaehler;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.graphics.Color;
import android.widget.TextView;

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
   private final LineGraphSeries<DataPoint> Current1;
   private final LineGraphSeries<DataPoint> Current2;
   private final LineGraphSeries<DataPoint> Current3;
   private final LineGraphSeries<DataPoint> Voltage1;
   private final LineGraphSeries<DataPoint> Voltage2;
   private final LineGraphSeries<DataPoint> Voltage3;
   private final LineGraphSeries<DataPoint> Power1;
   private final LineGraphSeries<DataPoint> Power2;
   private final LineGraphSeries<DataPoint> Power3;
   private final LineGraphSeries<DataPoint> value_180;
   private final LineGraphSeries<DataPoint> value_280;

   public MeterTCPUpdaterTask(final Activity activity)
   {
      super(activity);

      // styling series
      Current1 = new LineGraphSeries<>();
      Current2 = new LineGraphSeries<>();
      Current3 = new LineGraphSeries<>();
      Voltage1 = new LineGraphSeries<>();
      Voltage2 = new LineGraphSeries<>();
      Voltage3 = new LineGraphSeries<>();
      Power1 = new LineGraphSeries<>();
      Power2 = new LineGraphSeries<>();
      Power3 = new LineGraphSeries<>();
      value_180 = new LineGraphSeries<>();
      value_280 = new LineGraphSeries<>();

      Current1.setTitle("I_L1 [A]");
      Current2.setTitle("I_L2 [A]");
      Current3.setTitle("I_L3 [A]");
      Voltage1.setTitle("U_L1 [dV]");
      Voltage2.setTitle("U_L2 [dV]");
      Voltage3.setTitle("U_L3 [dV]");
      Power1.setTitle("P_L1 [kW]");
      Power2.setTitle("P_L2 [kW]");
      Power3.setTitle("P_L3 [kW]");
      value_180.setTitle("1.8.0 [kWh]");
      value_280.setTitle("2.8.0 [kWh]");


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

      Power1.setColor(Color.rgb(0,255,0));
      Power1.setDrawDataPoints(false);
      Power1.setDataPointsRadius(10);
      Power1.setThickness(8);
      Power2.setColor(Color.rgb(0,192,0));
      Power2.setDrawDataPoints(false);
      Power2.setDataPointsRadius(10);
      Power2.setThickness(8);
      Power3.setColor(Color.rgb(0,128,0));
      Power3.setDrawDataPoints(false);
      Power3.setDataPointsRadius(10);
      Power3.setThickness(8);

      value_180.setColor(Color.rgb(0,0,0));
      value_180.setDrawDataPoints(false);
      value_180.setDataPointsRadius(10);
      value_180.setThickness(8);
      value_280.setColor(Color.rgb(255,128,0));
      value_280.setDrawDataPoints(false);
      value_280.setDataPointsRadius(10);
      value_280.setThickness(8);

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
      graph.addSeries(Power1);
      graph.addSeries(Power2);
      graph.addSeries(Power3);

      graph.addSeries(value_180);
      graph.addSeries(value_280);

      graph.getLegendRenderer().setVisible(true);
      graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);


      // set date label formatter
      //DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
      SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
      graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt));
      graph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 vertical lines because of the space
      graph.getGridLabelRenderer().setNumVerticalLabels(6); // 6 horizontal lines
      graph.getGridLabelRenderer().setHumanRounding(true);
      graph.getGridLabelRenderer().setVerticalAxisTitle("Strom [A] / Spannung [dV] / Leistung [kW] / Energie [kWh]");

//      graph.getSecondScale().setVerticalAxisTitle("Spannung [V]");
//      graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.BLACK);
      //graph.getGridLabelRenderer().setHorizontalAxisTitle("Uhrzeit");

      graph.getViewport().setScalable(true);  // activate horizontal zooming and scrolling
      graph.getViewport().setScrollable(true);  // activate horizontal scrolling
      graph.getViewport().setScalableY(true);  // activate horizontal and vertical zooming and scrolling
      graph.getViewport().setScrollableY(true);  // activate vertical scrolling
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

            Current1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].current_phase1)});
            Current2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].current_phase2)});
            Current3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].current_phase3)});

            Voltage1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].voltage_phase1/10.0)});
            Voltage2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].voltage_phase2/10.0)});
            Voltage3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].voltage_phase3/10.0)});

            Power1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].power_phase1/1000.0)});
            Power2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].power_phase2/1000.0)});
            Power3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].power_phase3/1000.0)});

            value_180.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].value_180_hour/1000.0)});
            value_280.resetData(new DataPoint[] {new DataPoint(d1, value.Values[0].value_280_hour/1000.0)});

            double HighestCurrent = Math.max(Current1.getHighestValueY(), Math.max(Current2.getHighestValueY(), Current3.getHighestValueY()));
            double HighestVoltage = Math.max(Voltage1.getHighestValueY(), Math.max(Voltage2.getHighestValueY(), Voltage3.getHighestValueY()));
            double HighestPower = Math.max(Power1.getHighestValueY(), Math.max(Power2.getHighestValueY(), Power3.getHighestValueY()));
            double HighestEnergy = Math.max(value_180.getHighestValueY(), value_280.getHighestValueY());
            double HighestYValue = Math.max(HighestCurrent, Math.max(HighestVoltage, Math.max(HighestPower, HighestEnergy)));

            double LowestCurrent = Math.min(Current1.getLowestValueY(), Math.min(Current2.getLowestValueY(), Current3.getLowestValueY()));
            double LowestVoltage = Math.min(Voltage1.getLowestValueY(), Math.min(Voltage2.getLowestValueY(), Voltage3.getLowestValueY()));
            double LowestPower = Math.min(Power1.getLowestValueY(), Math.min(Power2.getLowestValueY(), Power3.getLowestValueY()));
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
         } else {
            Current1.appendData(new DataPoint(d1, value.Values[0].current_phase1), false, 300); // Ampere: 0...63A
            Current2.appendData(new DataPoint(d1, value.Values[0].current_phase2), false, 300); // Ampere: 0...63A
            Current3.appendData(new DataPoint(d1, value.Values[0].current_phase3), false, 300); // Ampere: 0...63A

            Voltage1.appendData(new DataPoint(d1, value.Values[0].voltage_phase1/10.0), false, 300); // dVolt: around 22.0..24.0V
            Voltage2.appendData(new DataPoint(d1, value.Values[0].voltage_phase2/10.0), false, 300); // dVolt: around 22.0..24.0V
            Voltage3.appendData(new DataPoint(d1, value.Values[0].voltage_phase3/10.0), false, 300); // dVolt: around 22.0..24.0V

            Power1.appendData(new DataPoint(d1, value.Values[0].power_phase1/1000.0), false, 300); // kW: 0...15kW
            Power2.appendData(new DataPoint(d1, value.Values[0].power_phase2/1000.0), false, 300); // kW: 0...15kW
            Power3.appendData(new DataPoint(d1, value.Values[0].power_phase3/1000.0), false, 300); // kW: 0...15kW

            value_180.appendData(new DataPoint(d1, value.Values[0].value_180_hour/1000.0), false, 300); // kWh: 0...30kWh
            value_280.appendData(new DataPoint(d1, value.Values[0].value_280_hour/1000.0), false, 300); // kWh: 0...30kWh
         }

         // scale X-axis
         graph.getViewport().setXAxisBoundsManual(true);
         graph.getViewport().setMinX(Power1.getLowestValueX());
         graph.getViewport().setMaxX(Power1.getHighestValueX());
      }else{
         // show last 7 days
         Calendar calendar = Calendar.getInstance();

         calendar.add(Calendar.HOUR_OF_DAY, -HelperFunctions.ReceivedElements+1);
         Date d1 = calendar.getTime();

         // reset the graph
         Current1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].current_phase1)});
         Current2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].current_phase2)});
         Current3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].current_phase3)});

         Voltage1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].voltage_phase1/10.0)});
         Voltage2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].voltage_phase2/10.0)});
         Voltage3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].voltage_phase3/10.0)});

         Power1.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].power_phase1/1000.0)});
         Power2.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].power_phase2/1000.0)});
         Power3.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].power_phase3/1000.0)});

         value_180.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].value_180_hour/1000.0)});
         value_280.resetData(new DataPoint[] {new DataPoint(d1, value.Values[HelperFunctions.ReceivedElements-1].value_280_hour/1000.0)});

         for (int i=(HelperFunctions.ReceivedElements-2); i>=0; i--) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            d1 = calendar.getTime();

            Current1.appendData(new DataPoint(d1, value.Values[i].current_phase1), false, 300);
            Current2.appendData(new DataPoint(d1, value.Values[i].current_phase2), false, 300);
            Current3.appendData(new DataPoint(d1, value.Values[i].current_phase3), false, 300);
            Voltage1.appendData(new DataPoint(d1, value.Values[i].voltage_phase1/10.0), false, 300);
            Voltage2.appendData(new DataPoint(d1, value.Values[i].voltage_phase2/10.0), false, 300);
            Voltage3.appendData(new DataPoint(d1, value.Values[i].voltage_phase3/10.0), false, 300);
            Power1.appendData(new DataPoint(d1, value.Values[i].power_phase1/1000.0), false, 300);
            Power2.appendData(new DataPoint(d1, value.Values[i].power_phase2/1000.0), false, 300);
            Power3.appendData(new DataPoint(d1, value.Values[i].power_phase3/1000.0), false, 300);
            value_180.appendData(new DataPoint(d1, value.Values[i].value_180_hour/1000.0), false, 300);
            value_280.appendData(new DataPoint(d1, value.Values[i].value_280_hour/1000.0), false, 300);
         }

         SimpleDateFormat fmt = new SimpleDateFormat("HH:mm\ndd.MM.");
         graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(activity, fmt));
         graph.getGridLabelRenderer().setNumHorizontalLabels(6); // only 6 vertical lines because of the space

         double HighestCurrent = Math.max(Current1.getHighestValueY(), Math.max(Current2.getHighestValueY(), Current3.getHighestValueY()));
         double HighestVoltage = Math.max(Voltage1.getHighestValueY(), Math.max(Voltage2.getHighestValueY(), Voltage3.getHighestValueY()));
         double HighestPower = Math.max(Power1.getHighestValueY(), Math.max(Power2.getHighestValueY(), Power3.getHighestValueY()));
         double HighestEnergy = Math.max(value_180.getHighestValueY(), value_280.getHighestValueY());
         double HighestYValue = Math.max(HighestCurrent, Math.max(HighestVoltage, Math.max(HighestPower, HighestEnergy)));

         double LowestCurrent = Math.min(Current1.getLowestValueY(), Math.min(Current2.getLowestValueY(), Current3.getLowestValueY()));
         double LowestVoltage = Math.min(Voltage1.getLowestValueY(), Math.min(Voltage2.getLowestValueY(), Voltage3.getLowestValueY()));
         double LowestPower = Math.min(Power1.getLowestValueY(), Math.min(Power2.getLowestValueY(), Power3.getLowestValueY()));
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
         // scale X-axis
         calendar.add(Calendar.HOUR_OF_DAY, 1);
         d1 = calendar.getTime();
         graph.getViewport().setMaxX(d1.getTime());
         calendar.add(Calendar.HOUR_OF_DAY, -HelperFunctions.ReceivedElements-1);
         d1 = calendar.getTime();
         graph.getViewport().setMinX(d1.getTime());
         //graph.getViewport().setMinX(TemperatureCurve1.getLowestValueX());
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
}
