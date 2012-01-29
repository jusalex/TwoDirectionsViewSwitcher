package ru.example;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import ru.android.view.TwoDirectionsViewSwitcher;

public class Example extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TwoDirectionsViewSwitcher viewSwitcher = new TwoDirectionsViewSwitcher(getApplicationContext(), 3);

        final int[] backgroundColors = { Color.RED, Color.BLUE, Color.CYAN, Color.GREEN,
                Color.YELLOW, Color.MAGENTA, Color.CYAN,
                Color.GRAY, Color.LTGRAY};
        for (int i = 0; i <= 8; i++) {
            TextView textView = new TextView(getApplicationContext());
            textView.setText(Integer.toString(i + 1));
            textView.setTextSize(100);
            textView.setTextColor(Color.BLACK);
            textView.setGravity(Gravity.CENTER);
            textView.setBackgroundColor(backgroundColors[i]);
            viewSwitcher.addView(textView);
        }
        setContentView(viewSwitcher);
        viewSwitcher.setOnScreenSwitchListener(onScreenSwitchListener);
    }

    private final TwoDirectionsViewSwitcher.OnScreenSwitchListener onScreenSwitchListener = new TwoDirectionsViewSwitcher.OnScreenSwitchListener() {

        public void onScreenSwitched(int screen) {
            Log.d("TwoDirectionsViewSwitcher", "switched to screen: " + screen);
        }

    };

}

