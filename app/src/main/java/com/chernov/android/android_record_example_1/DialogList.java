package com.chernov.android.android_record_example_1;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DialogList extends DialogFragment {

    private AdapterList boxAdapter;
    // список записей для адаптера
    private ArrayList<ItemList> list_item = new ArrayList<ItemList>();
    private ListView lvMain;
    private View v;

    @SuppressLint({ "NewApi", "CommitTransaction", "InflateParams" })
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if(v == null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            v = inflater.inflate(R.layout.dialog_list, null);

            lvMain = (ListView) v.findViewById(R.id.lvMain);

            // возвращает список записей для адаптера
            list_item = searchPCM();
            // создаем адаптер
            boxAdapter = new AdapterList(v.getContext(), list_item);
            // присоединяем адаптер к listview
            lvMain.setAdapter(boxAdapter);
            // слушатель нажатий на элементы
            lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                    // отправляем имя выбранной записи в RecordFragment на воспроизведения
                    sendMessage(boxAdapter.getText(position));
                }
            });
        }
        return v;
    }

    // поиск всех записей
    @SuppressWarnings("unchecked")
    ArrayList<ItemList> searchPCM() {
        // возвращает все файлы в папке mic
        File[] list = new File(RecordFragment.memory).listFiles();
        ArrayList filterFiles = new ArrayList();
        // фильтруем по расширению .pcm
        for (int i = 0; i < list.length; i++) {
            if(list[i].getName().endsWith(".pcm"))
                filterFiles.add(new ItemList(list[i].getName()
                        .toString(), R.mipmap.btn_song));
        }
        return filterFiles;
    }

    private void sendMessage(String audio) {
        // отправляем имя выбранной записи в RecordFragment на воспроизведения
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                new Intent("custom-event-name")
                        .putExtra("message", audio));
        // уничтожаем DialogList
        dismiss();
    }
}