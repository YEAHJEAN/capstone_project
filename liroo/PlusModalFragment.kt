package com.example.liroo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlusModalFragment : BottomSheetDialogFragment() {

    interface OptionClickListener {
        fun onOptionSelected(option: String)
    }

    private var optionClickListener: OptionClickListener? = null

    fun setOptionClickListener(listener: OptionClickListener) {
        optionClickListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plus_modal_list_dialog, container, false)

        val btnRecognize = view.findViewById<Button>(R.id.btn_recognize)
        val btnDirectInput = view.findViewById<Button>(R.id.btn_direct_input)

        btnRecognize.setOnClickListener {
            optionClickListener?.onOptionSelected("책장 인식")

            // 열고자 하는 FragmentPlus 클래스의 인스턴스를 생성합니다.
            val fragmentPlus = FragmentPlus()

            // fragmentPlus 열기 위한 코드
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragmentPlus)
            transaction.addToBackStack(null) // 이전 Fragment로 돌아갈 수 있도록 설정
            transaction.commit()

            dismiss()
        }

        btnDirectInput.setOnClickListener {
            optionClickListener?.onOptionSelected("직접 입력")

            val fragmentInput = FragmentInput()

            // fragmentPlus 열기 위한 코드
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragmentInput)
            transaction.addToBackStack(null) // 이전 Fragment로 돌아갈 수 있도록 설정
            transaction.commit()

            dismiss()
        }

        return view
    }
}
