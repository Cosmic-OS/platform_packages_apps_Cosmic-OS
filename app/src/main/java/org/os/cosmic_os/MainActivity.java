package org.os.cosmic_os;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationViewPager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    AHBottomNavigation ahBottomNavigation;
    AHBottomNavigationAdapter ahBottomNavigationAdapter;
    AHBottomNavigationViewPager ahBottomNavigationViewPager;
    FloatingActionButton g_fab;
    String url = "https://raw.githubusercontent.com/Cosmic-OS/platform_vendor_cos/oreo-mr1/team.json";
    String cosmicG = "https://plus.google.com/communities/116339021564888810193";
    int intervals= 86400000; //24 hours
    JobScheduler jobScheduler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        checkNetworkState();
        if (getSupportActionBar() != null)getSupportActionBar().setDisplayShowTitleEnabled(false);
        g_fab = findViewById(R.id.g_fab);
        //Download the json file from repo
        final DownloadTask downloadTask = new DownloadTask(this);
        downloadTask.execute(url);
        initUI();
        jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean("firstTime",false))
        {
            OTAJobScheduler();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstTime",true);
            editor.apply();
        }
    }

    private void checkNetworkState() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null)
        {
            new AlertDialog.Builder(this)
            .setTitle("No Network")
            .setMessage("Most features need network to work. Please consider turning on Wifi or Mobile Network")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            })
            .show();
        }
    }

    private void OTAJobScheduler() {
        assert jobScheduler != null;
        jobScheduler.schedule(new JobInfo.Builder(0,new ComponentName(this,OTAService.class)).setPeriodic(intervals).build());
    }

    private void initUI() {
        //setup the AHViewPager
        ahBottomNavigationViewPager = findViewById(R.id.viewPager);
        ahBottomNavigationViewPager.setAdapter(new MyPageAdapter(getSupportFragmentManager()));
        ahBottomNavigationViewPager.setPageTransformer(false, new FadePageTransform() );

        //setup the bottom navigation tab
        ahBottomNavigation = findViewById(R.id.bottom_navigation);
        ahBottomNavigationAdapter = new AHBottomNavigationAdapter(this,R.menu.bottom_bar_menu);
        ahBottomNavigationAdapter.setupWithBottomNavigation(ahBottomNavigation);
        ahBottomNavigation.setBehaviorTranslationEnabled(true);
        ahBottomNavigation.manageFloatingActionButtonBehavior(g_fab);
        g_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent gplusIntent = new Intent(Intent.ACTION_VIEW);
                gplusIntent.setData(Uri.parse(cosmicG));
                startActivity(gplusIntent);
            }
        });
        ahBottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                if(!wasSelected)ahBottomNavigationViewPager.setCurrentItem(position);
                if (position == 3)g_fab.setVisibility(View.VISIBLE);
                else g_fab.setVisibility(View.GONE);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.fragment_update_interval, null);

            final BottomSheetDialog dialog = new BottomSheetDialog(this);
            dialog.setContentView(view);
            dialog.show();

            TextView update4hours = view.findViewById(R.id.interval_4);
            update4hours.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intervals=14400000;
                    jobScheduler.cancel(0);
                    jobScheduler.schedule(new JobInfo.Builder(0,new ComponentName(getApplicationContext(),OTAService.class)).setPeriodic(intervals).build());
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(),"Check interval set to 4 Hours",Toast.LENGTH_LONG).show();
                }
            });
            TextView update8hours = view.findViewById(R.id.interval_8);
            update8hours.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intervals=28800000;
                    jobScheduler.cancel(0);
                    jobScheduler.schedule(new JobInfo.Builder(0,new ComponentName(getApplicationContext(),OTAService.class)).setPeriodic(intervals).build());
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(),"Check interval set to 8 Hours",Toast.LENGTH_LONG).show();
                }
            });
            TextView update12hours = view.findViewById(R.id.interval_12);
            update12hours.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intervals=43200000;
                    jobScheduler.cancel(0);
                    jobScheduler.schedule(new JobInfo.Builder(0,new ComponentName(getApplicationContext(),OTAService.class)).setPeriodic(intervals).build());
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(),"Check interval set to 12 Hours",Toast.LENGTH_LONG).show();
                }
            });
            TextView update24hours = view.findViewById(R.id.interval_24);
            update24hours.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intervals=86400000;
                    jobScheduler.cancel(0);
                    jobScheduler.schedule(new JobInfo.Builder(0,new ComponentName(getApplicationContext(),OTAService.class)).setPeriodic(intervals).build());
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(),"Check interval set to 24 Hours",Toast.LENGTH_LONG).show();
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class MyPageAdapter extends FragmentStatePagerAdapter {
        MyPageAdapter(FragmentManager fm) {
            super(fm);
        }

        //Handle to create fragment instances
        @Override
        public Fragment getItem(int position) {
            switch (position)
            {
                case 0 : return HomeFragment.newInstance();
                case 1 : return ChangelogFragment.newInstance();
                case 2 : return TeamFragment.newInstance();
                case 3 : return DonateFragment.newInstance();
            }
            return HomeFragment.newInstance();
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

    public static class DownloadTask extends AsyncTask<String, Integer, String> {
        String file;
        DownloadTask(Context getContext) {
            file = getContext.getExternalFilesDir(null)+"/"+"team.json";
        }

        @Override
        protected String doInBackground(String... strings) {
            InputStream inputStream;
            OutputStream outputStream;
            HttpURLConnection httpURLConnection;
            try {
                URL url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();
                if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + httpURLConnection.getResponseCode()
                            + " " + httpURLConnection.getResponseMessage();
                }
                inputStream = httpURLConnection.getInputStream();
                outputStream = new FileOutputStream(new File(file));
                byte data[] = new byte[4096];
                int count;
                while ((count = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, count);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    //FadeOut and FadeIn animation for ViewPager
    public class FadePageTransform implements ViewPager.PageTransformer{

        @Override
        public void transformPage(@NonNull View page, float position) {
            if(position <= -1.0F || position >= 1.0F) {
                page.setTranslationX(page.getWidth() * position);
                page.setAlpha(0.0F);
            } else if( position == 0.0F ) {
                page.setTranslationX(page.getWidth() * position);
                page.setAlpha(1.0F);
            } else {
                // position is between -1.0F & 0.0F OR 0.0F & 1.0F
                page.setTranslationX(page.getWidth() * -position);
                page.setAlpha(1.0F - Math.abs(position));
            }
        }
    }
}
