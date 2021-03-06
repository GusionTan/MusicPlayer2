package com.example.test12;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MusicActivity extends Activity {


    final MediaPlayer mp = new MediaPlayer();
    String song_path = "";
    private SeekBar seekBar;
    private TextView currentTV;
    private TextView totalTV;
    boolean isStop = true;
    private boolean isSeekBarChanging;//互斥变量，防止进度条与定时器冲突。
    private int currentposition;//当前音乐播放的进度
    private Timer timer;
    private ArrayList<String> list;
    private File[] songFiles;
    private Button choosebtn;
    private int Bofang = 1;
    private Button bofang;
    private String ta;


    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            // 展示给进度条和当前时间
            int progress = mp.getCurrentPosition();
            seekBar.setProgress(progress);
            currentTV.setText(formatTime(progress));
            // 继续定时发送数据
            updateProgress();
            return true;
        }
    });

    //3、使用formatTime方法对时间格式化：
    private String formatTime(int length) {
        Date date = new Date(length);
        //时间格式化工具
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        String totalTime = sdf.format(date);
        return totalTime;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        totalTV = findViewById(R.id.music_total_time);
        currentTV = findViewById(R.id.music_current_time);
        seekBar = (SeekBar) findViewById(R.id.music_seekbar);
        seekBar.setOnSeekBarChangeListener(new MySeekBar());
        final TextView gequ = (TextView) findViewById(R.id.gequ);
        //图片旋转设置
        ImageView imageView = (ImageView) findViewById(R.id.draw_1);
        //动画
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.img_animation);
        LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
        animation.setInterpolator(lin);
        imageView.startAnimation(animation);

        //播放顺序设置
        bofang = (Button) findViewById(R.id.bofang);
        bofang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Bofang == 1){
                    bofang.setText("循环播放");
                    Bofang =2;
                }else if(Bofang ==2){
                    Bofang =3;
                    bofang.setText("随机播放");
                }else{
                    Bofang =1;
                    bofang.setText("顺序播放");
                }
            }
        });

        //选择本地歌曲按钮设置
        choosebtn = (Button) findViewById(R.id.nav_button);
        choosebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mp.stop();
                Intent intent = new Intent(MusicActivity.this,ChooseAreaFragment.class);
                startActivity(intent);
            }
        });

        if (ActivityCompat.checkSelfPermission(MusicActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MusicActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
            return;
        }

        //判断是否是AndroidN以及更高的版本 N=24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        //扫描SD卡
        list = new ArrayList<String>();   //音乐列表
        File sdpath = Environment.getExternalStorageDirectory(); //获得手机SD卡路径
        File path = new File(sdpath+ "//Music//");      //获得SD卡的mp3文件夹
        //返回以.mp3结尾的文件 (自定义文件过滤)
        songFiles = path.listFiles(new Tanr(".mp3"));
        for (File file : songFiles) {
            String str = file.getAbsolutePath();
            String str1 = str.substring(str.indexOf("谭")+1,str.indexOf(".mp3"));
            list.add(str1);//获取文件的绝对路径
        }

        //接受选择歌曲与传值
        Intent intent = getIntent();
        song_path = intent.getStringExtra("spath");
        String tr1 = intent.getStringExtra("cposition");
        currentposition = Integer.parseInt(tr1);
        changeMusic(currentposition);
        gequ.setText(song_path.substring(song_path.indexOf("谭")+1,song_path.indexOf(".mp3")));
        try {
            mp.reset();    //重置
            mp.setDataSource(song_path);
            mp.prepare();     //准备
            mp.start(); //播放
            seekBar.setMax(mp.getDuration());
            isStop = false;
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!isSeekBarChanging) {
                        seekBar.setProgress(mp.getCurrentPosition());

                    }
                }
            }, 0, 50);
        }catch (Exception e){

        }

        //暂停和播放
        final ImageButton btnpause = (ImageButton) findViewById(R.id.btn_pause);
        btnpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (song_path.isEmpty())
                    Toast.makeText(getApplicationContext(), "先选收歌曲先听听", Toast.LENGTH_SHORT).show();
                if (mp.isPlaying()) {
                    mp.pause();  //暂停
                    isStop = true;
                    btnpause.setImageResource(android.R.drawable.ic_media_play);
                } else if (!song_path.isEmpty()) {
                    mp.start();   //继续播放
                    isStop = false;
                    btnpause.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });


        //上一曲和下一曲
        final ImageButton previous = (ImageButton) findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMusic(--currentposition);
                gequ.setText(ta);
            }
        });
        final ImageButton next = (ImageButton) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMusic(++currentposition);
                gequ.setText(ta);
            }
        });

    }

    //切换歌曲
    private void changeMusic(int position) {
        if (position < 0) {
            currentposition = position = list.size() - 1;
        } else if (position > list.size() - 1) {
            currentposition = position = 0;
        }
        song_path = songFiles[position].getAbsolutePath();

        try {
            // 切歌之前先重置，释放掉之前的资源
            mp.reset();
            // 设置播放源
            mp.setDataSource(song_path);
            // 开始播放前的准备工作，加载多媒体资源，获取相关信息
            mp.prepare();

            // 开始播放
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        seekBar.setProgress(0);//将进度条初始化
        seekBar.setMax(mp.getDuration());//设置进度条最大值为歌曲总时间
        totalTV.setText(formatTime(mp.getDuration()));//显示歌曲总时长
        ta =song_path.substring(song_path.indexOf("谭")+1,song_path.indexOf(".mp3"));
        updateProgress();//更新进度条
    }

    private static final int INTERNAL_TIME = 500;
    private void updateProgress() {
        // 使用Handler每间隔1s发送一次空消息，通知进度条更新
        Message msg = Message.obtain();// 获取一个现成的消息
        // 使用MediaPlayer获取当前播放时间除以总时间的进度
        int progress = mp.getCurrentPosition();
        msg.arg1 = progress;
        mHandler.sendMessageDelayed(msg, INTERNAL_TIME);
        if(progress >= mp.getDuration() ){
            if(Bofang ==1) {
                changeMusic(++currentposition);
            }else if(Bofang ==2){
                changeMusic(currentposition);
            }else{
                changeMusic(--currentposition);
            }
        }
    }

    public class MySeekBar implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }

        /*滚动时,应当暂停后台定时器*/
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;
        }

        /*滑动结束后，重新设置值*/
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;
            mp.seekTo(seekBar.getProgress());
        }

    }

    protected void onDestroy() {
        super.onDestroy();
        if (mp != null) {
            mp.stop();
            mp.release();
        }
        Toast.makeText(getApplicationContext(), "退出啦", Toast.LENGTH_SHORT).show();
    }
}
