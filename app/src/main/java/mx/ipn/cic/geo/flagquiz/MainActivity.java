package mx.ipn.cic.geo.flagquiz;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    // Keys for reading data from SharedPreferences.
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true; // Used to force portrait mode.
    private boolean preferencesChanged = true; // The user changed preferences.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set default values in the app's SharedPreferences.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Register listener for SharedPreferences changes.
        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(preferencesChangeListener);

        // Determine screen size.
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        // If the device is a tablet, set phoneDevice to false.
        if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
        {
            phoneDevice = false;
        }

        // If the app is running on phone-sized device, allow only portrait orientation.
        if(phoneDevice == true)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Get the device's current orientation.
        int orientation = getResources().getConfiguration().orientation;

        // Display the app's menu only in portrait orientation.
        if(orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(preferencesChanged) {
            // Now that the default preferences have been set, initialize MainActivityFragment
            // and start the quiz.
            MainActivityFragment quizFragment = (MainActivityFragment)getSupportFragmentManager().
                    findFragmentById(R.id.quizFragment);
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    }

    // Listener for changes to the app's SharedPreferences.
    private OnSharedPreferenceChangeListener preferencesChangeListener =
        new OnSharedPreferenceChangeListener() {
            // Called when the user changes the app's preferences.
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                preferencesChanged = true;  // User changed app settings.

                MainActivityFragment quizFragment = (MainActivityFragment)getSupportFragmentManager().
                    findFragmentById(R.id.quizFragment);

                if(key.equals(CHOICES))
                {
                    // Number of choices to display changed.
                    quizFragment.updateGuessRows(sharedPreferences);
                    quizFragment.resetQuiz();
                }
                else if (key.equals(REGIONS))
                {
                    // Regions to include changed.
                    Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                    if(regions != null && regions.size() > 0)
                    {
                        quizFragment.updateRegions(sharedPreferences);
                        quizFragment.resetQuiz();
                    }
                    else
                    {
                        // Must select one region-set, set North America as default.
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        regions.add(getString(R.string.default_region));
                        editor.putStringSet(REGIONS, regions);
                        editor.apply();

                        Toast.makeText(MainActivity.this,
                            R.string.default_region_message,
                            Toast.LENGTH_SHORT).show();
                    }
                }
                Toast.makeText(MainActivity.this,
                    R.string.restarting_quiz,
                    Toast.LENGTH_SHORT).show();
            }
        };
}
