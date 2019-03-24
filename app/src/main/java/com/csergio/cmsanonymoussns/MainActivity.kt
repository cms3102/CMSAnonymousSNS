package com.csergio.cmsanonymoussns

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.card_posting.*
import kotlinx.android.synthetic.main.card_posting.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Minutes
import java.text.SimpleDateFormat
import java.time.DateTimeException
import java.util.*

class MainActivity : AppCompatActivity() {

    // DB에서 가져온 글 목록을 저장할 변수 선언
    private val postings:MutableList<Posting> = mutableListOf()

    // 파이어베이스 인증 객체 저장 변수
    private lateinit var firebaseAuth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 툴바 적용
        setSupportActionBar(toolbar)

        // 액션 바 제목 변경
        supportActionBar?.title = "이야기 목록"

        // 레이아웃 매니저 생성
        val layoutManager = LinearLayoutManager(this@MainActivity)
        // 출력할 아이템을 역순으로 정렬
        layoutManager.reverseLayout = true
        // 아이템 출력을 끝에서부터 하도록 설정
        layoutManager.stackFromEnd = true

        // RecyclerView에 레이아웃 매니저 할당
        recyclerView.layoutManager = layoutManager
        // RecyclerView에 어댑터 할당
        recyclerView.adapter = Adapter()

        // 파이어베이스에서 글 데이터 불러오기
        FirebaseDatabase.getInstance().getReference("/Postings").orderByChild("time").addChildEventListener(object:ChildEventListener{

            // 작업 취소 시 오류 보여주기
            override fun onCancelled(databaseError: DatabaseError) {
                databaseError.toException().printStackTrace()
            }

            // 글의 순서가 변경된 경우 처리
            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot?.let {

                    // 변경된 글 데이터 변수에 저장
                    val posting = dataSnapshot.getValue(Posting::class.java)

                    posting?.let {

                        // 저장된 기존 데이터 삭제
                        val index = postings.map { it.postingId }.indexOf(it.postingId)
                        postings.removeAt(index)
                        recyclerView.adapter?.notifyItemRemoved(index)

                        if (previousChildName == null){
                            // 기존 데이터의 최상위에 추가
                            postings.add(posting)
                            recyclerView.adapter?.notifyItemChanged(postings.size - 1)
                        } else {
                            // PreviousChildName값을 활용해 변경된 위치에 데이터 추가
                            val previousIndex = postings.map { it.postingId }.indexOf(previousChildName)
                            postings.add(previousIndex + 1, posting)
                            recyclerView.adapter?.notifyItemChanged(previousIndex + 1)
                        }

                    }
                }
            }

            // 글 정보가 변경된 경우
            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {

                dataSnapshot?.let {
                    val posting = dataSnapshot.getValue(Posting::class.java)
                    posting?.let {
                        val previousIndex = postings.map { it.postingId }.indexOf(previousChildName)
                        postings[previousIndex + 1] = posting
                        recyclerView.adapter?.notifyItemChanged(previousIndex + 1)
                    }
                }

            }

            // 글이 추가된 경우
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot?.let {
                    val posting = dataSnapshot.getValue(Posting::class.java)
                    posting?.let {
                        if (previousChildName == null){
                            postings.add(it)
                            recyclerView.adapter?.notifyItemInserted(postings.size - 1)
                        } else {
                            val previousIndex = postings.map { it.postingId }.indexOf(previousChildName)
                            postings.add(previousIndex + 1, it)
                            recyclerView.adapter?.notifyItemInserted(previousIndex + 1)
                        }
                    }
                }
            }

            // 글이 삭제된 경우
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                dataSnapshot?.let {
                    val posting = dataSnapshot.getValue(Posting::class.java)
                    posting?.let {
                        val index = postings.map { it.postingId }.indexOf(it.postingId)
                        postings.removeAt(index)
                        recyclerView.adapter?.notifyItemRemoved(index)
                    }
                }
            }

        })

        // 파이어베이스 인증 객체 저장
        firebaseAuth = FirebaseAuth.getInstance()

        // 플로팅액션버튼 클릭 시 이벤트 리스너 등록
        floatingActionButton.setOnClickListener {
            // Intent를 통해 WriteActivity 실행
            val intent = Intent(this@MainActivity, WriteActivity::class.java)
            startActivity(intent)
        }

    }

    // 툴바 메뉴 생성
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    // 툴바 메뉴 선택 이벤트 처리
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            if (item.itemId == R.id.logoutMenu) {
                firebaseAuth.signOut()
                Toast.makeText(this@MainActivity, "로그아웃 되었습니다.", Toast.LENGTH_LONG).show()
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // 뷰홀더 클래스 생성 및 변수에 뷰 할당
    inner class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        val backgroundView:ImageView = itemView.cardBackground
        val content:TextView = itemView.listContent
        val time:TextView = itemView.timeText
        val commentCount:TextView = itemView.commentCountText
    }

    // 어댑터 클래스
    inner class Adapter:RecyclerView.Adapter<ViewHolder>(){

        // 뷰홀더 생성
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.card_posting, parent, false))
        }

        // 출력할 아이템 수 반환
        override fun getItemCount(): Int {
            return postings.size
        }

        // 뷰홀더에 데이터 할당
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            // 뷰홀더에 할당할 글 데이터 변수에 저장
            val posting = postings[position]
            // 배경 이미지 설정
            Picasso.get().load(Uri.parse(posting.background)).fit().centerCrop().into(holder.backgroundView)
            // 댓글 수 설정
            holder.commentCount.text = posting.commentCount.toString()
            holder.content.text = posting.content
            holder.time.text = getTime(posting.time as Long)

            // 카드 클릭 시 상세 화면으로 전환
            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                intent.putExtra("postingId", posting.postingId)
                startActivity(intent)
            }
        }
    }

    // joda-time 라이브러리를 활용한 글 작성 날짜 계산
    fun getTime(target:Long):String {

        val currentTime = DateTime()
        val postedTime = DateTime().withMillis(target)

        val days = Days.daysBetween(currentTime, postedTime).days
        val hours = Hours.hoursBetween(postedTime, currentTime).hours
        val minutes = Minutes.minutesBetween(postedTime, currentTime).minutes

        // 경과 시간에 따라 글 작성 시간을 다르게 표기
        if (days == 0){
            when{
                hours == 0 && minutes == 0 -> return "방금 전"
                hours > 0 -> return "${hours}시간 전"
            }
            return "${minutes}분 전"
        } else {
            val date = SimpleDateFormat("yyyy년 MM월 dd일").format(Date(target)) ?: ""
            return date
        }
    }


}
