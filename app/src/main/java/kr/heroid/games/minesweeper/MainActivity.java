package kr.heroid.games.minesweeper;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private int fieldWidth = 12;
    private int fieldHeight = 20;
    private int maxMineCount = 40;
    private int userCollectedMineCount = 0;

    private Button[] fieldButtons;
    private TextView[] fieldTexts;
    RelativeLayout fieldLayout;

    private int[] fieldData;
    private int[] userFieldData;

    private CountDownTimer timer;
    private int time = 0;

    private int currentMode = 0;
    // 0 : explore mode,  1 : flag mode

    private boolean isGameFinished = false;
    private boolean isAnswerShowed = false;
    private boolean isFirstHit = true;
    private boolean isPaused = false;

    private boolean isSeekBarShowed = false;

    private int VIEW_SIZE_X;
    private int VIEW_SIZE_Y;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_main);

        initViews();
        placeMines();
        placeFieldData();
        printFieldData();

        placeFirstHitHint();

        ((SwitchCompat) findViewById(R.id.switch_mode)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentMode = isChecked ? 1 : 0;
            }
        });
        ((Button) findViewById(R.id.button_more)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomDialog();
            }
        });
        ((Button) findViewById(R.id.button_more)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, "Code by HanTaehyeok\nOriginal game by microsoft.", Toast.LENGTH_SHORT).show();
