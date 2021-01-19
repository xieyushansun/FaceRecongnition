package com.example.facerecongnition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {

    @OnClick(R.id.btn_login) void login(){
        String url = Constant.URL + "login?number=" + ed_phone.getText().toString() + "&password=" + ed_password.getText().toString();
        OkGo.<String>post(url)
//                .params("number ", ed_phone.getText().toString())
//                .params("password ", ed_password.getText().toString())
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        String res = response.body();
                        JSONObject jo = JSONObject.parseObject(res);
                        int code = jo.getInteger("code");
                        if (code == 0){
                            Toasty.success(getApplicationContext(), "登录成功！", Toast.LENGTH_SHORT, true).show();
                            startActivity(new Intent(MainActivity.this, RecognizeActivity.class));
                        }else {
                            Toasty.error(getApplicationContext(), "用户名或密码错误！", Toast.LENGTH_SHORT, true).show();
                        }
                    }
                });
    }
    @BindView(R.id.ed_username)
    EditText ed_phone;
    @BindView(R.id.ed_password)
    EditText ed_password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//remove title bar  即隐藏标题栏
        Objects.requireNonNull(getSupportActionBar()).hide();// 隐藏ActionBar
        setContentView(R.layout.activity_main);
        // 绑定视图
        ButterKnife.bind(this);
    }
}