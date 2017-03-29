package in.deepaksood.videocutntrim.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.appyvet.rangebar.RangeBar;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;

import in.deepaksood.videocutntrim.R;

public class MainActivity extends AppCompatActivity {

    // TAG for logging
    private static final String TAG = MainActivity.class.getSimpleName();

    // static variable to store the request code for browse functionality
    private static final int READ_REQUEST_CODE = 42;

    // linear layout hosts both the seek bar and the timer TextView associated with it
    private LinearLayout llCutController;

    // browse button for searching video from file explorer
    private Button btnBrowse;
    private Button btnCut;
    private TextView tvBrowse;
    private RangeBar rbView;
    private TextView tvStartTime;
    private TextView tvEndTime;

    private VideoView vvPlayer;
    private MediaController mediaController;
    private int videoDurationSeconds;

    private Uri uri;
    private int cutStartTimeSeconds;
    private int cutEndTimeSeconds;
    private FFmpeg ffmpeg;
    private String uploadVideoName;
    private String cutVideoName;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llCutController = (LinearLayout) findViewById(R.id.ll_cut_controller);
        llCutController.setVisibility(View.INVISIBLE);
        tvStartTime = (TextView) findViewById(R.id.tv_start_time);
        tvEndTime = (TextView) findViewById(R.id.tv_end_time);
        rbView = (RangeBar) findViewById(R.id.rb_view);
        vvPlayer = (VideoView) findViewById(R.id.vv_player);

        tvBrowse = (TextView) findViewById(R.id.tv_browse);
        btnCut = (Button) findViewById(R.id.btn_cut);
        btnBrowse = (Button) findViewById(R.id.btn_browse);
        btnBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });

        btnCut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(uri != null && !uri.toString().contentEquals("")) {
                    Log.v(TAG,"cutStartTimeSeconds: "+cutStartTimeSeconds);
                    Log.v(TAG,"cutEndTimeSeconds: "+cutEndTimeSeconds);
                    cutVideo(cutStartTimeSeconds, cutEndTimeSeconds);

                } else {
                    Toast.makeText(MainActivity.this, "please select a video first", Toast.LENGTH_SHORT).show();
                }

            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);

        loadFFMPEGBinary();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.v(TAG,"data: "+data);
            if(data != null) {
                uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                tvBrowse.setText(uri.toString());
                uploadVideoName = uri.getLastPathSegment();
                Log.v(TAG,"uri.getLastPathSegment: "+uri.getLastPathSegment());
                setVideoContainer();
            }
        } else {
            Log.v(TAG,"requestCode: "+requestCode);
        }
    }

    private void setVideoContainer() {
        if(uri != null) {
            mediaController = new MediaController(this) {
                @Override
                public void hide() {}
            };
            mediaController.setAnchorView(vvPlayer);
            vvPlayer.setMediaController(mediaController);
            vvPlayer.setVideoURI(uri);
            vvPlayer.requestFocus();
            vvPlayer.start();
            llCutController.setVisibility(View.VISIBLE);
        }

        vvPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                videoDurationSeconds = mp.getDuration()/1000;
                Log.v(TAG,"VideoDurationSeconds: "+videoDurationSeconds);
                rbView.setVisibility(View.VISIBLE);
                rbView.setTickEnd(videoDurationSeconds);
                tvEndTime.setText(convertTime(videoDurationSeconds));
            }
        });

        rbView.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
                vvPlayer.seekTo(leftPinIndex*1000);
                cutStartTimeSeconds = leftPinIndex;
                cutEndTimeSeconds = rightPinIndex;
                tvStartTime.setText(convertTime(leftPinIndex));
                tvEndTime.setText(convertTime(rightPinIndex));
            }
        });


    }

    public String convertTime(int videoDurationSeconds) {
        int hr = videoDurationSeconds / 3600;
        int rem = videoDurationSeconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec);
    }

    private void loadFFMPEGBinary() {

        if(ffmpeg == null) {
            ffmpeg = FFmpeg.getInstance(this);
        }
        try {
            ffmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Log.v(TAG,"ffmpeg not supported");
                    Toast.makeText(MainActivity.this, "Ffmpeg not supported", Toast.LENGTH_SHORT).show();
                    MainActivity.this.finish();
                }

                @Override
                public void onSuccess() {
                    Log.v(TAG,"ffmpeg supported");
                }

                @Override
                public void onStart() {

                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
    }

    private void cutVideo(int cutStartTimeSeconds, int cutEndTimeSeconds) {
        getSaveDirectory();

        /*String videoLocation = getPath(MainActivity.this, uri);

        Log.d(TAG, "cutStartTimeSeconds: " + cutStartTimeSeconds);
        Log.d(TAG, "cutEndTimeSeconds: " + cutEndTimeSeconds);
        Log.d(TAG, "uri: " + uri);
        Log.d(TAG, "videoLocation: " + videoLocation);

        String[] complexCommand = {"-ss", "" + cutStartTimeSeconds / 1000, "-y", "-i", videoLocation, "-t", "" + (cutEndTimeSeconds - cutStartTimeSeconds) / 1000, "-s", "320x240", "-r", "15", "-vcodec", "mpeg4", "-b:v", "2097152", "-b:a", "48000", "-ac", "2", "-ar", "22050", cutVideoName};

        execFFmpegBinary(complexCommand);*/
    }

    private void getSaveDirectory() {
        File directoryFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if(directoryFile.exists() && directoryFile.isDirectory()) {
            cutVideoName = directoryFile.getAbsolutePath() + "/cropped_"+uploadVideoName+".mp4";
            Log.v(TAG,"cutVideoName: "+cutVideoName);
        }
    }

    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : " + s);
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output : " + s);

                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage("progress : " + s);
                    Log.d(TAG, "progress : " + s);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage("Processing...");
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg " + command);
                    progressDialog.dismiss();

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    private String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri.
     */
    private String getDataColumn(Context context, Uri uri, String selection,
                                 String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

}
