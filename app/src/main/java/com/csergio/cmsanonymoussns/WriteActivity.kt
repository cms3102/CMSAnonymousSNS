package com.csergio.cmsanonymoussns

import android.graphics.Color
import android.hardware.usb.UsbDevice.getDeviceId
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_write.*
import kotlinx.android.synthetic.main.card_background.view.*

class WriteActivity : AppCompatActivity() {

    // 카드 배경 데이터 모음
    private val backgroundList = mutableListOf(
        "android.resource://com.csergio.cmsanonymoussns/drawable/bg1",
        "android.resource://com.csergio.cmsanonymoussns/drawable/bg2",
        "android.resource://com.csergio.cmsanonymoussns/drawable/bg3",
        "android.resource://com.csergio.cmsanonymoussns/drawable/bg4",
        "android.resource://com.csergio.cmsanonymoussns/drawable/bg5",
        "android.resource://com.csergio.cmsanonymoussns/drawable/bg6"
    )

    // 일반글/댓글 쓰기 구분값 저장 변수
    private var type = "write"

    // 댓글이 속할 글의 아이디
    private var postingId = ""

    // 현재 선택된 배경 이미지 인덱스 저장용 변수
    private var currentBackgroundIndex = 0

    // 사용자 아이디 저장 변수
    lateinit var userId:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        // 사용자 아이디 저장
        userId = FirebaseAuth.getInstance().currentUser?.uid.toString()

        // 글쓰기 구분 확인
        intent.getStringExtra("type")?.let {
            type = intent.getStringExtra("type")
            postingId = intent.getStringExtra("postingId")
        }

        // 액션 바 제목 변경
        supportActionBar?.title = when(type){
            "comment" -> "댓글 작성"
            "modify" -> "글 수정"
            else -> "글 작성"
        }

        // RecyclerView에 적용할 레이아웃 매니저 생성 및 배치 방향 설정
        val layoutManager = LinearLayoutManager(this@WriteActivity)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        // RecyclerView에 레이아웃 매니저 할당
        backgroundRecycler.layoutManager = layoutManager

        // RecyclerView에 어댑터 할당
        backgroundRecycler.adapter = Adapter()

        // type이 글 수정일 때 글 내용 및 배경 이미지 뿌려주기
        if (type == "modify"){

            Picasso.get()
                .load(Uri.parse(intent.getStringExtra("postingBackground")))
                .fit()
                .centerCrop()
                .into(writeBackground)
            writeContent.setText(intent.getStringExtra("postingContent"))
        }

        // 글 올리기 버튼 클릭 시 이벤트 리스너 등록
        writeButton.setOnClickListener {

            // 글 내용이 없을 경우 메시지 출력
            if (TextUtils.isEmpty(writeContent.text)){
                Toast.makeText(applicationContext, "내용을 입력해 주세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            when(type){

                "write" -> {
                    Log.d("로그", "일반 글쓰기 모드")
                    // 작성한 내용을 담을 Posting 클래스 생성
                    val posting = Posting()

                    // 파이어베이스 데이터베이스에 데이터를 저장하기 위한 정보 생성
                    val firebaseRef = FirebaseDatabase.getInstance().getReference("Postings").push()

                    // Posting 객체에 작성된 데이터 할당
                    posting.background = backgroundList[currentBackgroundIndex]
                    posting.content = writeContent.text.toString()
                    // 파이어베이스에서 생성해준 키를 글의 아이디로 할당
                    posting.postingId = firebaseRef.key.toString()
                    // 휴대폰 디바이스 아이디를 작성자 아이디로 할당
                    posting.writerId = userId
                    // 글 작성 시간은 파이어베이스 서버 시간으로 할당
                    posting.time = ServerValue.TIMESTAMP

                    // 파이어베이스에 데이터 업로드
                    firebaseRef.setValue(posting)
                }

                "comment" -> {
                    Log.d("로그", "댓글 쓰기 모드")
                    val comment = Comment()

                    val firebaseRef = FirebaseDatabase.getInstance().getReference("Comments/$postingId").push()

                    comment.commentId = firebaseRef.key.toString()
                    comment.background = backgroundList[currentBackgroundIndex]
                    comment.content = writeContent.text.toString()
                    comment.parentId = postingId
                    comment.time = ServerValue.TIMESTAMP
                    comment.writerId = userId

                    firebaseRef.setValue(comment)
                }

                "modify" -> {
                    Log.d("로그", "수정 모드")
                    val posting = Posting()

                    val firebaseRef = FirebaseDatabase.getInstance().getReference("Postings/$postingId")

                    posting.background = intent.getStringExtra("postingBackground")
                    posting.content = writeContent.text.toString()
                    posting.postingId = firebaseRef.key.toString()
                    posting.writerId = userId
                    posting.time = ServerValue.TIMESTAMP

                    firebaseRef.setValue(posting)
                }
            }

            Toast.makeText(applicationContext, "작성하신 이야기가 업로드되었습니다.", Toast.LENGTH_LONG).show()
            // 저장 작업 후 액티비티 종료
            finish()

        }

    }

    // 뷰를 담을 뷰홀더 클래스
    inner class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        val backgroundView = itemView.backgroundImageView
    }

    // RecyclerView에 적용할 어댑터 클래스
    inner class Adapter:RecyclerView.Adapter<ViewHolder>(){

        // RecyclerView로 그려질 ViewHolder 생성
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@WriteActivity).inflate(R.layout.card_background, parent, false))
        }

        // RecyclerView로 몇개의 항목을 출력할지 지정
        override fun getItemCount(): Int {
            return backgroundList.size
        }

        // 생성된 뷰홀더에 데이터 할당
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            // 효율적 외부 이미지 로딩을 위해 Picasso 라이브러리 사용
            Picasso.get()
                .load(Uri.parse(backgroundList[position]))
                .fit()
                .centerCrop()
                .into(holder.backgroundView)

            // 목록에 있는 배경 이미지를 선택했을 때의 이벤트 리스너 등록
            holder.itemView.setOnClickListener {

                // 선택된 배경 이미지 인덱스 저장
                currentBackgroundIndex = position

                Picasso.get()
                    .load(Uri.parse(backgroundList[position]))
                    .fit()
                    .centerCrop()
                    .into(writeBackground)
            }
        }
    }
}
