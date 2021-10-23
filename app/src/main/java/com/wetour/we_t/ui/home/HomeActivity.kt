package com.wetour.we_t.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.wetour.we_t.PreferenceUtil
import com.wetour.we_t.R
import com.wetour.we_t.data.MultiRecyclerData
import com.wetour.we_t.data.TourRoomData
import com.wetour.we_t.network.RequestToServer
import com.wetour.we_t.network.data.MainResponse
import com.wetour.we_t.ui.home.tourContents.MultiRecyclerContentsAdapter
import com.wetour.we_t.ui.home.tourContents.MultiRecyclerTitleAdapter
import com.wetour.we_t.ui.home.tourContents.model.MultiRecyclerTitle
import com.wetour.we_t.ui.home.tourRoom.TourRoomAdapter
import com.wetour.we_t.ui.makeTour.MakeTourActivity
import com.wetour.we_t.ui.myPage.FamilyListActivity
import com.wetour.we_t.ui.myPage.MyPageActivity
import kotlinx.android.synthetic.main.activity_home.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var preferenceUtil: PreferenceUtil

    lateinit var tourRoomAdapter: TourRoomAdapter
    lateinit var multiRecyclerTitleAdapter: MultiRecyclerTitleAdapter
    var mode = "child"
    var tourRoomDatas = mutableListOf<TourRoomData>()

    var titleData = mutableListOf<MultiRecyclerTitle>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        preferenceUtil = PreferenceUtil(this)

        setRv()
//        getDummyData()

        getDataFromServer()

    }

    private fun setRv() {
        tourRoomAdapter = TourRoomAdapter(this)
        act_home_tour_room_recyclerview.adapter = tourRoomAdapter
        act_home_tour_room_recyclerview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)


        multiRecyclerTitleAdapter = MultiRecyclerTitleAdapter(this)
        act_home_contents_recyclerview.adapter = multiRecyclerTitleAdapter
        act_home_contents_recyclerview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    }

    private fun getDataFromServer() {

        val jsonData = JSONObject()
        jsonData.put("email", preferenceUtil.getString("email", "noEmail"))
        val body = JsonParser.parseString(jsonData.toString()) as JsonObject

        RequestToServer.service.requestMain(body).enqueue(object : Callback<MainResponse> {
            override fun onResponse(call: Call<MainResponse>, response: Response<MainResponse>) {

                if (response.isSuccessful) {
                    Log.e("HomeActivity", "onResponse ${response.body()}")

                    val res = response.body()

                    if (res != null) {
                        // init Static ------------------------------------------------------------
                        act_home_text_user.text = "${res.name}님,"

                        if (res.type == "1") {
                            //자녀
                            if (res.num_fam.toInt() < 1) {
                                // 부모 등록 안됨
                                act_home_text.text = "가족을 초대하고 여행을 개설해보세요."
                                act_home_btn_make_tour.isVisible = false
                                act_home_btn_invite_family.isVisible = true

                            } else {
                                // 등록된 부모 존재
                                act_home_text.text = "행복한 가족여행을 준비해주세요."
                                act_home_btn_make_tour.isVisible = true
                                act_home_btn_invite_family.isVisible = false
                            }

                        } else {
                            // 부모
                            act_home_text.text = "행복한 여행을 도와드리겠습니다."
                            act_home_btn_make_tour.visibility = View.GONE
                            act_homt_text_open_tour.text = "자녀가 개설한 여행방"
                        }
                        // init Dynamic ------------------------------------------------------------

                        val wet_reco = arrayListOf<MultiRecyclerData>()
                        val tv = arrayListOf<MultiRecyclerData>()
                        val festival = arrayListOf<MultiRecyclerData>()
                        val parent_good = arrayListOf<MultiRecyclerData>()

                        // 위트가 추천하는 관광지 데이터
                        res.wet_good.forEach {
                            wet_reco.add(
                                MultiRecyclerData(2, it.title, it.firstimage, it.tag_list)
                            )
                        }

                        // tv속 관광지
                        res.tv_tour.forEach {
                            tv.add(
                                MultiRecyclerData(1, it.addr1, it.firstimage, it.tag_list)
                            )
                        }

                        // 페스티벌 데이터
                        res.festival.forEach {
                            festival.add(
                                MultiRecyclerData(3, it.title, it.firstimage, null)
                            )
                        }

                        // 자녀 - 부모님 좋아요가 있을 경우
                        res.p_good?.forEach {
                            parent_good.add(
                                MultiRecyclerData(0, it.title, it.firstimage, null)
                            )
                        }

                        Log.e("HomeActivity", "1 ${wet_reco}")
                        Log.e("HomeActivity", "2 ${tv}")
                        Log.e("HomeActivity", "3 ${festival}")
                        Log.e("HomeActivity", "4 ${parent_good}")

                        titleData.apply {
                            add(
                                MultiRecyclerTitle(
                                    "위트가 추천하는 관광지",
                                    wet_reco
                                )
                            )
                            add(
                                MultiRecyclerTitle(
                                    "TV 속 그 여행지",
                                    tv
                                )
                            )
                            add(
                                MultiRecyclerTitle(
                                    "축제중인 여행지",
                                    festival
                                )
                            )
                        }

                        multiRecyclerTitleAdapter.datas = titleData
                        multiRecyclerTitleAdapter.notifyDataSetChanged()

                        // 여행방 불러오기--------------------------------------------------------------
                        res.trip_record_list?.forEach {
                            tourRoomAdapter.datas.add(
                                TourRoomData(
                                    profile1 = it.attend_famliy[0].profile,
                                    profile2 = it.attend_famliy[1].profile,
                                    roomName = it.trip_name,
                                    dDay = it.start_day,
                                    date = "${it.start_day}~${it.end_day}"
                                )
                            )
                        }

                        tourRoomAdapter.notifyDataSetChanged()

                    }

                } else {
                    Log.e("HomeActivity", "onResponse ${response.message()}")
                }
            }

            override fun onFailure(call: Call<MainResponse>, t: Throwable) {
                Log.e("HomeActivity", "onFailure ${t.message}")
            }

        })
    }

