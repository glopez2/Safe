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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class WalkerLoginRegisterActivity extends AppCompatActivity {

    private Button walkerLoginButton;
    private Button walkerRegisterButton;
    private TextView walkerRegisterLink;
    private TextView walkerStatus;
    private EditText walkerEmail;
    private EditText walkerPassword;
    private ProgressDialog loadingBar;
    private FirebaseAuth mAuth;
    private DatabaseReference walkerDatabaseRef;
    private String onlineWalkerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walker_login_register);

        mAuth = FirebaseAuth.getInstance();

        walkerLoginButton = (Button) findViewById(R.id.walker_login_btn);
        walkerRegisterButton = (Button) findViewById(R.id.walker_register_btn);
        walkerRegisterLink = (TextView) findViewById(R.id.register_walker_link);
        walkerStatus = (TextView) findViewById(R.id.walker_status);
        walkerEmail = (EditText) findViewById(R.id.email_walker);
        walkerPassword = (EditText) findViewById(R.id.password_walker);
        loadingBar = new ProgressDialog(this);

        walkerRegisterButton.setVisibility(View.INVISIBLE);
        walkerRegisterButton.setEnabled(false);

        walkerRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                walkerLoginButton.setVisibility(View.INVISIBLE);
                walkerRegisterLink.setVisibility(View.INVISIBLE);
                walkerStatus.setText("Register Walker");
                walkerRegisterButton.setVisibility(View.VISIBLE);
                walkerRegisterButton.setEnabled(true);
            }
        });

        walkerRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = walkerEmail.getText().toString();
                String password = walkerPassword.getText().toString();

                RegisterCustomer(email, password);
            }
        });

        walkerLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = walkerEmail.getText().toString();
                String password = walkerPassword.getText().toString();

                signInWalker(email, password);
            }
        });
    }

    private void signInWalker(String email, String password) {
        if(TextUtils.isEmpty(email)) {
            Toast.makeText(WalkerLoginRegisterActivity.this, "Please enter Email...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(password)) {
            Toast.makeText(WalkerLoginRegisterActivity.this, "Please enter Password...", Toast.LENGTH_SHORT).show();
        }
        else {
            loadingBar.setTitle("Walker Login");
            loadingBar.setMessage("Please wait, while credentials are checked");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        Intent walkerIntent = new Intent(WalkerLoginRegisterActivity.this, WalkersMapActivity.class);
                        startActivity(walkerIntent);

                        Toast.makeText(WalkerLoginRegisterActivity.this, "Walker logged In Successful", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                    } else {
                        Toast.makeText(WalkerLoginRegisterActivity.this, "Login Unsuccessful, Please Try Again", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    private void RegisterCustomer(String email, String password) {
        if(TextUtils.isEmpty(email)) {
            Toast.makeText(WalkerLoginRegisterActivity.this, "Please enter Email...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(password)) {
            Toast.makeText(WalkerLoginRegisterActivity.this, "Please enter Password...", Toast.LENGTH_SHORT).show();
        }
        else {
            loadingBar.setTitle("Walker Registration");
            loadingBar.setMessage("Please wait, while data gets registered");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        onlineWalkerId = mAuth.getCurrentUser().getUid();
                        walkerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Walkers").child(onlineWalkerId);

                        walkerDatabaseRef.setValue(true);
                        Intent helperIntent = new Intent(WalkerLoginRegisterActivity.this, HelpersMapActivity.class);
                        startActivity(helperIntent);

                        Toast.makeText(WalkerLoginRegisterActivity.this, "Walker Register Successful", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    } else {
                        Toast.makeText(WalkerLoginRegisterActivity.this, "Registration Unsuccessful, Please Try Again", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }
}
