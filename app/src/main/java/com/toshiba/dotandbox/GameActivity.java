package com.toshiba.dotandbox;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class GameActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        DotsLayoutView customView = findViewById(R.id.dotCustomView);
        TextView player1score = findViewById(R.id.player1score_id);
        TextView player2score = findViewById(R.id.player2score_id);
        TextView declarewinner = findViewById(R.id.winnerdeclare_id);
        TextView player1name = findViewById(R.id.player1name_id);
        TextView player2name = findViewById(R.id.player2name_id);
        FrameLayout fm1 = findViewById(R.id.frame_p1);
        FrameLayout fm2 = findViewById(R.id.frame_p2);
        TextView loadingPlayer1 = findViewById(R.id.loadingPlayer1);
        TextView loadingPlayer2 = findViewById(R.id.loadingPlayer2);
        customView.setviews(player1score, player2score, declarewinner, player1name, player2name, fm1, fm2, loadingPlayer1, loadingPlayer2);

        // Retrieve the values from the Intent
        Intent intent = getIntent();
        int rows = intent.getIntExtra("ROWS_VALUE", 5);
        int cols = intent.getIntExtra("COLS_VALUE", 5);
        boolean singlePlayer = intent.getBooleanExtra("MODE_VALUE", true); // true is the default value if not found
        customView.setValues(singlePlayer, rows, cols);

        Button homeButton = findViewById(R.id.button_home);

        homeButton.setOnClickListener(v -> finish());   // to return to home screen on home button press

        Button restartButton = findViewById(R.id.button_restart);
        restartButton.setOnClickListener(view -> {
            // Restart the activity
            restartActivity();
        });
    }
    private void restartActivity() {
        Intent intent = getIntent();
        finish(); // Finish the current instance
        startActivity(intent); // Start a new instance with same passed intents from main activity to restart the game
    }
}
