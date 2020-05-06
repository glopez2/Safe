package com.groupfeb.safe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class WalkerLoginRegisterActivity extends AppCompatActivity {

    private TextView CreateWalkerAccount;
    private TextView TitleWalker;
    private Button LoginWalkerButton;
    private Button RegisterWalkerButton;
    private EditText WalkerEmail;
    private EditText WalkerPassword;

    private DatabaseReference walkersDatabaseRef;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListner;

    private ProgressDialog loadingBar;

    private FirebaseUser currentUser;
    String currentUserId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walker_login_register);

        mAuth = FirebaseAuth.getInstance();


        firebaseAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if (currentUser != null) {
                    Intent intent = new Intent(WalkerLoginRegisterActivity.this, WalkersMapActivity.class);
                    startActivity(intent);
                }
            }
        };


        CreateWalkerAccount = (TextView) findViewById(R.id.walker_register_link);
        TitleWalker = (TextView) findViewById(R.id.walker_status);
        LoginWalkerButton = (Button) findViewById(R.id.walker_login_btn);
        RegisterWalkerButton = (Button) findViewById(R.id.walker_register_btn);
        WalkerEmail = (EditText) findViewById(R.id.walker_email);
        WalkerPassword = (EditText) findViewById(R.id.walker_password);
        loadingBar = new ProgressDialog(this);


        RegisterWalkerButton.setVisibility(View.INVISIBLE);
        RegisterWalkerButton.setEnabled(false);

        CreateWalkerAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CreateWalkerAccount.setVisibility(View.INVISIBLE);
                LoginWalkerButton.setVisibility(View.INVISIBLE);
                TitleWalker.setText("Helper Registration");

                RegisterWalkerButton.setVisibility(View.VISIBLE);
                RegisterWalkerButton.setEnabled(true);
            }
        });


        RegisterWalkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = WalkerEmail.getText().toString();
                String password = WalkerPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(WalkerLoginRegisterActivity.this, "Please Enter Email...", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(WalkerLoginRegisterActivity.this, "Please Enter Password...", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Please wait :");
                    loadingBar.setMessage("While system is performing processing on your data...");
                    loadingBar.show();

                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                currentUserId = mAuth.getCurrentUser().getUid();
                                walkersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Walkers").child(currentUserId);
                                walkersDatabaseRef.setValue(true);

                                Intent intent = new Intent(WalkerLoginRegisterActivity.this, WalkersMapActivity.class);
                                startActivity(intent);

                                loadingBar.dismiss();
                            } else {
                                Toast.makeText(WalkerLoginRegisterActivity.this, "Please Try Again. Error Occurred, while registering... ", Toast.LENGTH_SHORT).show();

                                loadingBar.dismiss();
                            }
                        }
                    });
                }
            }
        });

        LoginWalkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = WalkerEmail.getText().toString();
                String password = WalkerPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(WalkerLoginRegisterActivity.this, "Please Enter Email...", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(WalkerLoginRegisterActivity.this, "Please Enter Password...", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Please wait :");
                    loadingBar.setMessage("While system is performing processing on your data...");
                    loadingBar.show();

                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(WalkerLoginRegisterActivity.this, "Sign In , Successful...", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(WalkerLoginRegisterActivity.this, WalkersMapActivity.class);
                                startActivity(intent);

                                loadingBar.dismiss();
                            } else {
                                Toast.makeText(WalkerLoginRegisterActivity.this, "Error Occurred, while Signing In... ", Toast.LENGTH_SHORT).show();

                                loadingBar.dismiss();
                            }
                        }
                    });
                }
            }
        });
    }
}