package opensource.zeocompanion.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.MultiSeries;
import com.jjoe64.graphview.series.Series;
import com.jjoe64.graphview.series.StackedBarGraphSeries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import opensource.zeocompanion.ZeoCompanionApplication;
import opensource.zeocompanion.utility.Utilities;

// displays a time-serial line chart
public class TrendsGraphView extends GraphView {
    // member variables
    private Context mContext = null;
    private  ArrayList<Trends_dataSet> mOrigDataSet = null;
    private int mDatasetLen = 0;
    private int mShowAsMode = 0;
    private Point mScreenSize = null;
    private int mQtySeries = 0;
    private long mLowestTimestamp = 0L;
    private double mGoalTotalSleepMin = 480.0;  // 8 hours
    private double mGoalREMpct = 20.0;
    private double mGoalDeepPct = 15.0;
    private double mGoalLightPct = 70.0;
    LineGraphSeries<DataPoint> mLineSeries_TimeToZ = null;
    LineGraphSeries<DataPoint> mLineSeries_TotalSleep = null;
    LineGraphSeries<DataPoint> mLineSeries_Awake = null;
    LineGraphSeries<DataPoint> mLineSeries_REM = null;
    LineGraphSeries<DataPoint> mLineSeries_Light = null;
    LineGraphSeries<DataPoint> mLineSeries_Deep = null;
    LineGraphSeries<DataPoint> mLineSeries_Awakenings = null;
    LineGraphSeries<DataPoint> mLineSeries_ZQscore = null;
    LineGraphSeries<DataPoint> mLineSeries_Goal = null;
    StackedBarGraphSeries<DataPoint> mStackedBarSeries = null;

    public boolean mShowBarsAndLines = false;
    public boolean mShowGoalLine = false;
    public boolean mShowTrendLine = false;
    public boolean mShowEntireTrendLine = false;
    public boolean mShowTimeToZ = false;
    public boolean mShowTotalSleep = false;
    public boolean mShowAwake = false;
    public boolean mShowREM = false;
    public boolean mShowLight = false;
    public boolean mShowDeep = false;
    public boolean mShowZQscore = false;
    public boolean mShowAwakenings = false;

    // member constants and other static content
    private static final String _CTAG = "TG";
    private static final int MAXFIELDS = 9;
    private SimpleDateFormat mDF1 = new SimpleDateFormat("MM/dd/yy");

    // data record
    public static class Trends_dataSet {
        public long mTimestamp = 0;
        public double mDataArray[] = new double[MAXFIELDS];

        // constructor
        public Trends_dataSet(long timestamp, double timeToZMin, double totalSleepMin, double remMin, double awakeMin, double lightMin, double deepMin, int awakeningsQty, int zq_score) {
            mTimestamp = timestamp;
            mDataArray[0] = timeToZMin;
            mDataArray[1] = totalSleepMin;
            mDataArray[2] = awakeMin;
            mDataArray[3] = remMin;
            mDataArray[4] = lightMin;
            mDataArray[5] = deepMin;
            mDataArray[6] = awakeningsQty;
            mDataArray[7] = zq_score;
            mDataArray[8] = timeToZMin + totalSleepMin +  awakeMin;
        }
    }

    // custom legend renderer
    public class TGV_LegendRenderer extends LegendRenderer {
        // constructor
        public TGV_LegendRenderer(GraphView graphView) {
            super(graphView);
        }

