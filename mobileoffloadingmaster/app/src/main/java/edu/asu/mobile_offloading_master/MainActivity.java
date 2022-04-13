package edu.asu.mobile_offloading_master;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.os.Bundle;


public class MainActivity extends AppCompatActivity {

    Button primaryButton;
    Button secondaryButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        primaryButton = findViewById(R.id.bStartMaster);
        secondaryButton = findViewById(R.id.bStartSlave);
        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Master.class);
                startActivity(intent);
            }
        });
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Secondary.class);
                startActivity(intent);
            }
        });
    }
}