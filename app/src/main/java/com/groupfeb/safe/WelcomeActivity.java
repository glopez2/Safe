package com.groupfeb.safe;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {

    private Button welcomeHelperButton;
    private Button welcomeWalkerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        welcomeWalkerButton = (Button) findViewById(R.id.welcome_walker_btn);
        welcomeHelperButton = (Button) findViewById(R.id.welcome_helper_btn);

        welcomeWalkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginRegisterWalkerIntent = new Intent(WelcomeActivity.this, WalkerLoginRegisterActivity.class);
                startActivity(loginRegisterWalkerIntent);
            }
        });

        welcomeHelperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginRegisterHelperIntent = new Intent(WelcomeActivity.this, HelperLoginRegisterActivity.class);
                startActivity(loginRegisterHelperIntent);
            }
        });
    }
}