        // draw the legend to the left of the graph right next to the ending line point
        @Override
        public void draw(Canvas canvas) {
            float top = mGraphView.getGraphContentTop();
            float bottom = mGraphView.getGraphContentTop()+mGraphView.getGraphContentHeight();
            float left =  mGraphView.getGraphContentLeft()+mGraphView.getGraphContentWidth();
            float right = left + getLegendRenderLayoutWidth();

            if (mStyles.backgroundColor != Color.TRANSPARENT) {
                mPaint.setColor(mStyles.backgroundColor);
                canvas.drawRect(left, top, right, bottom, mPaint);
            }

            mPaint.setTextSize(mStyles.textSize);
            List<Series> mainSeries = mGraphView.getSeries();
            for (Series s : mainSeries) {
                if (s.getQtySubseries() <= -1) {
                    // this series has no subseries
                    String title = s.getTitle();
                    int len = s.size();
                    if (len > 0 && title != null) {
                        float y = s.getDrawY(mGraphView, len - 1) + mStyles.textSize / (float)2.0;
                        if (y > bottom) { y = bottom; }
                        mPaint.setColor(s.getColor());
                        canvas.drawText(title, left + mStyles.padding, y, mPaint);
                    }
                } else {
                    // this series has zero or more subseries
                    for (int j = 0; j < s.getQtySubseries(); j++) {
                        MultiSeries ms = (MultiSeries)s;
                        String title = ms.getTitle(j);
                        int len = ms.size(j);
                        if (len > 0 && title != null) {
                            float y = ms.getDrawY(mGraphView, j, len - 1) + mStyles.textSize / (float)2.0;
                            if (y > bottom) { y = bottom; }
                            mPaint.setColor(ms.getColor(j));
                            canvas.drawText(title, left + mStyles.padding, y, mPaint);
                        }
                    }
                }
            }
        }

        // get the width needed for the legend (takes away from main graphing area)
        @Override
        public int getLegendRenderLayoutWidth() {
            // width
            int legendWidth = mStyles.width;
            if (legendWidth == 0) {
                // auto
                legendWidth = cachedLegendWidth;
                if (legendWidth == 0) {
                    mPaint.setTextSize(mStyles.textSize);
                    Rect textBounds = new Rect();
                    mPaint.getTextBounds("Time2Z%", 0, 7, textBounds);
                    legendWidth = Math.max(legendWidth, textBounds.width());
                    List<Series> mainSeries = mGraphView.getSeries();
                    for (Series s : mainSeries) {
                        String title = s.getTitle();
                        if (title != null) {
                            mPaint.getTextBounds(title, 0, title.length(), textBounds);
                            legendWidth = Math.max(legendWidth, textBounds.width());
                        }
                    }
                    if (legendWidth > 0) {
                        legendWidth += (mStyles.padding * 2);
                        cachedLegendWidth = legendWidth;
                    }
                }
            }
            return legendWidth;
        }

        // get the height needed for the legend (takes away from main graphing area)
        @Override
        public int getLegendRenderLayoutHeight() {
            return 0;
        }
    }

    public class TGV_StaticLabelsFormatter extends StaticLabelsFormatter {
        public TGV_StaticLabelsFormatter(GraphView graphView, LabelFormatter dlf) {
            super(graphView, dlf);
        }

        @Override
        public String formatLabelEx(GridLabelRenderer.LabelFormatterReason reason, int index, double value, boolean isValueX) {    // CHANGE NOTICE: include reason and index# in the callback (used only in callback Overrides)
            switch (reason) {
                case SIZING:
                case SIZING_MAX:
                case SIZING_MIN:
                    if (isValueX && mHorizontalLabels != null) { return "00/00/00"; }
                    return mDynamicLabelFormatter.formatLabelEx(reason, index, value, isValueX);

                case AXIS_STEP:
                case AXIS_STEP_SECONDSCALE:
                    if (isValueX && mHorizontalLabels != null) {
                        double dateValue = 0.0;
                        switch (index) {
                            case 0:
                                dateValue = mViewport.getMinX(false);
                                break;
                            case 1:
                            case 2:
                            case 3:
                                double interval = (mViewport.getMaxX(false) - mViewport.getMinX(false)) / 4.0;
                                dateValue = mViewport.getMinX(false) + interval * index;
                                break;
                            case 4:
                                dateValue = mViewport.getMaxX(false);
                                break;
                        }
                        long ts = (long)dateValue;
                        ts = (ts * 60000L) + mLowestTimestamp;
                        Date dt = new Date(ts);
                        return mDF1.format(dt);
                    }
                    return mDynamicLabelFormatter.formatLabelEx(reason, index, value, isValueX);

                default:
                    // index cannot be utilized
                    return mDynamicLabelFormatter.formatLabelEx(reason, index, value, isValueX);
            }
        }
    }

