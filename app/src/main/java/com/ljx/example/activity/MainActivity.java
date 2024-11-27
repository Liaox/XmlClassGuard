package com.ljx.example.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.ljx.example.R;
import com.ljx.example.model.User;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        User user = new User();
        user.setName("12");
        user.setSex("12");
        user.setAvatar("12");
        user.setInfo(new User.Info());
        user.setAge("12");
        System.out.println(user);
        if (TextUtils.equals(user.getSex(),"1")){
            System.out.println(user.getName());
        }
        String b = "{\"press\":\"126\",\"fence\":\"125\",\"reception\":{},\"privacy\":\"123\",\"pair\":\"124\"}";
        User c = new Gson().fromJson(b, User.class);
        System.out.println(c.getName());
        System.out.println(new Gson().toJson(c));
        String data = new Gson().toJson(user);
        System.out.println(data);
        TextView tv = findViewById(R.id.tv1);
        tv.setText(c.getName());
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(),new Gson().toJson(c),Toast.LENGTH_SHORT).show();
            }
        });
    }
}
