package com.chernov.android.android_record_example_1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Window;

public class RecordActivity extends FragmentActivity {

    RecordFragment mContentFragment = null;
    private final String TAG = "myLog";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (savedInstanceState != null) {
            mContentFragment = (RecordFragment) getSupportFragmentManager().getFragment(
                    savedInstanceState, "mContent");
            ft.replace(R.id.fragmentContainerRecord, mContentFragment);
            ft.commit();
            Log.d(TAG, "savedInstanceState != null");
        }
        if (mContentFragment == null) {
            mContentFragment = new RecordFragment();
            ft.add(R.id.fragmentContainerRecord, mContentFragment);
            ft.commit();
            Log.d(TAG, "savedInstanceState == null");
        }
    }

    // Вызывается для того, чтобы сохранить пользовательский интерфейс
    // перед выходом из "активного" состояния.
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Сохраните состояние UI в переменную savedInstanceState.
        // Она будет передана в метод onCreate при закрытии и
        // повторном запуске процесса.
        super.onSaveInstanceState(savedInstanceState);
        getSupportFragmentManager().putFragment(savedInstanceState, "mContent", mContentFragment);
    }

    // Вызывается, когда метод onCreate завершил свою работу,
    // и используется для восстановления состояния пользовательского
    // интерфейса
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Восстановите состояние UI из переменной savedInstanceState.
        // Этот объект типа Bundle также был передан в метод onCreate.
    }
}