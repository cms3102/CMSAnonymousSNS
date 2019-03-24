package com.csergio.cmsanonymoussns

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.card_comment.view.*

class DetailActivity : AppCompatActivity() {

    // 댓글 목록 저장 변수
    private val commentList = mutableListOf<Comment>()

    // 부모글 아이디 저장 변수
    private var postingId = ""

    // 배경 이미지 주소 저장 변수
    private var backgroundUri = ""

    // 플로팅액션버튼 토글값 저장 변수
    private var fabState = false

    // 글 작성자 아이디 저장 변수
    private var writerId = ""

    // 사용자 아이디 저장 변수
    private var userId = ""

    // 플로팅버튼 애니메이션 저장 변수
    private lateinit var fabOpen:Animation
    private lateinit var fabClose:Animation

    // 파이어베이스 인증 객체 저장 변수
    private lateinit var firbaseAuth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val layoutManager = LinearLayoutManager(this@DetailActivity)
        commentRecyclerView.layoutManager = layoutManager
        commentRecyclerView.adapter = Adapter()

        // 부모글 아이디 저장
        postingId = intent.getStringExtra("postingId")

        // 파이어 베이스 인증 객체 저장
        firbaseAuth = FirebaseAuth.getInstance()

        // 사용자 아이디 저장
//        userId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        userId = firbaseAuth.currentUser?.uid.toString()