    public class TGV_DefaultLabelFormatter extends DefaultLabelFormatter {
        @Override
        public String formatLabelEx(GridLabelRenderer.LabelFormatterReason reason, int index, double value, boolean isValueX) {
            switch (reason) {
                case SIZING:
                case SIZING_MAX:
                case SIZING_MIN:
                    // return the largest sized label
                    if (isValueX) {
                        return "00/00/00";
                    } else {
                        return "000%";
                    }

                case AXIS_STEP:
                case AXIS_STEP_SECONDSCALE:
                case DATA_POINT:
                default:
                    if (isValueX) {
                        // show the date
                        long ts = (long)value;
                        ts = (ts * 60000L) + mLowestTimestamp;
                        Date dt = new Date(ts);
                        return mDF1.format(dt);
                    } else {
                        // show the Y value
                        return String.format("%.0f",value)+"%";
                    }
            }
        }
    }

    // constructors
    public TrendsGraphView(Context context) { super(context); mContext = context; setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    public TrendsGraphView(Context context, AttributeSet attrs) { super(context, attrs); mContext = context; setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    public TrendsGraphView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); mContext = context; setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

    // toogle methods for the various lines available to show
    public void toggleBarsAndLines(boolean bars) { mShowBarsAndLines = bars; refresh(); }
    public void toggleTimeToZ(boolean show) { mShowTimeToZ = show; refresh(); }
    public void toggleTotalSleep(boolean show) { mShowTotalSleep = show; refresh(); }
    public void toggleREM(boolean show) { mShowREM = show; refresh(); }
    public void toggleAwake(boolean show) { mShowAwake = show; refresh(); }
    public void toggleLight(boolean show) { mShowLight = show; refresh(); }
    public void toggleDeep(boolean show) { mShowDeep = show; refresh(); }
    public void toggleAwakenings(boolean show) { mShowAwakenings = show; refresh(); }
    public void toggleZQ(boolean show) { mShowZQscore = show; refresh(); }
    public void toggleAllOff() {    // does not refresh
        mShowTimeToZ = false;
        mShowTotalSleep = false;
        mShowREM = false;
        mShowAwake = false;
        mShowLight = false;
        mShowDeep = false;
        mShowAwakenings = false;
        mShowZQscore = false;
    }

    // show just one line plus goal and trend (Dashboard Tab)
    public void prepareForDashboard(Point screenSize) {
        mShowAsMode = 1;
        mScreenSize = screenSize;
        GridLabelRenderer render = this.getGridLabelRenderer();
        render.setPadding(10);
        render.setHorizontalLabelsVisible(true);
        render.setVerticalLabelsVisible(true);
        render.setHorizontalLabelsColor(Color.WHITE);
        render.setVerticalLabelsColor(Color.WHITE);
        render.setLabelsSpace(5);
        render.setGridStyle(GridLabelRenderer.GridStyle.NONE);
        render.setLabelFormatter(new DateAsXAxisLabelFormatter(mContext));

        Viewport viewport = this.getViewport();
        viewport.setBackgroundColor(Color.LTGRAY);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0.0);
        viewport.setMaxY(105.0);
        render.setNumVerticalLabels(6);
        render.setVerticalLabelsEndY(100.0);
        viewport.setXAxisBoundsManual(true);    // for dates, must handle the X-axis manually
        render.setNumHorizontalLabels(2);

        TGV_DefaultLabelFormatter dlf = new TGV_DefaultLabelFormatter();
        render.setLabelFormatter(dlf);

        if (mScreenSize.x >= 1024) {
            setLegendRenderer(new TGV_LegendRenderer(this));
            LegendRenderer lr = getLegendRenderer();
            lr.setVisible(true);
            lr.setBackgroundColor(Color.LTGRAY);
            lr.setPadding(5);
        }

        mShowTimeToZ = false;
        mShowTotalSleep = false;
        mShowAwake = false;
        mShowREM = false;
        mShowLight = false;
        mShowDeep = true;
        mShowZQscore = false;
        mShowAwakenings = false;
        mShowGoalLine = true;
        mShowTrendLine = true;
        mShowEntireTrendLine = false;
    }