//                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED){
//                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.READ_PHONE_STATE}, 215);
//                    //Toast.makeText(MainActivity.this, "Checking DevMode...", Toast.LENGTH_SHORT).show();
//                    return true;
//                }
//                if(Build.VERSION.SDK_INT >= 26 && Build.getSerial().equals("SerialNumberHere")) {
//                    for (int i = 0; i < fieldData.length - 1; i++) {
//                        if (isAnswerShowed && userFieldData[i] == 0) {
//                            fieldButtons[i].setVisibility(View.VISIBLE);
//                        } else if (!isAnswerShowed && userFieldData[i] == 0) {
//                            fieldButtons[i].setVisibility(View.INVISIBLE);
//                        }
//                    }
//                    isAnswerShowed = !isAnswerShowed;
//                }else{
//                    Toast.makeText(MainActivity.this, "DevMode Check Failed.", Toast.LENGTH_LONG).show();
//                }
                return true;
            }
        });
        ((FloatingActionButton) findViewById(R.id.fab_zoom)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu menu = new PopupMenu(MainActivity.this, v);
                menu.inflate(R.menu.main);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId()){
                            case R.id.menu_zoom:
                                if(isSeekBarShowed) {
                                    ((AppCompatSeekBar) findViewById(R.id.zoom_seek_bar)).setVisibility(View.INVISIBLE);
                                    ((TextView) findViewById(R.id.zoom_text)).setVisibility(View.INVISIBLE);
                                }else {
                                    ((AppCompatSeekBar) findViewById(R.id.zoom_seek_bar)).setVisibility(View.VISIBLE);
                                    ((TextView) findViewById(R.id.zoom_text)).setVisibility(View.VISIBLE);
                                    ((TextView) findViewById(R.id.zoom_text)).setText(getResources().getString(R.string.zoom) + " " + (float)((AppCompatSeekBar)findViewById(R.id.zoom_seek_bar)).getProgress()/100);
                                }
                                isSeekBarShowed = !isSeekBarShowed;
                                return true;
                            case R.id.menu_screen_capture:
                                v.setVisibility(View.INVISIBLE);
                                takeScreenshot();
                                v.setVisibility(View.VISIBLE);
                                return true;
                        }
                        return false;
                    }
                });
                menu.show();
            }
        });
        ((AppCompatSeekBar)findViewById(R.id.zoom_seek_bar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float ratio = (float)progress/100;
                fieldLayout.setPivotX(0.0f);
                fieldLayout.setPivotY(0.0f);
                fieldLayout.setScaleX(ratio);
                fieldLayout.setScaleY(ratio);
                ((TextView) findViewById(R.id.zoom_text)).setText(getResources().getString(R.string.zoom) + " " + ratio);
                if(ratio < 1) return;
                ViewGroup.LayoutParams params = fieldLayout.getLayoutParams();
                params.width = (int) (VIEW_SIZE_X * ratio);
                params.height = (int) (VIEW_SIZE_Y * ratio);
                fieldLayout.setLayoutParams(params);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        timer = new CountDownTimer(1000, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                time++;
                ((TextView) findViewById(R.id.text_time)).setText(getResources().getString(R.string.time) + " " + time);
                timer.start();
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        timer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isPaused && !isGameFinished) {
            timer.start();
        }
        isPaused = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        timer = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 427 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            FloatingActionButton b = ((FloatingActionButton) findViewById(R.id.fab_zoom));
            b.setVisibility(View.INVISIBLE);
            takeScreenshot();
            b.setVisibility(View.VISIBLE);
        }else if(requestCode == 427){
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
//        else if(requestCode == 215 && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
//            if(Build.VERSION.SDK_INT >= 26 && Build.getSerial().equals("SerialNumberHere")) {
//                for (int i = 0; i < fieldData.length - 1; i++) {
//                    if (isAnswerShowed && userFieldData[i] == 0) {
//                        fieldButtons[i].setVisibility(View.VISIBLE);
//                    } else if (!isAnswerShowed && userFieldData[i] == 0) {
//                        fieldButtons[i].setVisibility(View.INVISIBLE);
//                    }
//                }
//                isAnswerShowed = !isAnswerShowed;
//
//                //Toast.makeText(MainActivity.this, "Complete!!", Toast.LENGTH_SHORT).show();
//            }
//        }else if(requestCode == 215){
//            Toast.makeText(MainActivity.this, "DevMode Check Failed.", Toast.LENGTH_SHORT).show();
//        }
    }

    boolean isExitDialogShowing = false;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_BACK:
                if(!isExitDialogShowing){
                    new AlertDialog.Builder(this)
                            .setTitle("Exit")
                            .setMessage(R.string.dialog_exit)
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    isExitDialogShowing = false;
                                }
                            })
                            .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    isExitDialogShowing = false;
                                }
                            })
                            .show();
                    isExitDialogShowing = true;
                }
                return true;
        }
        return false;
    }

    private void initViews() {
        fieldButtons = new Button[fieldWidth * fieldHeight];
        fieldTexts = new TextView[fieldWidth * fieldHeight];
        fieldData = new int[fieldWidth * fieldHeight];
        userFieldData = new int[fieldWidth * fieldHeight];
        fieldLayout = (RelativeLayout) findViewById(R.id.field_layout);

        ((TextView) findViewById(R.id.text_mine_left)).setText(getResources().getString(R.string.mine_left) + " " + (maxMineCount - userCollectedMineCount));
        ((TextView) findViewById(R.id.text_time)).setText(getResources().getString(R.string.time) + " " + time);
        ((Button)findViewById(R.id.button_more)).setText(":/");

        for (int heightIndex = 0; heightIndex < fieldHeight; heightIndex++) {
            for (int widthIndex = 0; widthIndex < fieldWidth; widthIndex++) {
                TextView text = new TextView(this);
                text.setId(View.generateViewId());
                text.setContentDescription("text_" + (widthIndex + heightIndex * fieldWidth + 1));
                text.setBackgroundResource(R.drawable.field_text_border);
                text.setGravity(Gravity.CENTER);
                text.setOnClickListener(this);
                RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(100, 100);
                if (widthIndex != 0) {
                    textParams.addRule(RelativeLayout.RIGHT_OF, fieldTexts[heightIndex * fieldWidth + widthIndex - 1].getId());
                    textParams.setMargins(10, 0, 0, 0);
                }
                if (heightIndex != 0) {
                    textParams.addRule(RelativeLayout.BELOW, fieldTexts[widthIndex + (heightIndex - 1) * fieldWidth].getId());
                    textParams.setMargins(textParams.leftMargin, 10, 0, 0);
                }
                fieldLayout.addView(text);
                text.setLayoutParams(textParams);
                fieldTexts[widthIndex + heightIndex * fieldWidth] = text;

                Button button = new Button(this);
                button.setId(View.generateViewId());
                button.setContentDescription("button_" + (widthIndex + heightIndex * fieldWidth + 1));
                button.setBackgroundResource(R.drawable.field_button);
                button.setPadding(-30, -30, -30, -30);
                button.setOnClickListener(this);
                button.setOnLongClickListener(this);
                if (Build.VERSION.SDK_INT >= 23)
                    button.setForeground(getResources().getDrawable(R.drawable.field_button_ripple, getTheme()));
                RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(100, 100);
                if (widthIndex != 0) {
                    buttonParams.addRule(RelativeLayout.RIGHT_OF, fieldButtons[heightIndex * fieldWidth + widthIndex - 1].getId());
                    buttonParams.setMargins(10, 0, 0, 0);
                }
                if (heightIndex != 0) {
                    buttonParams.addRule(RelativeLayout.BELOW, fieldButtons[widthIndex + (heightIndex - 1) * fieldWidth].getId());
                    buttonParams.setMargins(buttonParams.leftMargin, 10, 0, 0);
                }
                fieldLayout.addView(button);
                button.setLayoutParams(buttonParams);
                fieldButtons[widthIndex + heightIndex * fieldWidth] = button;
            }
        }

        fieldLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        VIEW_SIZE_X = fieldLayout.getMeasuredWidth();
        VIEW_SIZE_Y = fieldLayout.getMeasuredHeight();

    }

    private void placeMines() {
        int addedMineCount = 0;
        int i = 0;
        while (addedMineCount < maxMineCount) {
            if (fieldData[i] != -1 && p(5)) {
                fieldData[i] = -1;
                addedMineCount++;
                fieldTexts[i].setText("x");
            }
            i++;
            if (i == fieldData.length - 1) {
                i = 0;
            }
        }
    }

    private void placeFieldData() {
        for (int i = 0; i < fieldData.length; i++) {
            if (fieldData[i] == -1) {
                if (i - 1 >= 0 && (i % fieldWidth) != 0) {
                    if (fieldData[i - 1] != -1) {
                        fieldData[i - 1]++;
                    }
                }
                if (i + 1 < fieldData.length && i % fieldWidth != fieldWidth - 1) {
                    if (fieldData[i + 1] != -1) {
                        fieldData[i + 1]++;
                    }
                }
                if (i - fieldWidth >= 0) {
                    if (fieldData[i - fieldWidth] != -1) {
                        fieldData[i - fieldWidth]++;
                    }
                }
                if (i + fieldWidth < fieldData.length) {
                    if (fieldData[i + fieldWidth] != -1) {
                        fieldData[i + fieldWidth]++;
                    }
                }
                if (i - (fieldWidth + 1) >= 0 && i % fieldWidth != 0) {
                    if (fieldData[i - (fieldWidth + 1)] != -1) {
                        fieldData[i - (fieldWidth + 1)]++;
                    }
                }
                if (i - (fieldWidth - 1) >= 0 && i % fieldWidth != fieldWidth - 1) {
                    if (fieldData[i - (fieldWidth - 1)] != -1) {
                        fieldData[i - (fieldWidth - 1)]++;
                    }
                }
                if (i + (fieldWidth - 1) < fieldData.length && i % fieldWidth != 0) {
                    if (fieldData[i + (fieldWidth - 1)] != -1) {
                        fieldData[i + (fieldWidth - 1)]++;
                    }
                }
                if (i + (fieldWidth + 1) < fieldData.length && i % fieldWidth != fieldWidth - 1) {
                    if (fieldData[i + (fieldWidth + 1)] != -1) {
                        fieldData[i + (fieldWidth + 1)]++;
                    }
                }
            }//End of fieldData[i] == -1
        }//End of For
    }

    private void printFieldData() {
        for (int i = 0; i < fieldTexts.length; i++) {
            if(fieldData[i] == -1){
                fieldTexts[i].setTextColor(Color.parseColor("#FFC400"));
            }else if (fieldData[i] == 1) {
                fieldTexts[i].setTextColor(Color.parseColor("#5677EA"));
            } else if (fieldData[i] == 2) {
                fieldTexts[i].setTextColor(Color.parseColor("#3F8E17"));
            } else if (fieldData[i] == 3) {
                fieldTexts[i].setTextColor(Color.parseColor("#E64346"));
            } else if (fieldData[i] == 4) {
                fieldTexts[i].setTextColor(Color.parseColor("#273FF4"));
            } else if (fieldData[i] == 5) {
                fieldTexts[i].setTextColor(Color.parseColor("#933529"));
            } else if (fieldData[i] == 6) {
                fieldTexts[i].setTextColor(Color.parseColor("#1C859C"));
            } else {
                fieldTexts[i].setTextColor(Color.parseColor("#000000"));
            }
            if (fieldData[i] != -1 && fieldData[i] != 0) {
                fieldTexts[i].setText(String.format(Locale.getDefault(), "%d", fieldData[i]));
                fieldTexts[i].setTypeface(Typeface.DEFAULT_BOLD);
            } else if (fieldData[i] == 0) {
                fieldTexts[i].setText("");
                fieldTexts[i].setTypeface(Typeface.DEFAULT);
            } else if (fieldData[i] == -1) {
                fieldTexts[i].setText(Html.fromHtml("<big>☢</big>"));
                fieldTexts[i].setTypeface(Typeface.DEFAULT_BOLD);
            }
            fieldTexts[i].setBackgroundResource(R.drawable.field_text_border);
        }
    }

    private void placeFirstHitHint(){
        boolean hintPlaced = false;
        int index = 0;

        while(!hintPlaced){
            if(fieldData[index] == 0 && p(5)){
                fieldButtons[index].setBackgroundResource(R.drawable.field_button_hint);
                hintPlaced = true;
            }

            index++;
            if(index >= fieldData.length) index = 0;
        }
    }

    private void explore(int position) {
        Log.d("MainActivity", "explore : view position is " + position);
        if (userFieldData[position] == -1) return;
        if (fieldData[position] == -1) {
            gameOver(position);
            return;
        }

        fieldButtons[position].setVisibility(View.INVISIBLE);
        userFieldData[position] = 1;

        if (fieldData[position] == 0) {
            if (position - 1 >= 0 && position % fieldWidth != 0) {    //left
                if (fieldButtons[position - 1].getText().equals("") && userFieldData[position - 1] != 1)
                    explore(position - 1); //fieldButtons[position - 1).Visible = False
            }
            if (position + 1 <= fieldData.length - 1 && position % fieldWidth != fieldWidth - 1) {    //right
                if (fieldButtons[position + 1].getText().equals("") && userFieldData[position + 1] != 1)
                    explore(position + 1); //fieldButtons[position + 1).Visible = False
            }
            if (position - fieldWidth >= 0) {    //over
                if (fieldButtons[position - fieldWidth].getText().equals("") && userFieldData[position - fieldWidth] != 1)
                    explore(position - fieldWidth); //fieldButtons[position - fieldWidth).Visible = False
            }
            if (position + fieldWidth <= fieldData.length - 1) {     //below
                if (fieldButtons[position + fieldWidth].getText().equals("") && userFieldData[position + fieldWidth] != 1)
                    explore(position + fieldWidth); //fieldButtons[position + fieldWidth).Visible = False
            }
            if (position - (fieldWidth + 1) >= 0 && position % fieldWidth != 0) {     //left over
                if (fieldButtons[position - (fieldWidth + 1)].getText().equals("") && userFieldData[position - (fieldWidth + 1)] != 1)
                    explore(position - (fieldWidth + 1)); //fieldButtons[position - (fieldWidth + 1)).Visible = False
            }
            if (position - (fieldWidth - 1) >= 0 && position % fieldWidth != fieldWidth - 1) {    //right over
                if (fieldButtons[position - (fieldWidth - 1)].getText().equals("") && userFieldData[position - (fieldWidth - 1)] != 1)
                    explore(position - (fieldWidth - 1)); //fieldButtons[position - (fieldWidth - 1)).Visible = False
            }
            if (position + (fieldWidth - 1) <= fieldData.length - 1 && position % fieldWidth != 0) {      //left below
                if (fieldButtons[position + (fieldWidth - 1)].getText().equals("") && userFieldData[position + (fieldWidth - 1)] != 1)
                    explore(position + (fieldWidth - 1)); //fieldButtons[position + (fieldWidth - 1)).Visible = False
            }
            if (position + (fieldWidth + 1) <= fieldData.length - 1 && position % fieldWidth != fieldWidth - 1) {     //right below
                if (fieldButtons[position + (fieldWidth + 1)].getText().equals("") && userFieldData[position + (fieldWidth + 1)] != 1)
                    explore(position + (fieldWidth + 1)); //fieldButtons[position + (fieldWidth + 1)).Visible = False
            }
        }
    }

    private void toggleFlag(int position) {
        if (userFieldData[position] == 0) {
            userFieldData[position] = -1;
            fieldButtons[position].setText("⛏");
            userCollectedMineCount++;
        } else if (userFieldData[position] == -1) {
            userFieldData[position] = 0;
            fieldButtons[position].setText("");
            userCollectedMineCount--;
        }
        ((TextView) findViewById(R.id.text_mine_left)).setText(getResources().getString(R.string.mine_left) + " " + (maxMineCount - userCollectedMineCount));

        if(userCollectedMineCount == maxMineCount){
            for(int i = 0; i < fieldData.length - 1; i++){
                if(userFieldData[i] == -1 && fieldData[i] != -1) return;
            }
            //Game win
            timer.cancel();
            isGameFinished = true;
            ((Button)findViewById(R.id.button_more)).setText(":)");
        }
    }

    private void gameOver(int position) {
        isGameFinished = true;
        timer.cancel();
        userFieldData[position] = 1;
        fieldTexts[position].setTextColor(Color.RED);
        fieldButtons[position].setVisibility(View.INVISIBLE);
        ((Button)findViewById(R.id.button_more)).setText("X(");
        for(int i = 0; i < fieldData.length; i++){
            if(fieldData[i] == -1 && userFieldData[i] == 0){ fieldButtons[i].setText(Html.fromHtml("<big>☢</big>")); fieldButtons[i].setTextColor(Color.parseColor("#FFC400"));}
            if(userFieldData[i] == -1 && fieldData[i] != -1) fieldButtons[i].setText("!");
        }
    }

    private void restartGame(int width,  int height, int mines){
        for(int i = 0; i < fieldData.length - 1; i++){
            fieldData[i] = 0;
            userFieldData[i] = 0;
            fieldButtons[i] = null;
            fieldTexts[i] = null;
        }
        fieldLayout.removeAllViews();
        time = 0;
        isGameFinished = false;
        isFirstHit = true;
        userCollectedMineCount = 0;
        fieldWidth = width;
        fieldHeight = height;
        maxMineCount = mines;

        initViews();
        placeMines();
        placeFieldData();
        printFieldData();

        timer.cancel();

        placeFirstHitHint();

        System.gc();
    }

    private void showCustomDialog(){
        final View layout = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_newgame, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Game");
        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Restart", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int targetWidth, targetHeight, targetMines;
                EditText width = (EditText) layout.findViewById(R.id.dialog_edit_width);
                EditText height = (EditText) layout.findViewById(R.id.dialog_edit_height);
                EditText mines = (EditText) layout.findViewById(R.id.dialog_edit_mines);
                if(width.getText().toString().length() == 0){
                    targetWidth = 12;
                }else{
                    targetWidth = Integer.parseInt(width.getText().toString());
                }
                if(height.getText().toString().length() == 0){
                    targetHeight = 20;
                }else{
                    targetHeight = Integer.parseInt(height.getText().toString());
                }
                if(mines.getText().toString().length() == 0){
                    targetMines = 30;
                }else{
                    targetMines = Integer.parseInt(mines.getText().toString());
                }
                restartGame(targetWidth, targetHeight, targetMines);
            }
        });
        builder.show();
    }

    private void performButtonClick(View v) {
        if(isGameFinished) return;
        int position = Integer.parseInt(v.getContentDescription().toString().substring(7)) - 1;
        if (currentMode == 0) {
            explore(position);
        } else if (currentMode == 1) {
            toggleFlag(position);
        }
    }

    private void performButtonLongClick(View v) {
        if(isGameFinished) return;
        int position = Integer.parseInt(v.getContentDescription().toString().substring(7)) - 1;
        if (currentMode == 0) {
            toggleFlag(position);
        } else if (currentMode == 1) {
            explore(position);
        }
    }

    private void performTextClick(View v) {
        if(isGameFinished) return;
        int position = Integer.parseInt(v.getContentDescription().toString().substring(5)) - 1;
        int userCollectedAreaMines = 0;

        if (position - 1 >= 0 && position % fieldWidth != 0) {
            if (userFieldData[position - 1] == -1) {
                userCollectedAreaMines += 1;
            }
        }
        if (position + 1 <= fieldData.length - 1 && position % fieldWidth != fieldWidth - 1) {
            if (userFieldData[position + 1] == -1) {
                userCollectedAreaMines += 1;
            }
        }
        if (position - fieldWidth >= 0) {
            if (userFieldData[position - fieldWidth] == -1) {
                userCollectedAreaMines += 1;
            }
        }
        if (position + fieldWidth <= fieldData.length - 1) {
            if (userFieldData[position + fieldWidth] == -1) {
                userCollectedAreaMines += 1;
            }
        }
        if (position - (fieldWidth + 1) >= 0 && position % fieldWidth != 0) {
            if (userFieldData[position - (fieldWidth + 1)] == -1) {
                userCollectedAreaMines += 1;
            }
        }
        if (position - (fieldWidth - 1) >= 0 && position % fieldWidth != fieldWidth - 1) {
            if (userFieldData[position - (fieldWidth - 1)] == -1) {
                userCollectedAreaMines += 1;
            }
        }
        if (position + (fieldWidth - 1) <= fieldData.length - 1 && position % fieldWidth != 0) {
            if (userFieldData[position + (fieldWidth - 1)] == -1) {
                userCollectedAreaMines += 1;
            }
        }
        if (position + (fieldWidth + 1) <= fieldData.length - 1 && position % fieldWidth != fieldWidth - 1) {
            if (userFieldData[position + (fieldWidth + 1)] == -1) {
                userCollectedAreaMines += 1;
            }
        }

        if (userCollectedAreaMines == fieldData[position]) {
            if (position - 1 >= 0 && position % fieldWidth != 0) {
                if (fieldButtons[position - 1].getText().equals("")) explore(position - 1);
            }
            if (position + 1 <= fieldData.length - 1 && position % fieldWidth != fieldWidth - 1) {
                if (fieldButtons[position + 1].getText().equals("")) explore(position + 1);
            }
            if (position - fieldWidth >= 0) {
                if (fieldButtons[position - fieldWidth].getText().equals(""))
                    explore(position - fieldWidth);
            }
            if (position + fieldWidth <= fieldData.length - 1) {
                if (fieldButtons[position + fieldWidth].getText().equals(""))
                    explore(position + fieldWidth);
            }
            if (position - (fieldWidth + 1) >= 0 && position % fieldWidth != 0) {
                if (fieldButtons[position - (fieldWidth + 1)].getText().equals(""))
                    explore(position - (fieldWidth + 1));
            }
            if (position - (fieldWidth - 1) >= 0 && position % fieldWidth != fieldWidth - 1) {
                if (fieldButtons[position - (fieldWidth - 1)].getText().equals(""))
                    explore(position - (fieldWidth - 1));
            }
            if (position + (fieldWidth - 1) <= fieldData.length - 1 && position % fieldWidth != 0) {
                if (fieldButtons[position + (fieldWidth - 1)].getText().equals(""))
                    explore(position + (fieldWidth - 1));
            }
            if (position + (fieldWidth + 1) <= fieldData.length - 1 && position % fieldWidth != fieldWidth - 1) {
                if (fieldButtons[position + (fieldWidth + 1)].getText().equals(""))
                    explore(position + (fieldWidth + 1));
            }
        }

    }

    private boolean p(int possibility) {
        return 1 + new Random().nextInt(100) < possibility;
    }

    private void takeScreenshot() {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 427);
            return;
        }

        Calendar c = Calendar.getInstance();
        String datePath = String.format(Locale.getDefault(), "_%4d%02d%2d-%2d%2d%2d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));

        try {
            // image naming and path  to include sd card  appending name you choose for file
            if(!new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures").exists()){
                new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures").mkdir();
            }
            if(!new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Screenshots").exists()){
                new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Screenshots").mkdir();
            }
            String mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Screenshots/Screenshot" + datePath + ".jpg";

            // create bitmap screen capture
            View v1 = getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.parse("file://" + mPath));
            sendBroadcast(intent);

        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if(isGameFinished) return;
        if(isFirstHit) {
            timer.start();
            isFirstHit = false;
        }
        if (v.getContentDescription().toString().contains("text")) {
            performTextClick(v);
        } else if (v.getContentDescription().toString().contains("button")) {
            performButtonClick(v);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if(isGameFinished) return false;
        if(isFirstHit) {
            timer.start();
            isFirstHit = false;
        }
        performButtonLongClick(v);
        return true;
    }

}
