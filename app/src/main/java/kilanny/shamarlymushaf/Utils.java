package kilanny.shamarlymushaf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.RecoverySystem;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Yasser on 10/11/2015.
 */
public class Utils {

    public static final int[] AYAH_COUNT = {
            7,
            286,
            200,
            176,
            120,
            165,
            206,
            75,
            129,
            109,
            123,
            111,
            43,
            52,
            99,
            128,
            111,
            110,
            98,
            135,
            112,
            78,
            118,
            64,
            77,
            227,
            93,
            88,
            69,
            60,
            34,
            30,
            73,
            54,
            45,
            83,
            182,
            88,
            75,
            85,
            54,
            53,
            89,
            59,
            37,
            35,
            38,
            29,
            18,
            45,
            60,
            49,
            62,
            55,
            78,
            96,
            29,
            22,
            24,
            13,
            14,
            11,
            11,
            18,
            12,
            12,
            30,
            52,
            52,
            44,
            28,
            28,
            20,
            56,
            40,
            31,
            50,
            40,
            46,
            42,
            29,
            19,
            36,
            25,
            22,
            17,
            19,
            26,
            30,
            20,
            15,
            21,
            11,
            8,
            8,
            19,
            5,
            8,
            8,
            11,
            11,
            8,
            3,
            9,
            5,
            4,
            7,
            3,
            6,
            3,
            5,
            4,
            5,
            6
    };

    public static final int DOWNLOAD_SERVER_INVALID_RESPONSE = -1,
            DOWNLOAD_OK = 0,
            DOWNLOAD_MALFORMED_URL = -2,
            DOWNLOAD_FILE_NOT_FOUND = -3,
            DOWNLOAD_IO_EXCEPTION = -4,
            DOWNLOAD_USER_CANCEL = -5;