    // full capability graph
    public void prepareForStats(Point screenSize) {
        mShowAsMode = 2;
        mScreenSize = screenSize;
        GridLabelRenderer render = this.getGridLabelRenderer();
        render.setPadding(10);
        render.setHorizontalLabelsVisible(true);
        render.setVerticalLabelsVisible(true);
        render.setHorizontalLabelsColor(Color.WHITE);
        render.setVerticalLabelsColor(Color.WHITE);
        render.setLabelsSpace(5);
        render.setGridStyle(GridLabelRenderer.GridStyle.NONE);
        render.setLabelFormatter(new DateAsXAxisLabelFormatter(mContext));

        Viewport viewport = this.getViewport();
        viewport.setBackgroundColor(Color.LTGRAY);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0.0);
        viewport.setMaxY(105.0);
        viewport.setAxisMinY(0.0);
        viewport.setAxisMaxY(105.0);
        render.setNumVerticalLabels(6);
        render.setVerticalLabelsEndY(100.0);
        viewport.setXAxisBoundsManual(true);    // for dates, must handle the X-axis manually
        render.setNumHorizontalLabels(5);
        render.setHorizontalLabelsFixedPosition(true);

        TGV_DefaultLabelFormatter dlf = new TGV_DefaultLabelFormatter();
        TGV_StaticLabelsFormatter slf = new TGV_StaticLabelsFormatter(this, dlf);
        slf.setHorizontalLabels(new String[] {"00/00/00", "00/00/00", "00/00/00", "00/00/00", "00/00/00"});
        slf.setViewport(viewport);
        render.setLabelFormatter(slf);

        if (mScreenSize.x >= 1024) {
            setLegendRenderer(new TGV_LegendRenderer(this));
            LegendRenderer lr = getLegendRenderer();
            lr.setVisible(true);
            lr.setBackgroundColor(Color.LTGRAY);
            lr.setPadding(5);
        }

        viewport.setMinimumScaleWidth((float)10080.0);     // 7 days of minimum width in minutes
        viewport.setScrollable(true);
        viewport.setScalable(true);

