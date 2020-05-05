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

public class HelperLoginRegisterActivity extends AppCompatActivity {

    private Button helperLoginButton;
    private Button helperRegisterButton;
    private TextView helperRegisterLink;
    private TextView helperStatus;
    private EditText helperEmail;
    private EditText helperPassword;
    private ProgressDialog loadingBar;
    private FirebaseAuth mAuth;
    private DatabaseReference helperDatabaseRef;
    private String onlineHelperId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_login_register);

        mAuth = FirebaseAuth.getInstance();

        helperLoginButton = (Button) findViewById(R.id.helper_login_btn);
        helperRegisterButton = (Button) findViewById(R.id.helper_register_btn);
        helperRegisterLink = (TextView) findViewById(R.id.register_helper_link);
        helperStatus = (TextView) findViewById(R.id.helper_status);
        helperEmail = (EditText) findViewById(R.id.email_helper);
        helperPassword = (EditText) findViewById(R.id.password_helper);
        loadingBar = new ProgressDialog(this);

        helperRegisterButton.setVisibility(View.INVISIBLE);
        helperRegisterButton.setEnabled(false);

        helperRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helperLoginButton.setVisibility(View.INVISIBLE);
                helperRegisterLink.setVisibility(View.INVISIBLE);
                helperStatus.setText("Register Helper");
                helperRegisterButton.setVisibility(View.VISIBLE);
                helperRegisterButton.setEnabled(true);
            }
        });

        helperRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = helperEmail.getText().toString();
                String password = helperPassword.getText().toString();

                RegisterHelper(email, password);
            }
        });

        helperLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = helperEmail.getText().toString();
                String password = helperPassword.getText().toString();

                signInHelper(email, password);
            }
        });
    }

    private void signInHelper(String email, String password) {
        if(TextUtils.isEmpty(email)) {
            Toast.makeText(HelperLoginRegisterActivity.this, "Please enter Email...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(password)) {
            Toast.makeText(HelperLoginRegisterActivity.this, "Please enter Password...", Toast.LENGTH_SHORT).show();
        }
        else {
            loadingBar.setTitle("Helper Login");
            loadingBar.setMessage("Please wait, while credentials are checked");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        Intent helperIntent = new Intent(HelperLoginRegisterActivity.this, HelpersMapActivity.class);
                        startActivity(helperIntent);

                        Toast.makeText(HelperLoginRegisterActivity.this, "Helper logged In Successfully", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    } else {
                        Toast.makeText(HelperLoginRegisterActivity.this, "Login Unsuccessful, Please Try Again", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    private void RegisterHelper(String email, String password) {
        if(TextUtils.isEmpty(email)) {
            Toast.makeText(HelperLoginRegisterActivity.this, "Please enter Email...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(password)) {
            Toast.makeText(HelperLoginRegisterActivity.this, "Please enter Password...", Toast.LENGTH_SHORT).show();
        }
        else {
            loadingBar.setTitle("Helper Registration");
            loadingBar.setMessage("Please wait, while data gets registered");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        onlineHelperId = mAuth.getCurrentUser().getUid();
                        helperDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Helpers").child(onlineHelperId);
                        helperDatabaseRef.setValue(true);

                        Intent helperIntent = new Intent(HelperLoginRegisterActivity.this, HelpersMapActivity.class);
                        startActivity(helperIntent);

                        Toast.makeText(HelperLoginRegisterActivity.this, "Helper Register Successful", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                    } else {
                        Toast.makeText(HelperLoginRegisterActivity.this, "Registration Unsuccessful, Please Try Again", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }
}
