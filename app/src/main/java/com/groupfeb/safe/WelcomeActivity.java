package com.groupfeb.safe;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    private Button HelperWelcomeButton;
    private Button WalkerWelcomeButton;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListner;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


//        mAuth = FirebaseAuth.getInstance();
//
//        firebaseAuthListner = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
//            {
//                currentUser = FirebaseAuth.getInstance().getCurrentUser();
//
//                if(currentUser != null)
//                {
//                    Intent intent = new Intent(WelcomeActivity.this, WelcomeActivity.class);
//                    startActivity(intent);
//                }
//            }
//        };


        HelperWelcomeButton = (Button) findViewById(R.id.helper_welcome_btn);
        WalkerWelcomeButton = (Button) findViewById(R.id.walker_welcome_btn);

        HelperWelcomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent HelperIntent = new Intent(WelcomeActivity.this, HelperLoginRegisterActivity.class);
                startActivity(HelperIntent);
            }
        });

        WalkerWelcomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent WalkerIntent = new Intent(WelcomeActivity.this, WalkerLoginRegisterActivity.class);
                startActivity(WalkerIntent);
            }
        });
    }


//    @Override
//    protected void onStart()
//    {
//        super.onStart();
//
//        mAuth.addAuthStateListener(firebaseAuthListner);
//    }
//
//
//    @Override
//    protected void onStop()
//    {
//        super.onStop();
//
//        mAuth.removeAuthStateListener(firebaseAuthListner);
//    }
}