        mShowTimeToZ = false;
        mShowTotalSleep = false;
        mShowAwake = false;
        mShowREM = false;
        mShowLight = false;
        mShowDeep = true;
        mShowZQscore = false;
        mShowAwakenings = false;
        mShowGoalLine = true;
        mShowTrendLine = true;
        mShowEntireTrendLine = false;
    }

    // prepare to create a bitmap rather than a view
    public void prepDrawToCanvas(int width, int height) {
        this.setLeft(0);
        this.setRight(width);
        this.setTop(0);
        this.setBottom(height);
        return;
    }

    // set the data for the trends graph; note that the passed dataset is in descending date order;
    // however GraphView mandates that X-values be in ascending value order; this will be handled in the buildSeries methods
    public void setDatasetForDashboard(ArrayList<Trends_dataSet> theData, double goalTotalSleep, double goalREMpct, double goalDeepPct) {
        mGoalTotalSleepMin = goalTotalSleep;
        mGoalREMpct = goalREMpct;
        mGoalDeepPct = goalDeepPct;
        mGoalLightPct = 100.0 - goalREMpct - goalDeepPct;
        mOrigDataSet = theData;
        mDatasetLen = theData.size();
        if (mShowAsMode == 1 && mDatasetLen > 7) { mDatasetLen = 7; }

        Trends_dataSet item = mOrigDataSet.get(mDatasetLen - 1);
        mLowestTimestamp = item.mTimestamp;

        refresh();
    }

    // rebuild the trends graph usually after a change in the line(s) to display
    private void refresh() {
        // first clear out any existing sets of series
        if (mQtySeries > 0) {
            removeAllSeries_deferRedraw();
            mLineSeries_TimeToZ = null;
            mLineSeries_TotalSleep = null;
            mLineSeries_Awake = null;
            mLineSeries_REM = null;
            mLineSeries_Light = null;
            mLineSeries_Deep = null;
            mLineSeries_Awakenings = null;
            mLineSeries_ZQscore = null;
            mLineSeries_Goal = null;
            mStackedBarSeries = null;
            mQtySeries = 0;
        }

        double lowestDate = 0.0;
        double highestDate = 0.0;
        if (mDatasetLen > 0) {
            Trends_dataSet item = mOrigDataSet.get(0);
            double nextDate = (double)((item.mTimestamp - mLowestTimestamp) / 60000L);
            lowestDate = nextDate;
            highestDate = nextDate;
            int i = 1;
            while (i < mDatasetLen) {
                item = mOrigDataSet.get(i);
                nextDate = (double)((item.mTimestamp - mLowestTimestamp) / 60000L);
                if (nextDate < lowestDate) { lowestDate = nextDate; }
                if (nextDate > highestDate) { highestDate = nextDate; }
                i++;
            }
        }
        Viewport viewport = this.getViewport();
        viewport.setMinX(lowestDate);
        viewport.setAxisMinX(lowestDate);
        viewport.setMaxX(highestDate);
        double addX = viewport.xPixelsToDeltaXvalue(10.0f);
        viewport.setMaxX(highestDate + addX);
        viewport.setAxisMaxX(highestDate + addX);
        GridLabelRenderer render = this.getGridLabelRenderer();
        render.setHorizontalLabelsEndX(highestDate);

        // begin building series and adding them to the graph
        // first up are those series that can be also be shown as a stackedBar
        int qtyFieldsShown = 0;
        double maxY = 0.0;
        if (mShowBarsAndLines) {
            if (mShowDeep || mShowLight || mShowREM || mShowAwake || mShowTimeToZ) {
                mStackedBarSeries = new StackedBarGraphSeries<DataPoint>();
                if (mShowDeep) {
                    if (buildBarSubseries(5)) {
                        int subseriesNo = mStackedBarSeries.getQtySubseries() - 1;
                        mStackedBarSeries.setColor(subseriesNo,  Color.rgb(0, 0, 204));  // dark blue
                        mStackedBarSeries.setTitle(subseriesNo, "Deep%");
                        qtyFieldsShown++;
                    }
                }
                if (mShowLight) {
                    if (buildBarSubseries(4)) {
                        int subseriesNo = mStackedBarSeries.getQtySubseries() - 1;
                        mStackedBarSeries.setColor(subseriesNo,  Color.rgb(102, 178, 255));  // light blue
                        mStackedBarSeries.setTitle(subseriesNo, "Light%");
                        qtyFieldsShown++;
                    }
                }
                if (mShowREM) {
                    if (buildBarSubseries(3)) {
                        int subseriesNo = mStackedBarSeries.getQtySubseries() - 1;
                        mStackedBarSeries.setColor(subseriesNo,  Color.rgb(0, 153, 0));  // green
                        mStackedBarSeries.setTitle(subseriesNo, "REM%");
                        qtyFieldsShown++;
                    }
                }
                if (mShowAwake) {
                    if (buildBarSubseries(2)) {
                        int subseriesNo = mStackedBarSeries.getQtySubseries() - 1;
                        mStackedBarSeries.setColor(subseriesNo,  Color.RED);
                        mStackedBarSeries.setTitle(subseriesNo, "Awake%");
                        qtyFieldsShown++;
                    }
                }
                if (mShowTimeToZ) {
                    if (buildBarSubseries(0)) {
                        int subseriesNo = mStackedBarSeries.getQtySubseries() - 1;
                        mStackedBarSeries.setColor(subseriesNo,  Color.rgb(255, 165, 0));   // orange
                        mStackedBarSeries.setTitle(subseriesNo, "Time2Z%");
                        qtyFieldsShown++;
                    }
                }
                double y = mStackedBarSeries.getHighestValueY();
                if (y > maxY) { maxY = y; }

                float pixels = viewport.deltaXvalueToXpixels(960.0);    // 16 hours in minutes
                mStackedBarSeries.setBarWidth(pixels);
                addSeries_deferRedraw(mStackedBarSeries);
                mQtySeries++;
            }
        } else {
            if (mShowDeep) {
                mLineSeries_Deep = buildLineSeries(5);
                if (mLineSeries_Deep != null) {
                    double y = mLineSeries_Deep.getHighestValueY();
                    if (y > maxY) { maxY = y; }
                    if (mShowAsMode == 1) { mLineSeries_Deep.setColor(Color.BLUE); }
                    else { mLineSeries_Deep.setColor(Color.rgb(0, 0, 204)); }    // dark blue
                    mLineSeries_Deep.setDrawDataPoints(true);
                    if (ZeoCompanionApplication.mScreenDensity > 1.0f) { mLineSeries_Deep.setThickness(5); mLineSeries_Deep.setDataPointsRadius(5); }
                    else { mLineSeries_Deep.setThickness(3); mLineSeries_Deep.setDataPointsRadius(3); }
                    mLineSeries_Deep.setTitle("Deep%");
                    addSeries_deferRedraw(mLineSeries_Deep);
                    mQtySeries++;
                    qtyFieldsShown++;
                }
            }
            if (mShowLight) {
                mLineSeries_Light = buildLineSeries(4);
                if (mLineSeries_Light != null) {
                    double y = mLineSeries_Light.getHighestValueY();
                    if (y > maxY) { maxY = y; }
                    if (mShowAsMode == 1) { mLineSeries_Light.setColor(Color.BLUE); }
                    else { mLineSeries_Light.setColor(Color.rgb(102, 178, 255)); }    // light blue
                    mLineSeries_Light.setDrawDataPoints(true);
                    if (ZeoCompanionApplication.mScreenDensity > 1.0f) { mLineSeries_Light.setThickness(5); mLineSeries_Light.setDataPointsRadius(5); }
                    else { mLineSeries_Light.setThickness(3); mLineSeries_Light.setDataPointsRadius(3); }
                    mLineSeries_Light.setTitle("Light%");
                    addSeries_deferRedraw(mLineSeries_Light);
                    mQtySeries++;
                    qtyFieldsShown++;
                }
            }
            if (mShowREM) {
                mLineSeries_REM = buildLineSeries(3);
                if (mLineSeries_REM != null) {
                    double y = mLineSeries_REM.getHighestValueY();
                    if (y > maxY) { maxY = y; }
                    if (mShowAsMode == 1) { mLineSeries_REM.setColor(Color.BLUE); }
                    else { mLineSeries_REM.setColor(Color.rgb(0, 153, 0)); }    // green
                    mLineSeries_REM.setDrawDataPoints(true);
                    if (ZeoCompanionApplication.mScreenDensity > 1.0f) { mLineSeries_REM.setThickness(5); mLineSeries_REM.setDataPointsRadius(5); }
                    else { mLineSeries_REM.setThickness(3); mLineSeries_REM.setDataPointsRadius(3); }
                    mLineSeries_REM.setTitle("REM%");
                    addSeries_deferRedraw(mLineSeries_REM);
                    mQtySeries++;
                    qtyFieldsShown++;
                }
            }
            if (mShowAwake) {
                mLineSeries_Awake = buildLineSeries(2);
                if (mLineSeries_Awake != null) {
                    double y = mLineSeries_Awake.getHighestValueY();
                    if (y > maxY) { maxY = y; }
                    if (mShowAsMode == 1) { mLineSeries_Awake.setColor(Color.BLUE); }
                    else { mLineSeries_Awake.setColor(Color.RED); }
                    mLineSeries_Awake.setDrawDataPoints(true);
                    if (ZeoCompanionApplication.mScreenDensity > 1.0f) { mLineSeries_Awake.setThickness(5); mLineSeries_Awake.setDataPointsRadius(5); }
                    else { mLineSeries_Awake.setThickness(3); mLineSeries_Awake.setDataPointsRadius(3); }
                    mLineSeries_Awake.setTitle("Awake%");
                    addSeries_deferRedraw(mLineSeries_Awake);
                    mQtySeries++;
                    qtyFieldsShown++;
                }
            }
            if (mShowTimeToZ) {
                mLineSeries_TimeToZ = buildLineSeries(0);
                if (mLineSeries_TimeToZ != null) {
                    double y = mLineSeries_TimeToZ.getHighestValueY();
                    if (y > maxY) { maxY = y; }
                    if (mShowAsMode == 1) { mLineSeries_TimeToZ.setColor(Color.BLUE); }
                    else { mLineSeries_TimeToZ.setColor(Color.rgb(255, 165, 0)); }  // orange
                    mLineSeries_TimeToZ.setDrawDataPoints(true);
                    if (ZeoCompanionApplication.mScreenDensity > 1.0f) { mLineSeries_TimeToZ.setThickness(5); mLineSeries_TimeToZ.setDataPointsRadius(5); }
                    else { mLineSeries_TimeToZ.setThickness(2); mLineSeries_TimeToZ.setDataPointsRadius(2); }
                    mLineSeries_TimeToZ.setTitle("Time2Z%");
                    addSeries_deferRedraw(mLineSeries_TimeToZ);
                    mQtySeries++;
                    qtyFieldsShown++;
                }
            }
        }

        // now those series that are always lines
        if (mShowTotalSleep) {
            mLineSeries_TotalSleep = buildLineSeries(1);
            if (mLineSeries_TotalSleep != null) {
                double y = mLineSeries_TotalSleep.getHighestValueY();
                if (y > maxY) { maxY = y; }
                if (mShowAsMode == 1) { mLineSeries_TotalSleep.setColor(Color.BLUE); }
                else { mLineSeries_TotalSleep.setColor(Color.BLACK); }
                mLineSeries_TotalSleep.setDrawDataPoints(true);
                if (ZeoCompanionApplication.mScreenDensity > 1.0f) { mLineSeries_TotalSleep.setThickness(5); mLineSeries_TotalSleep.setDataPointsRadius(5); }
                else { mLineSeries_TotalSleep.setThickness(3); mLineSeries_TotalSleep.setDataPointsRadius(3); }
                mLineSeries_TotalSleep.setTitle("Total%");
                addSeries_deferRedraw(mLineSeries_TotalSleep);
                mQtySeries++;
                qtyFieldsShown++;
            }
        }
        /*if (mShowAwakenings) {
            mSeries_Awakenings = buildLineSeries(6);
            if (mSeries_Awakenings != null) {
                double y = mSeries_Awakenings.getHighestValueY();
                if (y > maxY) { maxY = y; }
                if (mShowAsMode == 1) { mSeries_Awakenings.setColor(Color.BLUE); }
                else { mSeries_Awakenings.setColor(Color.MAGENTA); }
                mSeries_Awakenings.setDrawDataPoints(true);
                mSeries_Awakenings.setDataPointsRadius(5);
                mSeries_Awakenings.setTitle("Awaken#");
                addSeries_deferRedraw(mSeries_Awakenings);
                mQtySeries++;
                qtyFieldsShown++;
            }
        }*/
        if (mShowZQscore) {
            mLineSeries_ZQscore = buildLineSeries(7);
            if (mLineSeries_ZQscore != null) {
                double y = mLineSeries_ZQscore.getHighestValueY();
                if (y > maxY) { maxY = y; }
                if (mShowAsMode == 1) { mLineSeries_ZQscore.setColor(Color.BLUE); }
                else { mLineSeries_ZQscore.setColor(Color.WHITE); }
                mLineSeries_ZQscore.setDrawDataPoints(true);
                if (ZeoCompanionApplication.mScreenDensity > 1.0f) { mLineSeries_ZQscore.setThickness(5); mLineSeries_ZQscore.setDataPointsRadius(5); }
                else { mLineSeries_ZQscore.setThickness(3); mLineSeries_ZQscore.setDataPointsRadius(3); }
                mLineSeries_ZQscore.setTitle("ZQ");
                addSeries_deferRedraw(mLineSeries_ZQscore);
                mQtySeries++;
                qtyFieldsShown++;
            }
        }

        if (mShowGoalLine && qtyFieldsShown == 1) {
            if (mDatasetLen > 0) {
                double goal = getGoal();
                if (goal > 0.0) {
                    DataPoint[] goalDataPoints = new DataPoint[2];
                    goalDataPoints[0] = new DataPoint(0, lowestDate, goal);
                    goalDataPoints[1] = new DataPoint(1, highestDate, goal);
                    mLineSeries_Goal = new LineGraphSeries<DataPoint>(goalDataPoints);
                    double y = mLineSeries_Goal.getHighestValueY();
                    if (y > maxY) { maxY = y; }
                    mLineSeries_Goal.setColor(Color.GRAY);
                    mLineSeries_Goal.setDrawDataPoints(false);
                    if (ZeoCompanionApplication.mScreenDensity > 1.0f) { mLineSeries_Goal.setThickness(5); }
                    else { mLineSeries_Goal.setThickness(3); }
                    mLineSeries_Goal.setTitle("Goal");
                    addSeries_deferRedraw(mLineSeries_Goal);
                    mQtySeries++;
                }
            }
        }

        if (maxY < 25.0) {
            maxY = 25.0;
        } else if (maxY < 50.0) {
            maxY = 50.0;
        } else if (maxY < 75.0) {
            maxY = 75.0;
        } else if (maxY < 100.0) {
            maxY = 100.0;
        } else if (maxY < 125.0) {
            maxY = 125.0;
        } else if (maxY < 150.0) {
            maxY = 150.0;
        } else if (maxY < 175.0) {
            maxY = 175.0;
        }
        viewport.setMaxY(maxY + 5.0);
        viewport.setAxisMaxY(maxY + 5.0);
        render.setVerticalLabelsEndY(maxY);

        // now redraw the entire graph
        onDataChanged(false, false);
    }

    // get the proper goal value for the shown data field; only used in show-single-line mode
    private double getGoal() {
        if (mShowREM) {
            return mGoalREMpct;
        } else if (mShowLight) {
            return mGoalLightPct;
        } else if (mShowDeep) {
            return mGoalDeepPct;
        } else if (mShowTotalSleep) {
            return 100.0;
        } else {
            return 0.0;
        }
    }

    // build a particular line series for a single data field
    private LineGraphSeries<DataPoint> buildLineSeries(int dataArrayIndex) {
        DataPoint[] theDataPoints = buildDataPoints(dataArrayIndex);
        if (theDataPoints == null) { return null; }
        return new LineGraphSeries<DataPoint>(theDataPoints);
    }

    // build a particular startedBar series for a single data field
    private boolean buildBarSubseries(int dataArrayIndex) {
        DataPoint[] theDataPoints = buildDataPoints(dataArrayIndex);
        if (theDataPoints == null) { return false; }
        mStackedBarSeries.addSubseries(theDataPoints);
        return true;
    }

    // build the data points for a single data field; note the X-values are in descending order but GraphView must have them in ascending order
    private DataPoint[] buildDataPoints(int dataArrayIndex) {
        if (mDatasetLen <= 0) { return null; }
        DataPoint[] theDataPoints = new DataPoint[mDatasetLen];
        int j = 0;
        for (int i = mDatasetLen - 1; i >= 0; i--) {
            Trends_dataSet item = mOrigDataSet.get(i);
            double y = 0.0;
            switch (dataArrayIndex) {
                case 1:
                    // total sleep (min); percentage to goal
                    if (mGoalTotalSleepMin == 0.0) { y = 0.0; }
                    else { y = item.mDataArray[1] / mGoalTotalSleepMin * 100.0; }
                    break;
                case 0:
                case 2:
                case 3:
                case 4:
                case 5:
                    // time-to-Z, awake, REM, light, deep (all min); percentage to total duration
                    if (item.mDataArray[8] == 0.0) { y = 0.0; }
                    else { y = item.mDataArray[dataArrayIndex] / item.mDataArray[8] * 100.0; }
                    break;
                case 6:
                    // qty awakenings (count)
                    break;
                case 7:
                    // ZQ score is generally 0 to 100, but could go higher than 100
                    y = item.mDataArray[dataArrayIndex];
                    break;
            }
            double x = (double)((item.mTimestamp - mLowestTimestamp)/60000L);
            theDataPoints[j] = new DataPoint(i, x, y);
            j++;
        }
        return theDataPoints;
    }

    // set a scrolling and scaling callback listener
    public void setScrollScaleListener(long callbackNumber, Viewport.ScrollScaleListener listener) {
        mParentNumber = callbackNumber;
        Viewport viewport = this.getViewport();
        viewport.setScrollScaleListener(listener);
    }
}