//    private fun getDummyData() {

//        val list0 = arrayListOf(
//            RecyclerModel(0, "제주", R.drawable.img_jeju_exam, null),
//            RecyclerModel(0, "여수", R.drawable.img_yeosu_exam, null),
//            RecyclerModel(0, "부산 광안리", R.drawable.img_busan_exam, null)
////        )
//
//        val list1 = arrayListOf(
//            MultiRecyclerData(2, "대관령 양떼목장", R.drawable.img_wet_reco1, arrayListOf("풍경", "산")),
//            MultiRecyclerData(2, "제주도 성산일출봉", R.drawable.img_wet_reco2, arrayListOf("풍경", "섬"))
//        )
//
//        val list2 = arrayListOf(
//            MultiRecyclerData(1, "전라남도 담양", R.drawable.img_tv1, arrayListOf("산", "대나무 숲")),
//            MultiRecyclerData(1, "경상남도 거제도", R.drawable.img_tv2, arrayListOf("풍경", "섬")),
//            MultiRecyclerData(1, "보성 녹차밭", R.drawable.img_tv3, arrayListOf("밭", "자연"))
//        )
//
//        val list3 = arrayListOf(
//            MultiRecyclerData(3, "태백 해바라기 축제", R.drawable.img_fes1, null)
//        )
//
//        if (mode == "parent") {
//            titleData.apply {
//                add(
//                    MultiRecyclerTitle(
//                        "위트가 추천하는 관광지",
//                        list1
//                    )
//                )
//                add(
//                    MultiRecyclerTitle(
//                        "TV 속 그 여행지",
//                        list2
//                    )
//                )
//                add(
//                    MultiRecyclerTitle(
//                        "축제중인 여행지",
//                        list3
//                    )
//                )
//            }
//        } else {
//            titleData.apply {
//                add(
//                    MultiRecyclerTitle(
//                        "부모님이 이 여행지를 좋아해요!",
//                        arrayListOf(
//                            MultiRecyclerData(0, "제주", R.drawable.img_jeju_exam, null),
//                            MultiRecyclerData(0, "여수", R.drawable.img_yeosu_exam, null),
//                            MultiRecyclerData(0, "부산 광안리", R.drawable.img_busan_exam, null)
//                        )
//
//                    )
//                )
//                add(
//                    MultiRecyclerTitle(
//                        "위트가 추천하는 관광지",
//                        list1
//                    )
//                )
//                add(
//                    MultiRecyclerTitle(
//                        "TV 속 그 여행지",
//                        list2
//                    )
//                )
//                add(
//                    MultiRecyclerTitle(
//                        "축제중인 여행지",
//                        list3
//                    )
//                )
//            }
//        }
//
//    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.act_home_btn_mypage -> {
                val intent = Intent(this, MyPageActivity::class.java)
                startActivity(intent)
            }

            R.id.act_home_btn_search -> {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
            }

            R.id.act_home_btn_invite_family -> {
                val intent = Intent(this, FamilyListActivity::class.java)
                startActivity(intent)
            }

            R.id.act_home_btn_make_tour -> {
                val intent = Intent(this, MakeTourActivity::class.java)
                startActivity(intent)
            }
        }
    }
}