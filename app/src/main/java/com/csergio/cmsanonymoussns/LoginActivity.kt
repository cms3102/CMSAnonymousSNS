package com.csergio.cmsanonymoussns

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.toolbar.*

class LoginActivity : AppCompatActivity() {

    private lateinit var firabaseAuth:FirebaseAuth

    private lateinit var id:String
    private lateinit var pw:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 파이어베이스 인증 객체 저장
        firabaseAuth = FirebaseAuth.getInstance()

        // 로그인 버튼 클릭 시 이벤트 처리
        goLoginButton.setOnClickListener {
            id = loginIdEditText.text.toString()
            pw = loginPwEditText.text.toString()

            firabaseAuth.signInWithEmailAndPassword(id, pw).addOnCompleteListener(this, object:OnCompleteListener<AuthResult>{
                override fun onComplete(task: Task<AuthResult>) {
                    if (task.isSuccessful){
                        Log.d("로그인", "로그인 성공")
                        Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)

                    } else {
                        Log.w("로그인", task.exception)
                        Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_LONG).show()
                    }
                }
            })
        }

        // 회원 가입 버튼 클릭 시 이벤트 처리
        goSignUpButton.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firabaseAuth.currentUser
        if (currentUser != null){
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}
