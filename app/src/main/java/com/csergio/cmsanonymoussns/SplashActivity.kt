package com.csergio.cmsanonymoussns

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
// 스플래시 화면 출력을 위한 액티비티
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 스플래시 이미지 출력 후 메인 액티비티로 전환
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