        // 플로팅액션버튼 애니메이션 저장 변수
        fabOpen = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_close)

        // 상세보기에 출력할 글 데이터 가져오기
        FirebaseDatabase.getInstance().getReference("/Postings/$postingId").addValueEventListener(object :ValueEventListener{

            override fun onCancelled(databaseError: DatabaseError) {
                databaseError?.toException().printStackTrace()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot?.let {
                    val posting = it.getValue(Posting::class.java)
                    posting?.let {
                        Picasso.get()
                            .load(Uri.parse(it.background))
                            .fit()
                            .centerCrop()
                            .into(detailBackground)
                        detailContent.text = it.content
                        // 배경 이미지 주소 저장
                        backgroundUri = it.background
                        // 작성자 아이디 저장
                        writerId = it.writerId
                    }
                }
            }

        })

        // 댓글 목록 가져오기 및 이벤트 리스너 설정
        FirebaseDatabase.getInstance().getReference("/Comments/$postingId").addChildEventListener(object :ChildEventListener{

            override fun onCancelled(databaseError: DatabaseError) {
                databaseError?.toException().printStackTrace()
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot?.let {

                    val comment = dataSnapshot.getValue(Comment::class.java)

                    comment?.let {

                        val index = commentList.map { it.commentId }.indexOf(it.commentId)
                        commentList.removeAt(index)

                        if (previousChildName == null){
                            // 기존 데이터의 최상위에 추가
                            commentList.add(comment)
                            commentRecyclerView.adapter?.notifyItemChanged(commentList.size - 1)
                        } else {
                            // PreviousChildName값을 활용해 변경된 위치에 데이터 추가
                            val previousIndex = commentList.map { it.commentId }.indexOf(previousChildName)
                            commentList.add(previousIndex + 1, comment)
                            commentRecyclerView.adapter?.notifyItemChanged(previousIndex + 1)
                        }

                    }
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot?.let {
                    val comment = dataSnapshot.getValue(Comment::class.java)
                    comment?.let {
                        val previousIndex = commentList.map { it.commentId }.indexOf(previousChildName)
                        commentList[previousIndex + 1] = comment
                        commentRecyclerView.adapter?.notifyItemChanged(previousIndex + 1)
                    }
                }
            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot?.let {
                    val comment = dataSnapshot.getValue(Comment::class.java)
                    comment?.let {
                        commentList.add(comment)
                        commentRecyclerView.adapter?.notifyItemInserted(commentList.size - 1)
                    }
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                dataSnapshot?.let {
                    val comment = dataSnapshot.getValue(Comment::class.java)
                    comment?.let {
                        val index = commentList.map { it.commentId }.indexOf(it.commentId)
                        commentList.removeAt(index)
                        commentRecyclerView.adapter?.notifyItemRemoved(index)
                    }
                }
            }

        })

        // 플로팅액션버튼 클릭 시 댓글 쓰기 화면 전환
        detailToggleFAB.setOnClickListener {
            toggleFAB(fabState)
        }

        // 댓글 쓰기 버튼 클릭 이벤트 설정
        writeCommentButton.setOnClickListener {
            val intent = Intent(this@DetailActivity, WriteActivity::class.java)
            // 글쓰기 종류 구분을 위한 정보 추가
            intent.putExtra("type", "comment")
            // 댓글이 소속될 글의 아이디 전달
            intent.putExtra("postingId", postingId)
            startActivity(intent)
            toggleFAB(true)
        }

        // 글 수정 버튼 클릭 이벤트 설정
        modifyDetailButton.setOnClickListener {
            val intent = Intent(this@DetailActivity, WriteActivity::class.java)
            // 글쓰기 종류 구분을 위한 정보 추가
            intent.putExtra("type", "modify")
            // 글의 아이디 전달
            intent.putExtra("postingId", postingId)
            // 글 내용 전달
            intent.putExtra("postingContent", detailContent.text.toString())
            // 배경 이미지 주소 전달
            intent.putExtra("postingBackground", backgroundUri)
            startActivity(intent)
            toggleFAB(true)
        }

        // 글 삭제 버튼 이벤트 설정
        deleteDetailButton.setOnClickListener {
            // 파이어베이스 DB에서 글과 댓글 삭제
            val firebaseDB = FirebaseDatabase.getInstance()
                firebaseDB.getReference("/Postings/$postingId").removeValue()
                firebaseDB.getReference("/Comments/$postingId").removeValue()
            // 메세지 띄우고 리스트로 이동
            Toast.makeText(this@DetailActivity, "글이 삭제되었습니다.", Toast.LENGTH_LONG).show()
            val intent = Intent(this@DetailActivity, MainActivity::class.java)
            startActivity(intent)
        }

    }

    // 아이디 일치 확인 메소드
    private fun checkId():Boolean{
        if(writerId != "" && userId != "" && writerId == userId) {
            return true
        }
        return false
    }

    // 플로팅액션버튼 토글 메소드
    private fun toggleFAB(State:Boolean){

        // 로그인 확인
        if (firbaseAuth.currentUser != null){
            if (State){
                writeCommentButton.startAnimation(fabClose)
                // 작성자 아이디와 사용자 아이디가 일치할 경우에만 수정 버튼 표시
                if (checkId()){
                    modifyDetailButton.startAnimation(fabClose)
                    deleteDetailButton.startAnimation(fabClose)
                }
                fabState = false
            } else {
                writeCommentButton.startAnimation(fabOpen)
                if (checkId()){
                    modifyDetailButton.startAnimation(fabOpen)
                    deleteDetailButton.startAnimation(fabOpen)
                }
                fabState = true
            }
        }

    }

    // 뷰홀더
    inner class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        val backgroundView = itemView.commentBackground
        val contentView = itemView.commentContent
    }

    // 어댑터
    inner class Adapter:RecyclerView.Adapter<ViewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@DetailActivity).inflate(R.layout.card_comment, parent, false))
        }

        override fun getItemCount(): Int {
            var size = commentList.size
            // 이 글의 댓글 수 DB 정보 업데이트
            if (size > 0){
                Toast.makeText(this@DetailActivity, "댓글 정보 있음", Toast.LENGTH_LONG).show()
                FirebaseDatabase.getInstance().getReference("/Postings/$postingId").child("commentCount").setValue(commentList.size)
                Log.d("태그", "commentCount ${commentList.size}로 업데이트 됨")
            } else {
                Toast.makeText(this@DetailActivity, "댓글 정보 없음", Toast.LENGTH_LONG).show()
                Log.d("태그", "현재 commentCount ${commentList.size}")
            }
            return size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val comment = commentList[position]

            comment?.let {

                Picasso.get()
                    .load(Uri.parse(comment.background))
                    .fit()
                    .centerCrop()
                    .into(holder.backgroundView)

                holder.contentView.text = comment.content

            }
        }

    }

}
