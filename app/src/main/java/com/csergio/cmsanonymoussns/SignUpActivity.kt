package com.csergio.cmsanonymoussns

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View.Z
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity() {

    private lateinit var id:String
    private lateinit var  pw1:String
    private lateinit var  pw2:String
    private lateinit var firebaseAuth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // 파이어베이스 인증 객체 생성
        firebaseAuth = FirebaseAuth.getInstance()

        // 가입하기 버튼 클릭 시 이벤트 처리
        signUpButton.setOnClickListener {

            // 입력값 가져와서 저장
            id = signUpIdEditText.text.toString()
            pw1 = signUpPwEditText1.text.toString()
            pw2 = signUpPwEditText2.text.toString()

            if (!validateEmail(id)){
                Toast.makeText(this@SignUpActivity, "올바른 이메일 주소를 입력해 주세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 비밀번호 길이 검사
            if (pw1.length < 6 || pw2.length < 6){
                Toast.makeText(this@SignUpActivity, "비밀번호는 6자 이상 입력해 주세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 비밀번호 일치 검사
            if (pw1 != pw2){
                Toast.makeText(this@SignUpActivity, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 계정 생성
            firebaseAuth.createUserWithEmailAndPassword(id, pw1).addOnCompleteListener(this, object : OnCompleteListener<AuthResult>{

                override fun onComplete(task: Task<AuthResult>) {
                    if (task.isSuccessful){
                        Log.d("회원 가입", "신규 유저 생성 성공")
                        Toast.makeText(this@SignUpActivity, "회원 가입이 완료되었습니다.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                        intent.putExtra("user", firebaseAuth.currentUser)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.w("회원 가입", task.exception)
                        Toast.makeText(this@SignUpActivity, "회원 가입 실패", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            })

        }
    }

    private fun validateEmail(email:String):Boolean{
        val regexResult = Regex("(\\w+\\.)*\\w+@(\\w+\\.)+[A-Za-z]+").matches(email)
        return regexResult
    }
}
