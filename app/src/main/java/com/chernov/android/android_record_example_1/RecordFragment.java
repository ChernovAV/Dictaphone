package com.chernov.android.android_record_example_1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by Android on 05.09.2015.
 */
public class RecordFragment extends Fragment {

    // Частота звука в Гц. (11025 - рекомендовано использовать для записи голоса)(44100 - для других)
    private final int RECORDER_SAMPLERATE = 11025;
    // Записываем в формате моно, если нужно стерео - AudioFormat.CHANNEL_IN_STEREO
    private final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    // Задаем битность кодирования
    private final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // ссылка к корневой папке "mic"
    static final String memory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mic/";
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    // значение режима записи
    private boolean isRecording = false;
    private boolean onPause = false;
    private boolean isPlay = false;
    private int count_pieces = 1;
    private String originalFileName;
    private Animation animation;
    private Chronometer myChronometer;
    private ImageView babinaFirst;
    private ImageView babinaSecond;
    private long timeWhenPause = 0;
    private View v;
    private AudioTrack at = null;
    private playAudioFileViaAudioTrack playAudio;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // при смене ориентации экрана этот фрагмент сохраняет свое состояние. onDestroy не вызывается
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // проверяем существование папки mic/, если не существует, создаем
        checkFolders();
        // регистрируем наблюдателя, чтобы получать intent (возвращаем путь к файлу)
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter("custom-event-name"));
        // Проверяем, создано ли представление фрагмента
        v = inflater.inflate(R.layout.fragment_main, container, false);
        // ссылки на кнопки, устанавливаем слушатели
        setButtonHandlers();
        // отключаем не нужные кнопки
        offButtons(true, false, false, false, true);

        return v;
    }

    private void setButtonHandlers() {
        // ссылки на кнопки, устанавливаем слушатели
        ((Button) v.findViewById(R.id.btnRecord)).setOnClickListener(btnClick);
        ((Button) v.findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        ((Button) v.findViewById(R.id.btnPlay)).setOnClickListener(btnClick);
        ((Button) v.findViewById(R.id.btnPause)).setOnClickListener(btnClick);
        ((Button) v.findViewById(R.id.btnMenu)).setOnClickListener(btnClick);

        animation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
        myChronometer = (Chronometer) v.findViewById(R.id.chronometer);
        babinaFirst = (ImageView) v.findViewById(R.id.babina_left);
        babinaSecond = (ImageView) v.findViewById(R.id.babina_right);
    }

    // отключаем не нужные кнопки
    private void offButtons(boolean relayStart, boolean relayStop,
                            boolean relayPlay, boolean relayPause,
                            boolean relayMenu) {
        ((Button) v.findViewById(R.id.btnRecord)).setEnabled(relayStart);
        ((Button) v.findViewById(R.id.btnStop)).setEnabled(relayStop);
        ((Button) v.findViewById(R.id.btnPlay)).setEnabled(relayPlay);
        ((Button) v.findViewById(R.id.btnPause)).setEnabled(relayPause);
        ((Button) v.findViewById(R.id.btnMenu)).setEnabled(relayMenu);
    }

    // Размер внутреннего буфера. Из него можно считывать аудиопоток. Размер порции
    // считывания не должен превышать эту величину. У этого параметра есть минимально допустимое
    // значение, которое можно получить через getMinBufferSize(). Например (int BufferElements2Rec =
    // 50* AudioTrack.getMinBufferSize(frequency, AudioFormat.CHANNEL_OUT_MONO, audioEncoding);
    private int BufferElements2Rec = 1024;
    // 2 байта в 16bit формате
    private int BytesPerElement = 2;

    private void startRecording() {
        // Создаем объект AudioRecorder для записи звука
        // 1. Подключаем источник звука - микрофон;
        // 2. Частота звука в Гц. (11025 - используется для записи голоса) (44100 - для других)
        // 3. Записываем монозвук, стерео AudioFormat.CHANNEL_IN_STEREO
        // 4. Задаем битрость кодирования
        // 5. Размер буфера в байтах в который будет производиться запись
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        // Начинаем запись
        recorder.startRecording();
        // запускаем анимацию
        startAnimation();
        // проверяем, была ли пауза
        if(timeWhenPause!=0) {
            myChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenPause);
        } else {
            myChronometer.setBase(SystemClock.elapsedRealtime());
            timeWhenPause = 0;
        }
        // запускаем таймер
        myChronometer.start();
        // значение режима записи true (запись началась)
        isRecording = true;
        // в потоке записываем в файл звуковой поток
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

        Toast.makeText(getActivity(),getString(R.string.start), Toast.LENGTH_SHORT).show();
    }

    private void writeAudioDataToFile() {
        // Буфер для звука в short
        short sData[] = new short[BufferElements2Rec];
        // создаем объект для работы с потоком записи информации в файл
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(memory + count_pieces + ".tmp");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // пока значение режима записи true, записуем в файл байты
        while (isRecording) {
            // получает голосовой выход с микрофона в формат байт
            recorder.read(sData, 0, BufferElements2Rec);
            try {
                // записываем информацию для файла в буфер после конвертации из short в byte
                byte bData [] = short2byte(sData);
                // запись в файл
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {e.printStackTrace();}
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // останавливаем запись
    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            // освобождаем ресурсы
            recorder.release();
            recorder = null;
            recordingThread = null;
            // останавливаем время
            stopChronometer();
            stopAnimation();
        }
    }

    // слушатель для button
    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnRecord: {
                    // подготовка к записи, запись
                    recordOn();
                    break;
                }
                case R.id.btnStop: {
                    // останавливаем запись/воспроизведения
                    stopOn();
                    break;
                }
                case R.id.btnPlay: {
                    // подготавливаем файл для воспроизведения, play
                    playOn();
                    break;
                }
                case R.id.btnPause: {
                    // подготовка к паузе
                    pauseOn();
                    break;
                }
                case R.id.btnMenu: {
                    // если идет запись или на паузе, нажимаем стоп
                    if(isRecording && onPause) ((Button) v.findViewById(R.id.btnStop)).performClick();
                    // запускаем DialogFragment, в котором список всех записей
                    new DialogList().show(getFragmentManager(), "DialogList");
                    break;
                }
            }
        }
    };

    // подготовка к записи
    private void recordOn() {
        offButtons(false, true, false, true, false);
        // если были паузы, значит склеиваем кусочки в один файл
        checkCountFiles();
        if(!onPause) {
            // если есть файл 1.tmp переименовуем
            renameFile();
        } else {
            onPause = false;
            count_pieces++;
        }
        // начало записи в файл
        startRecording();
    }

    // подготовка к паузе
    private void pauseOn() {
        offButtons(true, true, true, false, true);
        timeWhenPause = myChronometer.getBase() - SystemClock.elapsedRealtime();
        // если идет запись, останавливаем, освобождаем ресурсы
        stopRecording();
        // если были паузы, значит склеиваем кусочки в один файл
        checkCountFiles();
        onPause = true;
    }

    // подготавливаем файл для воспроизведения
    private void playOn() {
        // сброс хронометра
        myChronometer.setBase(SystemClock.elapsedRealtime());
        onPause = false;
        // если были паузы, значит склеиваем кусочки в один файл
        checkCountFiles();
        // если есть файл 1.tmp переименовуем
        renameFile();
        // воспроизведение уже переименованного файла
        playAudio = new playAudioFileViaAudioTrack();
        playAudio.execute(originalFileName);
    }

    // останавливаем запись/воспроизведения
    private void stopOn() {
        // если находимся не в режиме воспроизведения
        if(!isPlay) {
            // сброс хронометра
            myChronometer.setBase(SystemClock.elapsedRealtime());
            onPause = false;
            offButtons(true, false, true, false, true);
            // если идет запись, останавливаем, освобождаем ресурсы
            stopRecording();
            // если были паузы, значит склеиваем кусочки в один файл
            checkCountFiles();
            // если есть файл 1.tmp переименовуем originalFileName
            renameFile();
        } else {
            if(at!=null) {
                // останавливаем asyncktask
                playAudio.cancel(true);
                at.stop();
                // освобождаем ресурсы
                at.release();
                at = null;
                stopAnimation();
                stopChronometer();
                offButtons(true, false, true, false, true);
                isPlay = false;
            }
        }
    }

    // если были паузы, значит склеиваем кусочки в один файл
    private void checkCountFiles() {
        if(count_pieces > 1) {
            try {
                // если есть кусочки файлов, склеиваем
                glueFiles();
            } catch (IOException e) {e.printStackTrace();}
            count_pieces = 1;
        }
    }

    private void glueFiles() throws IOException {
        // каждый файл склеиваем
        for(int i = 2; i <= count_pieces; i++) {
            FileInputStream is1 = new FileInputStream(memory + 1 +".tmp");
            FileInputStream is2 = new FileInputStream(memory + i +".tmp");
            // размер файла в байтах
            int size1 = is1.available();
            int size2 = is2.available();

            byte [] jointSize = new byte[size1+size2];

            is1.read(jointSize, 0, size1);
            is2.read(jointSize, size1, size2);

            is1.close();
            is2.close();

            FileOutputStream os2 = new FileOutputStream(memory + 1 +".tmp");
            os2.write(jointSize);
            os2.close();

            new File(memory + i +".tmp").delete();
        }
    }

    private void resetStartChronometer() {
        myChronometer.setBase(SystemClock.elapsedRealtime());
        timeWhenPause = 0;
        myChronometer.start();
    }

    private void stopChronometer() {
        // останавливаем время
        myChronometer.stop();
    }

    class playAudioFileViaAudioTrack extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // действия в основном потоке перед воспроизведением
            offButtons(false, true, false, false, false);
            resetStartChronometer();
            startAnimation();
            isPlay = true;
        }

        @Override
        protected Void doInBackground(String... params) {
            // считываем файл
            byte[] byteData = null;
            File file = new File(memory + params[0]);
            byteData = new byte[(int) file.length()];
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                in.read(byteData);
                in.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Set and push to audio track..
            int intSize = android.media.AudioTrack.getMinBufferSize(
                    RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
                    RECORDER_AUDIO_ENCODING);

            // часть установленных параметров должна совпадать с параметрами записи
            at = new AudioTrack(
                    AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE,
                    AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING, intSize,
                    AudioTrack.MODE_STREAM);
            if (at != null) {
                at.play();
                // записать массив байтов в трэк
                at.write(byteData, 0, byteData.length);
                // после полного считывания массива байт, делаем stop
                if(!isCancelled()) {
                    at.stop();
                    // освобождаем ресурсы
                    at.release();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            isPlay = false;
            stopChronometer();
            stopAnimation();
            // действия в основном потоке после воспроизведения
            offButtons(true, false, true, false, true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    // переименовываем файл
    private void renameFile() {
        File file = new File(memory + 1 +".tmp");
        // если файл существует, переименовуем
        if (file.exists()) {
            file.renameTo(new File(memory + getNameNewFiles()));
        }
    }

    // получаем имя в виде даты и время
    private String getNameNewFiles() {
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DATE);
        int hour = c.get(Calendar.HOUR);
        int year = c.get(Calendar.YEAR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        originalFileName = "DATE_" + day + "." + month + "." + year + ";_TIME_" + hour + "." + minutes + "." + seconds + ".pcm";
        return  originalFileName;
    }

    // Конвертируем short в byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void startAnimation() {
        try {
            babinaFirst.startAnimation(animation);
            babinaSecond.startAnimation(animation);
        } catch(NullPointerException e) {}
    }

    private void stopAnimation() {
        // останавливаем анимацию
        babinaFirst.clearAnimation();
        babinaSecond.clearAnimation();
    }

    // проверка/создание папки mic
    public void checkFolders() {
        new File(memory).mkdirs();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // получаем данные, которые содержит Intent
            String message = intent.getStringExtra("message");
            // получили название файла
            originalFileName = message;
            playOn();
            Log.d("receiver", "Got message: " + message);
        }
    };

    @Override
    public void onDestroy() {
        if(isRecording && onPause) ((Button) v.findViewById(R.id.btnStop)).performClick();
        // обнуление регистрации наблюдателя
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

    }
}
