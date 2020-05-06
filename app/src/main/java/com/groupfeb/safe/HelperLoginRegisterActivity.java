package com.groupfeb.safe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

public class HelperLoginRegisterActivity extends AppCompatActivity {

    private TextView CreateHelperAccount;
    private TextView TitleHelper;
    private Button LoginHelperButton;
    private Button RegisterHelperButton;
    private EditText HelperEmail;
    private EditText HelperPassword;

    private DatabaseReference helpersDatabaseRef;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListner;

    private ProgressDialog loadingBar;

    private FirebaseUser currentUser;
    String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_login_register);

        mAuth = FirebaseAuth.getInstance();

//        firebaseAuthListner = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
//            {
//                currentUser = FirebaseAuth.getInstance().getCurrentUser();
//
//                if(currentUser != null)
//                {
//                    Intent intent = new Intent(HelperLoginRegisterActivity.this, HelpersMapActivity.class);
//                    startActivity(intent);
//                }
//            }
//        };


        CreateHelperAccount = (TextView) findViewById(R.id.create_helper_account);
        TitleHelper = (TextView) findViewById(R.id.title_helper);
        LoginHelperButton = (Button) findViewById(R.id.login_helper_btn);
        RegisterHelperButton = (Button) findViewById(R.id.register_helper_btn);
        HelperEmail = (EditText) findViewById(R.id.helper_email);
        HelperPassword = (EditText) findViewById(R.id.helper_password);
        loadingBar = new ProgressDialog(this);


        RegisterHelperButton.setVisibility(View.INVISIBLE);
        RegisterHelperButton.setEnabled(false);

        CreateHelperAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CreateHelperAccount.setVisibility(View.INVISIBLE);
                LoginHelperButton.setVisibility(View.INVISIBLE);
                TitleHelper.setText("Helper Registration");

                RegisterHelperButton.setVisibility(View.VISIBLE);
                RegisterHelperButton.setEnabled(true);
            }
        });


        RegisterHelperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = HelperEmail.getText().toString();
                String password = HelperPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(HelperLoginRegisterActivity.this, "Please Enter Email...", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(HelperLoginRegisterActivity.this, "Please Enter Password...", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Please wait :");
                    loadingBar.setMessage("While system is performing processing on your data...");
                    loadingBar.show();

                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                currentUserId = mAuth.getCurrentUser().getUid();
                                helpersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Helpers").child(currentUserId);
                                helpersDatabaseRef.setValue(true);

                                Intent intent = new Intent(HelperLoginRegisterActivity.this, HelpersMapActivity.class);
                                startActivity(intent);

                                loadingBar.dismiss();
                            } else {
                                Toast.makeText(HelperLoginRegisterActivity.this, "Please Try Again. Error Occurred, while registering... ", Toast.LENGTH_SHORT).show();

                                loadingBar.dismiss();
                            }
                        }
                    });
                }
            }
        });


        LoginHelperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = HelperEmail.getText().toString();
                String password = HelperPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(HelperLoginRegisterActivity.this, "Please Enter Email...", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(HelperLoginRegisterActivity.this, "Please Enter Password...", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Please wait :");
                    loadingBar.setMessage("While system is performing processing on your data...");
                    loadingBar.show();

                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(HelperLoginRegisterActivity.this, "Sign In , Successful...", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(HelperLoginRegisterActivity.this, HelpersMapActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(HelperLoginRegisterActivity.this, "Error Occurred, while Signing In... ", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
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