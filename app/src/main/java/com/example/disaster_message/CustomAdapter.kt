package com.example.disaster_message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.disaster_message.databinding.ItemDisasterMessageBinding
import com.google.gson.annotations.SerializedName

data class Profile(
    @SerializedName("Title") val title: String,
    @SerializedName("Body") val body: String,
    @SerializedName("Date") val date: String)

class CustomAdapter(val profileList : ArrayList<Profile>) : RecyclerView.Adapter<CustomAdapter.Holder>() {

    // ViewHolder가 생성되는 함수
    // 여기서 viewHolder 객체를 만든다.
    // 여기서 반환한 뷰 홀더 객체는 자동으로 onBindBiewHolder() 함수의 매개변수로 전달
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDisasterMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    // 매개변수로 전달된 뷰 홀더 객체의 뷰에 데이터를 출력하거나 필요한 이벤트를 등록한다.
    // 매개변수로 있는 position은 아이템 중 지금 아이템이 몇 번쨰 아이템인지 알려줌
    override fun onBindViewHolder(holder: Holder, position: Int) {
        // position - 몇번째 아이템인가
        holder.title.text = profileList[position].title
        holder.body.text = profileList[position].body
        holder.date.text = profileList[position].date
    }

    // RecyclerView에 몇가지의 아이템이 떠야되는지 알려주는 메서드
    // 이 함수가 반환한 숫자만큼 onBindViewHolder() 함수가 호출됨
    override fun getItemCount(): Int {
        return profileList.size
    }

    inner class Holder(val binding: ItemDisasterMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.rvTitle
        val body = binding.rvBody
        val date = binding.rvDate
    }
}