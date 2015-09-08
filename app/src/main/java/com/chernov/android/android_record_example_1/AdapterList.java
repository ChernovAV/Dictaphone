package com.chernov.android.android_record_example_1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class AdapterList extends BaseAdapter {
    Context ctx;
    LayoutInflater lInflater;
    ArrayList<ItemList> objects;

    AdapterList(Context context, ArrayList<ItemList> products) {
        ctx = context;
        objects = products;
        lInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public Object getItem(int position) {
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // пользуем созданные, но не используемые view
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.item_list, parent, false);
        }
        ItemList p = getProduct(position);

        // заполняем View в пункте списка данными из товаров: наименование, цена и картинка
        ((TextView) view.findViewById(R.id.tvDescr)).setText(p.name);
        ((ImageView) view.findViewById(R.id.ivImage)).setImageResource(p.image);

        return view;
    }

    // элемент по позиции
    ItemList getProduct(int position) {
        return ((ItemList) getItem(position));
    }

    // Получаем текущий text элемента
    public String getText(int position) {
        ItemList p = getProduct(position);
        String text = p.name;
        return text;
    }
}