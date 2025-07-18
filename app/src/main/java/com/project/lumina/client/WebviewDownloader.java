package com.project.lumina.client;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebviewDownloader extends AppCompatActivity {
    
    private WebView webView;
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private Handler mainHandler;
    private ExecutorService downloadExecutor;
    private String currentDownloadApkPath;
    private static final int THREAD_COUNT = 8;
    private BroadcastReceiver installReceiver;
    private volatile boolean isDownloadCancelled = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_downloader);
       
        mainHandler = new Handler(Looper.getMainLooper());
        downloadExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        webView = findViewById(R.id.webview_downloader);
        setupWebView();
        
        String url = getIntent().getStringExtra("url");
        if (url != null) {
            webView.loadUrl(url);
        }
        
        registerInstallReceiver();
    }

    private void registerInstallReceiver() {
        installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && 
                    (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) ||
                     Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()))) {
                    
                    if (intent.getData() != null) {
                        String installedPackage = intent.getData().getSchemeSpecificPart();
                        if (currentDownloadApkPath != null && isApkInstalled(installedPackage, currentDownloadApkPath)) {
                            cleanDownloadCache();
                            currentDownloadApkPath = null;
                        }
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(installReceiver, filter);
    }
    
    private boolean isApkInstalled(String packageName, String apkPath) {
        try {
            PackageInfo installed = getPackageManager().getPackageInfo(packageName, 0);
            PackageInfo downloaded = getPackageManager().getPackageArchiveInfo(apkPath, 0);
            return installed != null && downloaded != null && 
                   installed.packageName.equals(downloaded.packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                    return false;
                }
                
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Toast.makeText(WebviewDownloader.this, "无法打开链接: " + url, Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());
        
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, 
                    String contentDisposition, String mimetype, long contentLength) {
                String fileName = parseFileName(url, contentDisposition, mimetype);
                startMultiThreadDownload(url, fileName, contentLength);
            }
        });
    }
    
    private String parseFileName(String url, String contentDisposition, String mimetype) {
        String fileName = null;
        
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            int startIndex = contentDisposition.indexOf("filename=") + 9;
            int endIndex = contentDisposition.indexOf(";", startIndex);
            if (endIndex == -1) endIndex = contentDisposition.length();
            fileName = contentDisposition.substring(startIndex, endIndex).replace("\"", "");
        }
        
        if (fileName == null) {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash > 0) {
                fileName = url.substring(lastSlash + 1);
                int paramIndex = fileName.indexOf('?');
                if (paramIndex > 0) {
                    fileName = fileName.substring(0, paramIndex);
                }
            }
        }
        
        if (fileName != null) {
            try {
                fileName = URLDecoder.decode(fileName, "UTF-8");
            } catch (Exception ignored) {}
        } else {
            fileName = "download_file";
        }
        
        if (mimetype.equals("application/vnd.android.package-archive") 
            || url.toLowerCase().endsWith(".apk")) {
            if (!fileName.toLowerCase().endsWith(".apk")) {
                fileName += ".apk";
            }
        }
        
        return fileName;
    }
    
    private void startMultiThreadDownload(String downloadUrl, String fileName, long totalSize) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        
        progressText = new TextView(this);
        progressText.setText("文件名: " + fileName + "\n进度: 0%");
        progressText.setPadding(0, 0, 0, 20);
        
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        
        layout.addView(progressText);
        layout.addView(progressBar);
        
        // 添加取消按钮
        progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("下载中")
                .setView(layout)
                .setNegativeButton("取消", (dialog, which) -> {
                    isDownloadCancelled = true;
                    if (downloadExecutor != null) {
                        downloadExecutor.shutdownNow();
                        downloadExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
                    }
                    Toast.makeText(this, "下载已取消", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        isDownloadCancelled = false;
        downloadExecutor.execute(() -> {
            try {
                File downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir != null && !downloadDir.exists()) {
                    downloadDir.mkdirs();
                }
                
                File outputFile = new File(downloadDir, fileName);
                multiThreadDownload(downloadUrl, outputFile, totalSize, fileName);
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (progressDialog != null) progressDialog.dismiss();
                    Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void multiThreadDownload(String downloadUrl, File outputFile, long totalSize, String fileName) throws IOException {
        // 如果文件已存在，直接打开
        if (outputFile.exists() && outputFile.length() == totalSize) {
            mainHandler.post(() -> {
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(this, "文件已存在", Toast.LENGTH_SHORT).show();
                openFile(outputFile);
            });
            return;
        }
        
        final int threadCount = THREAD_COUNT;
        Thread[] downloadThreads = new Thread[threadCount];
        long[] progressPerThread = new long[threadCount];
        long[] completedPerThread = new long[threadCount]; // 记录每个线程的总完成量
        
        long blockSize = totalSize / threadCount;
        long remaining = totalSize % threadCount;
        
        // 启动下载线程
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            final long startPos = i * blockSize;
            final long endPos;
            
            // 为最后一个线程分配剩余字节
            if (i == threadCount - 1) {
                endPos = startPos + blockSize + remaining - 1;
            } else {
                endPos = startPos + blockSize - 1;
            }
            
            downloadThreads[i] = new Thread(() -> {
                try {
                    completedPerThread[threadIndex] = downloadPart(downloadUrl, outputFile, 
                            startPos, endPos, threadIndex, progressPerThread);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            downloadThreads[i].start();
        }
        
        // 启动进度监控线程
        Thread progressThread = new Thread(() -> {
            long lastProgress = 0;
            
            while (!isDownloadCancelled) {
                long totalDownloaded = 0;
                for (long p : progressPerThread) {
                    totalDownloaded += p;
                }
                
                if (totalSize <= 0) {
                    // 如果totalSize未知（可能是0），不更新进度
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }
                
                // 进度百分比计算
                int percent = (int) ((totalDownloaded * 100) / totalSize);
                
                // 确保进度在0-100之间
                if (percent < 0) percent = 0;
                if (percent > 100) percent = 100;
                
                final int displayPercent = percent;
                
                // 仅当进度有变化时才更新UI（减少UI刷新频率）
                if (displayPercent != lastProgress) {
                    lastProgress = displayPercent;
                    mainHandler.post(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressText.setText("文件名: " + fileName + "\n进度: " + displayPercent + "%");
                            progressBar.setProgress(displayPercent);
                        }
                    });
                }
                
                // 检查是否完成
                if (totalDownloaded >= totalSize) {
                    break;
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        progressThread.start();
        
        // 等待所有下载线程完成
        for (Thread thread : downloadThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 中断进度监控线程
        progressThread.interrupt();
        
        // 检查是否被取消
        if (isDownloadCancelled) {
            // 清理临时文件
            for (int i = 0; i < threadCount; i++) {
                File tempFile = new File(outputFile.getAbsolutePath() + ".temp" + i);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
            return;
        }
        
        // 合并临时文件
        mergeTempFiles(outputFile, threadCount);
        
        mainHandler.post(() -> {
            if (progressDialog != null) progressDialog.dismiss();
            Toast.makeText(this, "下载完成", Toast.LENGTH_SHORT).show();
            openFile(outputFile);
        });
    }
    
    /**
     * 下载文件片段
     * 
     * @return 该片段实际下载的字节数
     */
    private long downloadPart(String downloadUrl, File outputFile, long startPos, long endPos, 
            int threadIndex, long[] progress) throws IOException {
        
        File tempFile = new File(outputFile.getAbsolutePath() + ".temp" + threadIndex);
        
        // 断点续传：获取已下载的字节数
        long downloadedBytes = tempFile.exists() ? tempFile.length() : 0;
        long newStartPos = startPos + downloadedBytes;
        
        // 检查是否已完成该分块下载
        if (newStartPos > endPos) {
            progress[threadIndex] = (endPos - startPos) + 1;
            return (endPos - startPos) + 1; // 返回总片段大小
        }
        
        // 修复：如果文件大小未知（contentLength=0），使用单线程下载整个文件
        if (startPos == 0 && endPos == 0) {
            startPos = 0;
            endPos = Long.MAX_VALUE;
            newStartPos = downloadedBytes;
        }

        // 修正问题：记录该片段总共需要下载的字节数
        long totalSegmentSize = (endPos - startPos) + 1;
        long downloadedThisSession = downloadedBytes; // 该会话下载的字节数
        
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 只设置Range如果分块有效
            if (endPos > 0) {
                connection.setRequestProperty("Range", "bytes=" + newStartPos + "-" + endPos);
            } else if (downloadedBytes > 0) {
                // 文件大小未知时续传
                connection.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
            }
            
            connection.connect();
            
            // 检查服务器是否支持断点续传
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned " + responseCode);
            }
            
            InputStream inputStream = connection.getInputStream();
            // 使用追加模式写入文件
            FileOutputStream outputStream = new FileOutputStream(tempFile, downloadedBytes > 0);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while (!isDownloadCancelled && (bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                downloadedThisSession += bytesRead;
                
                // 修正进度计算：这里记录的是片段内已下载的字节数（而非全局位置）
                progress[threadIndex] = downloadedThisSession;
            }
            
            outputStream.close();
            inputStream.close();
            connection.disconnect();
            
            // 返回该片段总共下载的字节数（包括之前的续传）
            return downloadedThisSession;
            
        } catch (IOException e) {
            // 发生错误时，保存已下载部分以便续传
            return downloadedThisSession;
        }
    }
    
    private void mergeTempFiles(File outputFile, int threadCount) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (int i = 0; i < threadCount; i++) {
                File tempFile = new File(outputFile.getAbsolutePath() + ".temp" + i);
                if (!tempFile.exists()) {
                    continue; // 有些线程可能没有下载内容
                }
                
                try (FileInputStream fis = new FileInputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                
                // 删除临时文件
                tempFile.delete();
            }
        }
    }
    
    private void openFile(File file) {
        try {
            String mimeType = getMimeType(file.getName());
            
            if (mimeType.equals("application/vnd.android.package-archive")) {
                currentDownloadApkPath = file.getAbsolutePath();
                installApk(file);
            } else {
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void installApk(File apkFile) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "安装失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void cleanDownloadCache() {
        try {
            File downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (downloadDir != null && downloadDir.exists()) {
                // 删除下载的APK
                if (currentDownloadApkPath != null) {
                    File apkFile = new File(currentDownloadApkPath);
                    if (apkFile.exists()) {
                        apkFile.delete();
                    }
                }
                
                // 删除临时文件
                File[] files = downloadDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".temp") || 
                            file.getName().toLowerCase().endsWith(".apk")) {
                            file.delete();
                        }
                    }
                }
                
                // 清空下载历史
                webView.clearHistory();
                webView.clearCache(true);
                
                Toast.makeText(this, "安装完成，缓存已清除", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("WebviewDownloader", "清除缓存失败: " + e.getMessage());
        }
    }
    
    private String getMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        
        String extension = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }
        
        switch (extension) {
            case "apk":
                return "application/vnd.android.package-archive";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            default:
                return "application/octet-stream";
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadExecutor != null) {
            downloadExecutor.shutdownNow();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        unregisterReceiver(installReceiver);
    }
}
