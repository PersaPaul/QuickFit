package com.persasrl.paul.quickfit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity implements View.OnClickListener{

    UserLocalStore userLocalStore;
    TextView etName;
    Button bLogout,bStart,bSport;
    int type=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = (TextView) findViewById(R.id.etName);
        bLogout = (Button) findViewById(R.id.bLogout);
        bStart= (Button) findViewById(R.id.start);
        bSport= (Button) findViewById(R.id.sport);

        bStart.setOnClickListener(this);
        bLogout.setOnClickListener(this);
        bSport.setOnClickListener(this);

        userLocalStore = new UserLocalStore(this);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.bLogout: {
                userLocalStore.clearUserData();
                userLocalStore.setUserLoggedIn(false);
                Intent loginIntent = new Intent(this, LoginScreen.class);

                startActivity(loginIntent);
                break; }
            case R.id.start: {
                Intent startIntent = new Intent(this, StepCounter.class);
                Bundle bundle = new Bundle();
                bundle.putInt("sport",type);
                startIntent.putExtras(bundle);
                startActivity(startIntent);
                break; }
            case R.id.sport: {
                type=(type+1)%2;
                if(type==0)
                    bSport.setText("RUNNING MODE");
                else bSport.setText("CYCLING MODE");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authenticate() == true) {
            displayUserDetails();
        }
    }

    private boolean authenticate() {
        if (userLocalStore.getLoggedInUser() == null) {
            Intent intent = new Intent(this, LoginScreen.class);
            startActivity(intent);
            return false;
        }
        return true;
    }


    private void displayUserDetails() {
        User user = userLocalStore.getLoggedInUser();
        etName.setText(user.username);
    }

}