    public static void initDatabaseDir(Context context) {
        File filesDir;
        // Make sure it's available
        if (isExternalStorageWritable()) {
            // We can read and write the media
            filesDir = context.getExternalFilesDir(null);
        } else {
            // Load another directory, probably local memory
            filesDir = context.getFilesDir();
        }
        MyDbContext.externalFilesDir = filesDir;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static File getSurahDir(Context context, String reciter, int surah) {
        Setting s = Setting.getInstance(context);
        File dir = new File(s.saveSoundsDirectory, "recites/" + reciter
                + "/" + surah);
        return dir;
    }

    public static File getAyahFile(Context context, String reciter, int surah, int ayah,
                                   boolean createDirIfNotExists) {
        File dir = getSurahDir(context, reciter, surah);
        if (createDirIfNotExists && !dir.exists())
            dir.mkdirs();
        return new File(dir, "" + ayah);
    }

    /**
     * Used for less memory usage, less object instantiation
     */
    public static String getAyahFile(int ayah, File surahDir) {
        return surahDir.getAbsolutePath() + "//" + ayah;
    }

    public static String getAyahUrl(String reciter, int surah, int ayah) {
        return String.format(Locale.ENGLISH,
                "http://www.everyayah.com/data/%s/%03d%03d.mp3",
                reciter, surah, ayah);
    }

    public static String getAyahPath(Context context, String reciter, int surah, int ayah) {
        File f = getAyahFile(context, reciter, surah, ayah, false);
        if (f.exists())
            return f.getAbsolutePath();
        return getAyahUrl(reciter, surah, ayah);
    }

    public static int downloadAyah(Context context, String reciter, int surah, int ayah,
                                   byte[] buffer, File surahDir) {
        URL url;
        try {
            url = new URL(getAyahUrl(reciter, surah, ayah));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("connection", "close");
            connection.connect();
            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return DOWNLOAD_SERVER_INVALID_RESPONSE;
            // download the file
            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(
                    getAyahFile(ayah, surahDir));
            int count;
            while ((count = input.read(buffer)) != -1)
                output.write(buffer, 0, count);
            input.close();
            output.close();
            return DOWNLOAD_OK;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return DOWNLOAD_MALFORMED_URL;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return DOWNLOAD_FILE_NOT_FOUND;
        } catch (IOException e) {
            e.printStackTrace();
            return DOWNLOAD_IO_EXCEPTION;
        }
    }

    private static File[] listAyahs(Context context, String reciter, int surah) {
        File file = getSurahDir(context, reciter, surah);
        if (!file.exists())
            return null;
        return file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                for (int i = 0; i < s.length(); ++i)
                    if (!Character.isDigit(s.charAt(i))) return false;
                return true;
            }
        });
    }

    public static ConcurrentLinkedQueue<Integer> getNotDownloaded(Context context,
                          String reciter, int surah, boolean buffer[]) {
        Arrays.fill(buffer, false);
        File files[] = listAyahs(context, reciter, surah);
        if (files != null) {
            for (File f : files) {
                buffer[Integer.parseInt(f.getName())] = true;
            }
        }
        ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
        for (int i = surah == 1 ? 0 : 1; i <= AYAH_COUNT[surah - 1]; ++i)
            if (!buffer[i])
                q.add(i);
        return q;
    }

    public static int getNumDownloaded(Context context, String reciter, int surah) {
        File[] arr = listAyahs(context, reciter, surah);
        return arr == null ? 0 : arr.length;
    }

    private static void myDownloadSurah(final Context context,
                                        final String reciter, final int surah,
                                        final RecoverySystem.ProgressListener progressListener,
                                        final DownloadTaskCompleteListener listener,
                                        final CancelOperationListener cancel,
                                        final boolean[] buffer2) {
        final ConcurrentLinkedQueue<Integer> q =
                Utils.getNotDownloaded(context, reciter, surah, buffer2);
        final File surahDir = getSurahDir(context, reciter, surah);
        if (!surahDir.exists())
            surahDir.mkdirs();
        Thread[] threads = new Thread[4];
        final Shared progress = new Shared();
        progress.setData(AYAH_COUNT[surah - 1] + (surah == 1 ? 1 : 0) - q.size());
        final Shared error = new Shared();
        error.setData(DOWNLOAD_OK);
        final Lock lock = new ReentrantLock(true);
        for (int th = 0; th < threads.length; ++th) {
            threads[th] = new Thread(new Runnable() {

                @Override
                public void run() {
                    byte[] buf = new byte[1024];
                    while (cancel.canContinue() && error.getData() == DOWNLOAD_OK) {
                        Integer per = q.poll();
                        if (per == null) break;
                        int code = downloadAyah(context, reciter, surah, per, buf, surahDir);
                        lock.lock();
                        if (error.getData() == DOWNLOAD_OK) {
                            error.setData(code);
                        }
                        lock.unlock();
                        if (code == DOWNLOAD_OK) {
                            progress.increment();
                            progressListener.onProgress(progress.getData());
                        }
                    }
                }
            });
        }
        for (int i = 0; i < threads.length; ++i)
            threads[i].start();
        for (int i = 0; i < threads.length; ++i)
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        listener.taskCompleted(!cancel.canContinue() ? DOWNLOAD_USER_CANCEL : error.getData());
    }

    public static AsyncTask downloadSurah(final Context context,
                              final String reciter, final int surah,
                              final RecoverySystem.ProgressListener progress,
                              final DownloadTaskCompleteListener listener) {
        return new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                final Shared res = new Shared();
                myDownloadSurah(context, reciter, surah, new RecoverySystem.ProgressListener() {
                    @Override
                    public void onProgress(int progress) {
                        publishProgress(progress);
                    }
                }, new DownloadTaskCompleteListener() {
                    @Override
                    public void taskCompleted(int result) {
                        res.setData(result);
                    }
                }, new CancelOperationListener() {
                    @Override
                    public boolean canContinue() {
                        return !isCancelled();
                    }
                }, new boolean[290]);
                return res.getData();
            }

            @Override
            protected void onProgressUpdate(final Integer... values) {
                progress.onProgress(values[0]);
            }

            @Override
            protected void onCancelled() {
                listener.taskCompleted(DOWNLOAD_USER_CANCEL);
            }

            @Override
            protected void onPostExecute(Integer result) {
                listener.taskCompleted(result);
            }
        }.execute();
    }

    public static boolean deleteSurah(Context context, String reciter, int surah) {
        File[] files = listAyahs(context, reciter, surah);
        if (files == null) return true;
        boolean res = true;
        for (File f : files) {
            res &= f.delete();
        }
        return res;
    }

    public static void showConfirm(Context context, String title, String msg,
                             DialogInterface.OnClickListener ok,
                             DialogInterface.OnClickListener cancel) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("نعم", ok)
                .setNegativeButton("لا", cancel)
                .show();
    }

    public static void deleteAll(final Context context, final String reciter,
                                 final RecoverySystem.ProgressListener progress,
                                 final Runnable finish) {
        showConfirm(context, "حذف جميع التلاوات لقارئ",
                "حذف جميع التلاوات التي تم تحميلها لهذا القارئ نهائيا؟",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog show = new ProgressDialog(context);
                        show.setTitle("حذف جميع تلاوات قارئ");
                        show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        show.setIndeterminate(false);
                        show.setCancelable(false);
                        show.setMax(114);
                        show.setProgress(0);
                        show.show();
                        new AsyncTask<Void, Integer, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                for (int i = 1; i <= 114; ++i) {
                                    deleteSurah(context, reciter, i);
                                    publishProgress(i);
                                }
                                return null;
                            }

                            @Override
                            protected void onProgressUpdate(final Integer... values) {
                                show.setProgress(values[0]);
                                progress.onProgress(values[0]);
                            }

                            @Override
                            protected void onPostExecute(Void v) {
                                finish.run();
                                show.dismiss();
                            }
                        }.execute();
                    }
                }, null);
    }

    public static View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    public static AsyncTask downloadAll(final Activity context, final String reciter,
                                   final DownloadAllProgressChangeListener progress,
                                   final DownloadTaskCompleteListener listener) {
        return new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                final Shared error = new Shared();
                error.setData(DOWNLOAD_OK);
                boolean[] buffer = new boolean[290];
                for (int i = 1; !isCancelled() && error.getData() == DOWNLOAD_OK && i <= 114; ++i) {
                    final int surah = i;
                    myDownloadSurah(context, reciter, i, new RecoverySystem.ProgressListener() {
                        @Override
                        public void onProgress(int progress) {
                            publishProgress(surah, progress);
                        }
                    }, new DownloadTaskCompleteListener() {
                        @Override
                        public void taskCompleted(int result) {
                            error.setData(result);
                        }
                    }, new CancelOperationListener() {
                        @Override
                        public boolean canContinue() {
                            return !isCancelled();
                        }
                    }, buffer);
                }
                return isCancelled() ? DOWNLOAD_USER_CANCEL : error.getData();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progress.onProgressChange(values[0], values[1]);
            }

            @Override
            protected void onCancelled() {
                listener.taskCompleted(DOWNLOAD_USER_CANCEL);
            }

            @Override
            protected void onPostExecute(Integer result) {
                listener.taskCompleted(result);
            }
        }.execute();
    }

    public static void showAlert(Context context, String title, String msg, DialogInterface.OnClickListener ok) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(context);
        dlgAlert.setMessage(msg);
        dlgAlert.setTitle(title);
        dlgAlert.setPositiveButton("موافق", ok);
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }
}

interface DownloadTaskCompleteListener {
    void taskCompleted(int result);
}

interface DownloadAllProgressChangeListener {
    void onProgressChange(int surah, int ayah);
}

interface CancelOperationListener {
    boolean canContinue();
}