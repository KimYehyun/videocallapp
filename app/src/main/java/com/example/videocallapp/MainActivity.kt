package com.example.videocallapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)  // 권한 배열에 담아두기
    private val requestcode = 1 //권한 요청값 초기화

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isPermissionGranted()) {  //권한이 요청되지 않았으면
            askPermissions() //조건이 true 반환할 경우 askPermisstions에 권한요청
        }

        Firebase.initialize(this) // 파이어베이스 연동 (생성?)

        loginBtn.setOnClickListener {
            //버튼클릭
            val username = usernameEdit.text.toString() //입력한 username저장
            val intent = Intent(this, CallActivity::class.java) //액티비티끼리 서로 호출
            intent.putExtra("username", username) // 변수를 callactivity에서도 사용
            startActivity(intent)


        }

    }

    private fun askPermissions() {
        ActivityCompat.requestPermissions(this, permissions, requestcode)  //(activity,String[],int)
        //카메라, 외부저장소 등에 접근하기 위한 퍼미션 요청을 사용자가 허용해야만 앱에서 해당 하드웨어를 사용
    }

    private fun isPermissionGranted(): Boolean {
        //부울로 반환 (함수작성후에 :boolean)
        permissions.forEach {
            //permissions에 담긴 리스트 내용을 forEach로 하나씩 처리함 -> 권한을 부여함
            if (ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                return false  // 요청이 거부되었다면 false반환
            }

            return true // 연결이 되었으면 tre반환
        }

    }
