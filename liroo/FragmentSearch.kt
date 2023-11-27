package com.example.liroo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment

class FragmentSearch : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragsearch, container, false)

        val searchButton: Button = view.findViewById(R.id.searchButton)
        val searchEditText: EditText = view.findViewById(R.id.searchEditText)

        searchButton.setOnClickListener {
            val keyword = searchEditText.text.toString()
            searchPosts(keyword)
        }

        return view
    }

    private fun searchPosts(keyword: String) {
        // TODO: 서버에 검색 요청을 보내는 코드를 작성해야 합니다.
        // Retrofit을 사용하는 경우, `PostApi1` 인터페이스에 새로운 메서드를 추가하고
        // 그 메서드를 호출하여 검색 요청을 보낼 수 있습니다.
    }
}
