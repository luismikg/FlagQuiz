package mx.ipn.cic.geo.flagquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */

public class MainActivityFragment extends Fragment {
    // String used when logging error messages
    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList; // Flag file names
    private List<String> quizCountriesList; // Countries in current quiz
    private Set<String> regionsSet; // World regions in current quiz
    private String correctAnswer; // Correct country for the current flag
    private int totalGuesses; // Number of guesses made
    private int correctAnswers; // Number of correct guesses
    private int guessRows; // Number of rows displaying guess Buttons
    private SecureRandom random; // Used to randomize the quiz
    private Handler handler; // Used to delay loading next flag
    private Animation shakeAnimation; // Animation for incorrect guess

    private LinearLayout quizLinearLayout; // Layout that contains the quiz
    private TextView questionNumberTextView; // Shows current question #
    private ImageView flagImageView; // Displays a flag
    private LinearLayout[] guessLinearLayouts; // Rows of answer Buttons
    private TextView answerTextView; // Displays correct answer

    // Configures the MainActivityFragment when its View is created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        // Load the shake animation that's used for incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // animation repeats 3 times

        // Get references to GUI components
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        // Configure listeners for the guess Buttons
        for (LinearLayout row : guessLinearLayouts)
        {
            for (int column = 0; column < row.getChildCount(); column++)
            {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        // Set questionNumberTextView's text
        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));
        return view; // Return the fragment's view for display
    }

    // Update guessRows based on value in SharedPreferences
    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // Get the number of guess buttons that should be displayed
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choices) / 2;

        // Hide all guess button LinearLayouts
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        // Display appropriate guess button LinearLayouts
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    // Update world regions for quiz based on values in SharedPreferences
    public void updateRegions(SharedPreferences sharedPreferences)
    {
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    // Set up and start the next quiz
    public void resetQuiz() {
        // Use AssetManager to get image file names for enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); // Empty list of image file names

        try {
            // Loop through each region
            for (String region : regionsSet)
            {
                // Get a list of all flag image files in this region
                String[] paths = assets.list(region);

                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }

        correctAnswers = 0; // Reset the number of correct answers made
        totalGuesses = 0; // Reset the total number of guesses the user made
        quizCountriesList.clear(); // Clear prior list of quiz countries

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        // Add FLAGS_IN_QUIZ random file names to the quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfFlags);

            // Get the random file name
            String filename = fileNameList.get(randomIndex);

            // If the region is enabled and it hasn't already been chosen
            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename); // Add the file to the list
                ++flagCounter;
            }
        }

        loadNextFlag(); // Start the quiz by loading the first flag
    }

    // After the user guesses a correct flag, load the next flag
    private void loadNextFlag()
    {
        // Get file name of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; // Update the correct answer
        answerTextView.setText(""); // Clear answerTextView

        // Display current question number
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        // Extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        // Use AssetManager to load next image from assets folder
        AssetManager assets = getActivity().getAssets();

        // Get an InputStream to the asset representing the next flag
        // and try to use the InputStream
        try (InputStream stream = assets.open(region + "/" + nextImage + ".png")) {
            // Load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            animate(false); // Animate the flag onto the screen
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // Shuffle file names

        // Put the correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // Add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++)
        {
            // Place Buttons in currentTableRow
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++)
            {
                // Get reference to Button to configure
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // Get country name and set it as newGuessButton's text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        // Randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // Pick random row
        int column = random.nextInt(2); // Pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; // Get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    // Parses the country flag file name and returns the country name
    private String getCountryName(String name)
    {

        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    // Animates the entire quizLinearLayout on or off screen
    private void animate(boolean animateOut)
    {
        // Prevent animation into the the UI for the first flag
        if (correctAnswers == 0)
            return;

        // Calculate center x and center y
        // Calculate center x
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        // Calculate center y
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;

        // Calculate animation radius
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        // If the quizLinearLayout should animate out rather than in
        if (animateOut)
        {
            // Create circular reveal animation
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(new AnimatorListenerAdapter()
            {
                // Called when the animation finishes
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    loadNextFlag();
                }
            }
            );
        }
        else
        {
            // If the quizLinearLayout should animate in
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, 0, radius);
        }

        animator.setDuration(500); // Set animation duration to 500 ms
        animator.start(); // Start the animation
    }

    // Called when a guess Button is touched
    private View.OnClickListener guessButtonListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses; // Increment number of guesses the user has made

            if (guess.equals(answer)) { // If the guess is correct
                ++correctAnswers; // Increment the number of correct answers

                // Display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer,
                        getContext().getTheme()));

                disableButtons(); // Disable all guess Buttons

                // If the user has correctly identified FLAGS_IN_QUIZ flags
                if (correctAnswers == FLAGS_IN_QUIZ)
                {
                    // DialogFragment to display quiz stats and start new quiz
                    DialogFragment quizResults = new DialogFragment()
                    {
                        // Create an AlertDialog and return it
                        @Override
                        public Dialog onCreateDialog(Bundle bundle)
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results,
                                    totalGuesses, (1000 / (double) totalGuesses)));

                            // "Reset Quiz" Button
                            builder.setPositiveButton(R.string.reset_quiz,
                                    new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            resetQuiz();
                                        }
                                    }
                            );
                            return builder.create(); // Return the AlertDialog
                        }
                    };

                    // Use FragmentManager to display the DialogFragment
                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");
                }
                else
                {
                    // Answer is correct but quiz is not over load the next flag after a 2-second delay
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            animate(true); // animate the flag off the screen
                        }
                    }, 2000); // 2000 milliseconds for 2-second delay
                }
            }
            else
            {
                // Answer was incorrect
                flagImageView.startAnimation(shakeAnimation); // Play shake

                // Display "Incorrect!" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer,
                        getContext().getTheme()));
                guessButton.setEnabled(false); // Disable incorrect answer
            }
        }
    };

    // Utility method that disables all answer Buttons
    private void disableButtons()
    {
        for (int row = 0; row < guessRows; row++)
        {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }
}

