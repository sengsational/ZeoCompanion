package opensource.zeocompanion.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RadioButton;

import com.obscuredPreferences.ObscuredPrefs;

import java.util.ArrayList;

import opensource.zeocompanion.MainActivity;
import opensource.zeocompanion.R;
import opensource.zeocompanion.ZeoCompanionApplication;
import opensource.zeocompanion.activities.StatsActivity;
import opensource.zeocompanion.utility.JournalDataCoordinator;
import opensource.zeocompanion.views.DailyResultGraphView;
import opensource.zeocompanion.views.TrendsGraphView;

// fragment within the MainActivity that displays simple non-configurable statistical graphs
public class MainDashboardFragment extends MainFragmentWrapper {
    // member variables
    private View mRootView = null;
    private boolean mLayoutDone = false;

    // member constants and other static content
    private static final String _CTAG = "M1F";

    // common listener for presses on the radio buttons
    View.OnClickListener mRadioButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean checked = ((RadioButton)view).isChecked();
            TrendsGraphView theDailyResults = (TrendsGraphView)mRootView.findViewById(R.id.graph_trends);

            // Check which radio button was clicked
            switch(view.getId()) {
                case R.id.radioButton_deep:
                    if (checked) {
                        theDailyResults.toggleAllOff();
                        theDailyResults.toggleDeep(true);
                    }
                    break;
                case R.id.radioButton_rem:
                    if (checked) {
                        theDailyResults.toggleAllOff();
                        theDailyResults.toggleREM(true);
                    }
                    break;
                case R.id.radioButton_light:
                    if (checked) {
                        theDailyResults.toggleAllOff();
                        theDailyResults.toggleLight(true);
                    }
                    break;
                case R.id.radioButton_awake:
                    if (checked) {
                        theDailyResults.toggleAllOff();
                        theDailyResults.toggleAwake(true);
                    }
                    break;
                case R.id.radioButton_time2z:
                    if (checked) {
                        theDailyResults.toggleAllOff();
                        theDailyResults.toggleTimeToZ(true);
                    }
                    break;
                case R.id.radioButton_total:
                    if (checked) {
                        theDailyResults.toggleAllOff();
                        theDailyResults.toggleTotalSleep(true);
                    }
                    break;
                case R.id.radioButton_zq:
                    if (checked) {
                        theDailyResults.toggleAllOff();
                        theDailyResults.toggleZQ(true);
                    }
                    break;
            }
        }
    };

    private View.OnClickListener mGraphClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), StatsActivity.class);
            intent.putExtra("startTab", 0);
            startActivity(intent);
        }
    };

    // constructor
    public MainDashboardFragment() {}

    // instanciator
    public static MainDashboardFragment newInstance() { return new MainDashboardFragment(); }

    // called by the framework to create the Fragment's view contents
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Log.d(_CTAG+".onCreateView", "==========FRAG ON-CREATEVIEW=====");
        mRootView = inflater.inflate(R.layout.fragment_main_dashboard, container, false);

        // setup all the radio buttons
        RadioButton rb = (RadioButton)mRootView.findViewById(R.id.radioButton_deep);
        rb.setChecked(true);
        rb.setOnClickListener(mRadioButtonOnClickListener);
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_rem);
        rb.setOnClickListener(mRadioButtonOnClickListener);
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_light);
        rb.setOnClickListener(mRadioButtonOnClickListener);
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_awake);
        rb.setOnClickListener(mRadioButtonOnClickListener);
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_time2z);
        rb.setOnClickListener(mRadioButtonOnClickListener);
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_total);
        rb.setOnClickListener(mRadioButtonOnClickListener);
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_zq);
        rb.setOnClickListener(mRadioButtonOnClickListener);

        final TrendsGraphView theDailyResults = (TrendsGraphView)mRootView.findViewById(R.id.graph_trends);
        theDailyResults.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Ensure you call it only once
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    theDailyResults.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    theDailyResults.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                mLayoutDone = true;
                createGraphs();
            }
        });

        return mRootView;
    }

    // Called by the framework when the fragment's view has been detached from the fragment (counterpart to onCreateView)
    @Override
    public void onDestroyView () {
        mLayoutDone = false;
        super.onDestroyView();
        //Log.d(_CTAG + ".onDestroyView", "==========FRAG ON-DESTROYVIEW=====");
    }

    // Called when the fragment is visible to the user and actively running
    @Override
    public void onResume() {
        super.onResume();
        //Log.d(_CTAG + ".onResume", "==========FRAG ON-RESUME=====");
        if (mLayoutDone) { createGraphs(); }
    }

    // Called when the Fragment is no longer resumed
    @Override
    public void onPause () {
        super.onPause();
        //Log.d(_CTAG + ".onPause", "==========FRAG ON-PAUSE=====");
    }

    // called by the MainActivity when handlers or settings have made changes to the database
    // or to settings options, etc
    @Override
    public void needToRefresh() {
        // TODO V1.1 Dashboard tab
    }

    // create the various graphs; should have completed layout
    private void createGraphs() {
        // get user's sleep goals (if any)
        double goalTotalSleepMin = 480.0;
        double goalDeepPct = 15.0;
        double goalREMpct = 20.0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String wStr = ObscuredPrefs.decryptString(prefs.getString("profile_goal_hours_per_night", "8"));
        if (!wStr.isEmpty()) {
            double d = Double.parseDouble(wStr);
            if (d > 0.0) { goalTotalSleepMin = d * 60.0; }
        }
        wStr = ObscuredPrefs.decryptString(prefs.getString("profile_goal_percent_deep", "15"));
        if (!wStr.isEmpty()) {
            double d = Double.parseDouble(wStr);
            if (d > 0.0 && d <= 100.0) { goalDeepPct = d;  }
        }
        wStr  = ObscuredPrefs.decryptString(prefs.getString("profile_goal_percent_REM", "20"));
        if (!wStr.isEmpty()) {
            double d = Double.parseDouble(wStr);
            if (d > 0.0 && d <= 100.0) { goalREMpct = d;  }
        }

        ArrayList<JournalDataCoordinator.IntegratedHistoryRec> theIrecs = new ArrayList<JournalDataCoordinator.IntegratedHistoryRec>();
        ArrayList<TrendsGraphView.Trends_dataSet> theData = new ArrayList<TrendsGraphView.Trends_dataSet>();
        ZeoCompanionApplication.mCoordinator.getAllIntegratedHistoryRecs(theIrecs); // sorted newest to oldest
        for (JournalDataCoordinator.IntegratedHistoryRec iRec: theIrecs) {
            if (iRec.theZAH_SleepRecord != null) {
                TrendsGraphView.Trends_dataSet ds = new TrendsGraphView.Trends_dataSet(iRec.theZAH_SleepRecord.rStartOfNight, iRec.theZAH_SleepRecord.rTime_to_Z_min,
                        iRec.theZAH_SleepRecord.rTime_Total_Z_min, iRec.theZAH_SleepRecord.rTime_REM_min, iRec.theZAH_SleepRecord.rTime_Awake_min,
                        iRec.theZAH_SleepRecord.rTime_Light_min, iRec.theZAH_SleepRecord.rTime_Deep_min, iRec.theZAH_SleepRecord.rCountAwakenings,
                        iRec.theZAH_SleepRecord.rZQ_Score);
                theData.add(ds);
            }
        }

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);

        TrendsGraphView theDailyResults = (TrendsGraphView)mRootView.findViewById(R.id.graph_trends);
        theDailyResults.prepareForDashboard(screenSize);
        whichIsChecked();
        theDailyResults.setDatasetForDashboard(theData, goalTotalSleepMin, goalREMpct, goalDeepPct);
        theDailyResults.setOnClickListener(mGraphClickListener);
    }

    // determine which radio button is already checked
    private void whichIsChecked() {
        TrendsGraphView theDailyResults = (TrendsGraphView)mRootView.findViewById(R.id.graph_trends);
        theDailyResults.toggleAllOff();
        RadioButton rb = (RadioButton)mRootView.findViewById(R.id.radioButton_deep);
        if (rb.isChecked()) { theDailyResults.mShowDeep = true; return; };
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_rem);
        if (rb.isChecked()) { theDailyResults.mShowREM = true; return; };
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_light);
        if (rb.isChecked()) { theDailyResults.mShowLight = true; return; };
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_awake);
        if (rb.isChecked()) { theDailyResults.mShowAwake = true; return; };
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_time2z);
        if (rb.isChecked()) { theDailyResults.mShowTimeToZ = true; return; };
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_total);
        if (rb.isChecked()) { theDailyResults.mShowTotalSleep = true; return; };
        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_zq);
        if (rb.isChecked()) { theDailyResults.mShowZQscore = true; return; };

        rb = (RadioButton)mRootView.findViewById(R.id.radioButton_deep);
        rb.setChecked(true);
        theDailyResults.mShowDeep = true;
    }
}
