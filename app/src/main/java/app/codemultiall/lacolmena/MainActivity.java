package app.codemultiall.lacolmena;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfRenderer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.Browser;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.pusher.pushnotifications.PushNotifications;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION = 100;

    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID estándar para SPP
    private static final int FCR = 1;

    private WebView webView;
    private LottieAnimationView splash;
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private boolean isWebViewLoaded = false;
    private boolean isAnimationFinished = false;
    private ProgressBar loading;
    private String currentUrl;
    private GestureDetector gestureDetector;
    private int touchCount = 0; // Contador de toques
    private OkHttpClient client;
    private static final long DOUBLE_TAP_TIMEOUT = 300; // Tiempo para reiniciar el contador (en milisegundos)
    private long lastTouchTime = 0;
    private String webViewCookies;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PushNotifications.start(getApplicationContext(), "d79e14a6-d14c-40c2-bd1c-ce07b15a626b");
        PushNotifications.addDeviceInterest("hello");

        pedirPermisos();

        webView = findViewById(R.id.web);
        splash = findViewById(R.id.splash);
        loading = findViewById(R.id.spinner);



        setupWebView();
        // Verificar si la impresora está configurada
        if (isPrinterConfigured()) {
            // Si está configurada, cargar la configuración
            loadPrinterConfiguration();
        }
        client = new OkHttpClient();
    }

    private void pedirPermisos() {
        requestPermissions(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        }, 1);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        //webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        webView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
            return false;
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Cargar error.html cada vez que ocurra un error en el WebView
                view.loadUrl("file:///android_asset/error.html");
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("imprimir-venta?modelo=ticket")) {
                    // Si ya tienes permisos, descarga el PDF
                    String[] params = extractParameters(url);
                    String id = params[0];
                    String emisor = params[1];

                    // Construir la nueva URL
                    String URL_API = "https://estudiocontablelacolmena.com/sistema/presentacion/controller/api/venta?id=" + id + "&emisor=" + emisor;
                    if (!isPrinterConfigured()) {
                        showPrinterDialog();
                    }else {
                        new FetchDataVenta().execute(URL_API);
                    }
                    return true;
                }
                if(url.contains("imprimir-cotizacion?modelo=ticket")){
                    String[] params = extractParameters(url);
                    String id = params[0];
                    String emisor = params[1];

                    // Construir la nueva URL
                    String URL_API = "https://estudiocontablelacolmena.com/sistema/presentacion/controller/api/cotizacion?id=" + id + "&emisor=" + emisor;
                    if (!isPrinterConfigured()) {
                        showPrinterDialog();
                    }else {
                        new FetchDataCotizacion().execute(URL_API);
                    }
                    return true;
                }

                if(url.contains("imprimir-guia?modelo=ticket")){
                    String[] params = extractParameters(url);
                    String id = params[0];
                    String emisor = params[1];
                    // Construir la nueva URL
                    String URL_API = "https://estudiocontablelacolmena.com/sistema/presentacion/controller/api/guia?id=" + id + "&emisor=" + emisor;
                    if (!isPrinterConfigured()) {
                        showPrinterDialog();
                    }else {
                        new FetchDataGuia().execute(URL_API);
                    }
                    return true;
                }

                if (url.matches(".*modelo=(xml|cdr).*") || url.contains("imprimir-pedido?modelo=ticket") || url.contains("imprimir-venta?modelo=pdf")) {
                    abrirEnCustomTab(view.getContext(), url);
                    return true;
                }
                if (url.matches(".*imprimir-(productos|comprobantes|guias|cotizaciones|reportes|guia-dia-mes|clientes|transportistas|conductores).*") || url.contains("plantilla") || url.matches(".*exportar-(sire|excel).*") || url.contains("imprimir-cotizacion?modelo=pdf") || url.contains("imprimir-guia?modelo=pdf")) {
                    //abrirEnCustomTab(view.getContext(), url);
                    descargarYAbrirArchivo(view.getContext(), url);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {

                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                cookieManager.flush(); // Forzar guardado de cookies en memoria

                // Obtener y guardar cookies manualmente
                webViewCookies = cookieManager.getCookie(url);

                isWebViewLoaded = true;
                // Oculta el ImageView y muestra el WebView cuando la página haya cargado completamente
                if (isAnimationFinished) {
                    splash.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }

                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
        });

        webView.loadUrl("https://estudiocontablelacolmena.com/sistema/presentacion/");


        splash.postDelayed(new Runnable() {
            @Override
            public void run() {
                isAnimationFinished = true;
                // Verifica si ya terminó la carga del WebView para ocultar el splash
                if (isWebViewLoaded) {
                    splash.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.GONE);
                }
            }
        }, 6300);

        splash.postDelayed(new Runnable() {
            @Override
            public void run() {
                loading.setVisibility(View.VISIBLE); // Muestra la ruedita
            }
        }, 4170);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                startFileChooserIntent();
                return true;
            }


            // Other overridden methods related to file upload
        });

        // Configura el WebView y su OnTouchListener
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Solo contar toques en ACTION_DOWN
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    long currentTime = System.currentTimeMillis();

                    // Verifica si el tiempo desde el último toque es mayor que el tiempo de espera
                    if (currentTime - lastTouchTime > DOUBLE_TAP_TIMEOUT) {
                        touchCount = 0; // Reinicia el contador si ha pasado el tiempo
                    }

                    lastTouchTime = currentTime; // Actualiza el tiempo del último toque
                    touchCount++; // Aumenta el contador de toques

                    // Muestra el diálogo si se detectan tres toques
                    if (touchCount == 3) {
                        showPrinterDialog(); // Llama a tu método para mostrar el diálogo
                        touchCount = 0; // Reinicia el contador después de mostrar el diálogo
                    }
                }

                // Permitir que el WebView maneje los eventos táctiles
                return false;
            }
        });

    }

    private void startFileChooserIntent() {
        //Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Add code for taking picture

        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Selecciona una imagen");
        //chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});

        startActivityForResult(chooserIntent, FCR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            handleFileChooserForLollipop(requestCode, resultCode, intent);
        } else {
            handleFileChooserForLegacy(requestCode, resultCode, intent);
        }
    }

    private void handleFileChooserForLollipop(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FCR) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                } else if (mCM != null) {
                    results = new Uri[]{Uri.parse(mCM)};
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }
    }

    private void handleFileChooserForLegacy(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FCR) {
            if (mUM != null) {
                Uri result = (intent == null || resultCode != RESULT_OK) ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    //Metodos para que la app cambie en la pantalla dividida
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle configuration changes if needed
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registrar el receptor cuando la actividad se vuelve visible
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    // BroadcastReceiver para escuchar cambios en la conectividad
    private final BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkAvailable()) {
                // Si hay conexión, recargar la URL actual
                webView.loadUrl(currentUrl);
            } else {
                // Si no hay conexión, almacenar la URL actual
                currentUrl = webView.getUrl();
                webView.loadUrl("file:///android_asset/error.html"); // Cargar error.html si no hay conexión
            }
        }
    };

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Desregistrar el receptor cuando la actividad ya no es visible
        unregisterReceiver(networkChangeReceiver);
    }

    //para impresiom


    // Mostrar el diálogo para elegir impresora Bluetooth o de red
    private void showPrinterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar tipo de impresora");

        String[] printerTypes = {"Bluetooth", "Red"};
        builder.setItems(printerTypes, (dialog, which) -> {
            if (which == 0) {
                showBluetoothPrinterDialog();
            } else {
                showNetworkPrinterDialog();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    // Mostrar diálogo para impresora Bluetooth y preguntar por el tamaño
    private void showBluetoothPrinterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tamaño de la impresora Bluetooth");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_encode, null);
        builder.setView(dialogView);
        RadioGroup encodingRadioGroup = dialogView.findViewById(R.id.encodingRadioGroup);
        RadioGroup sizeRadioGroup = dialogView.findViewById(R.id.sizeRadioGroup);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            // Obtener el tamaño y la codificación seleccionados
            int selectedSizeId = sizeRadioGroup.getCheckedRadioButtonId();
            String selectedSize = selectedSizeId == R.id.size58 ? "58" : "80";
            int selectedEncodingId = encodingRadioGroup.getCheckedRadioButtonId();
            String selectedEncoding = selectedEncodingId == R.id.utf8 ? "UTF-8" : "ISO-8859-1";

            // Configurar la impresora con el tamaño y codificación seleccionados
            configureBluetoothPrinter(selectedSize, selectedEncoding);
        });

        // Botón de cancelación
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        // Mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    // Configurar la impresora Bluetooth y guardar en SharedPreferences
    private void configureBluetoothPrinter(String size, String encode) {
        savePreferences("printer_type", "bluetooth");
        savePreferences("bluetooth_size", size);
        savePreferences("bluetooth_encode", encode);
        Toast.makeText(this, "Configurando impresora Bluetooth de " + size + " encode " + encode, Toast.LENGTH_SHORT).show();
    }
    // Mostrar diálogo para impresora de red y solicitar IP, puerto y tamaño
    private void showNetworkPrinterDialog() {
        View vista = getLayoutInflater().inflate(R.layout.dialog_network_printer, null);
        EditText editTextIP = vista.findViewById(R.id.editTextIP);
        EditText editTextPort = vista.findViewById(R.id.editTextPort);
        RadioGroup encodingRadioGroup = vista.findViewById(R.id.encodingRadioGroup);
        RadioGroup sizeRadioGroup = vista.findViewById(R.id.sizeRadioGroup);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurar impresora de red");
        builder.setView(vista);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String ip = editTextIP.getText().toString();
            String port = editTextPort.getText().toString();
            int selectedSizeId = sizeRadioGroup.getCheckedRadioButtonId();
            String selectedSize = selectedSizeId == R.id.size58 ? "58" : "80";
            int selectedEncodingId = encodingRadioGroup.getCheckedRadioButtonId();
            String selectedEncode = selectedEncodingId == R.id.utf8 ? "UTF-8" : "ISO-8859-1";
            configureNetworkPrinter(ip, port, selectedSize, selectedEncode);
        });

        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    // Configurar la impresora de red y guardar en SharedPreferences
    private void configureNetworkPrinter(String ip, String port, String size, String encode) {
        savePreferences("printer_type", "network");
        savePreferences("network_ip", ip);
        savePreferences("network_port", port);
        savePreferences("network_size", size);
        savePreferences("network_encode", encode);
        Toast.makeText(this, "Configurando impresora de red: " + ip + ":" + port + " - Tamaño: " + size + " - Encode: " + encode, Toast.LENGTH_SHORT).show();
    }

    // Guardar los datos en SharedPreferences
    private void savePreferences(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrinterConfig", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    // Recuperar los datos guardados desde SharedPreferences
    private String getPreference(String key) {
        SharedPreferences sharedPreferences = getSharedPreferences("PrinterConfig", MODE_PRIVATE);
        return sharedPreferences.getString(key, "");
    }

    // Verificar si la impresora ha sido configurada
    private boolean isPrinterConfigured() {
        SharedPreferences sharedPreferences = getSharedPreferences("PrinterConfig", MODE_PRIVATE);
        String printerType = sharedPreferences.getString("printer_type", null);
        return printerType != null;  // Si el tipo de impresora está configurado, devuelve true
    }

    // Cargar la configuración guardada al iniciar la aplicación
    private void loadPrinterConfiguration() {
        String printerType = getPreference("printer_type");

        if ("bluetooth".equals(printerType)) {
            String size = getPreference("bluetooth_size");
            String encode = getPreference("bluetooth_encode");
            Toast.makeText(this, "Impresora Bluetooth configurada de tamaño: " + size + " - Encode: " + encode, Toast.LENGTH_SHORT).show();
        } else if ("network".equals(printerType)) {
            String ip = getPreference("network_ip");
            String port = getPreference("network_port");
            String size = getPreference("network_size");
            String encode = getPreference("network_encode");
            Toast.makeText(this, "Impresora de red configurada: " + ip + ":" + port + " - Tamaño: " + size + " - Encode: " + encode, Toast.LENGTH_SHORT).show();
        }
    }

    // Método para extraer los parámetros de la URL
    private String[] extractParameters(String url) {
        String id = "";
        String emisor = "";

        // Extraer los parámetros de la URL
        String[] urlParts = url.split("\\?");
        if (urlParts.length > 1) {
            String[] queryParams = urlParts[1].split("&");
            for (String param : queryParams) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if (pair[0].equals("id")) {
                        id = pair[1];
                    } else if (pair[0].equals("emisor")) {
                        emisor = pair[1];
                    }
                }
            }
        }
        return new String[]{id, emisor};
    }

    private class FetchDataVenta extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
            } catch (Exception e) {
                Log.e("API Error Venta", "Error: " + e.getMessage());
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // Aquí puedes manejar el JSON recibido
            //Log.d("API VENTA", result);
            mandarImprimirVenta(result);
        }
    }

    private class FetchDataCotizacion extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
            } catch (Exception e) {
                Log.e("API Error Cotizacion", "Error: " + e.getMessage());
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // Aquí puedes manejar el JSON recibido
            //Log.d("API COTIZACION", result);
            mandarImprimirCotizacion(result);
        }
    }

    private class FetchDataGuia extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
            } catch (Exception e) {
                Log.e("API Error Guias", "Error: " + e.getMessage());
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // Aquí puedes manejar el JSON recibido
            //Log.d("API COTIZACION", result);
            mandarImprimirGuia(result);
        }
    }
    private void mandarImprimirVenta(String data) {
        final Context context = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Cargar el bitmap desde el archivo PNG
                if (data != null && isPrinterConfigured()) {
                    String printerType = getPreference("printer_type");
                    EscPosPrinter printer = null;
                    int size = 0;
                    String linea = "";

                    try {
                        if ("bluetooth".equals(printerType)) {
                            size = Integer.parseInt(getPreference("bluetooth_size"));
                            String encode = getPreference("bluetooth_encode");
                            if (size == 58) {
                                printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32, new EscPosCharsetEncoding(encode, 6));
                                linea = "[L]--------------------------------\n";
                            } else if (size == 80) {
                                printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 72f, 48, new EscPosCharsetEncoding(encode, 6));
                                linea = "[L]------------------------------------------------\n";
                            }
                        } else if ("network".equals(printerType)) {
                            String ip = getPreference("network_ip");
                            String encode = getPreference("network_encode");
                            int port = Integer.parseInt(getPreference("network_port"));
                            size = Integer.parseInt(getPreference("network_size"));
                            TcpConnection conexion = new TcpConnection(ip, port, 15);
                            if (size == 58) {
                                linea = "[L]--------------------------------\n";
                                printer = new EscPosPrinter(conexion, 203, 48f, 32, new EscPosCharsetEncoding(encode, 6));
                            } else if (size == 80) {
                                linea = "[L]------------------------------------------------\n";
                                printer = new EscPosPrinter(conexion, 203, 72f, 48, new EscPosCharsetEncoding(encode, 6));
                            }
                        }

                        // Parsear el JSON
                        JSONObject jsonObject = new JSONObject(data);

                        // Obtener los datos del emisor, cliente, venta y ley
                        JSONObject emisor = jsonObject.getJSONObject("emisor");
                        JSONObject cliente = jsonObject.getJSONObject("cliente");
                        JSONObject venta = jsonObject.getJSONObject("venta");
                        JSONArray dtVenta = jsonObject.getJSONArray("dt_venta");
                        String ley = jsonObject.getString("ley");
                        String contenidoQR = jsonObject.getString("contenido_qr");

                        String url = emisor.getString("img");
                        Bitmap imagen = downloadImage(url);
                        // Convertir el bitmap a hexadecimal y enviar a imprimir
                        assert printer != null;
                        StringBuilder printText = new StringBuilder();
                        // Agregar la imagen al principio del ticket
                        printText.append("[C]<img>").append(PrinterTextParserImg.bitmapToHexadecimalString(printer, imagen)).append("</img>\n");
                        printText.append("[L]\n");
                        printText.append("[C]<b>").append(emisor.getString("nombre_comercial")).append("</b>\n");
                        printText.append("[C]").append(emisor.getString("direccion")).append("\n");
                        printText.append("[C]CEL: ").append(emisor.getString("celular")).append("\n");
                        printText.append("[C]EMAIL: ").append(emisor.getString("correo")).append("\n");
                        printText.append(linea);
                        printText.append("[C]<b>RUC: ").append(emisor.getString("documento")).append("</b>\n");
                        printText.append("[C]<b>").append(venta.getString("tipo_comprobate")).append("</b>\n");
                        printText.append("[C]<b>").append(venta.getString("serie")).append("</b>\n");
                        if (venta.has("modulo") && !venta.getString("modulo").equals("null") && !venta.getString("modulo").isEmpty()) {
                            printText.append("[C]<b>").append(venta.getString("modulo")).append("</b>\n");
                        }
                        printText.append(linea);
                        printText.append("[L]<b>DOCUMENTO: </b>").append(cliente.getString("documento")).append("\n");
                        printText.append("[L]<b>CLIENTE: </b>").append(cliente.getString("razon_social")).append("\n");
                        printText.append("[L]<b>DIRECCIÓN: </b>").append(cliente.getString("direccion").equals("null") ? "" : cliente.getString("direccion")).append("\n");
                        printText.append("[L]<b>CELULAR: </b>").append(cliente.getString("celular").equals("null") ? "" : cliente.getString("celular")).append("\n");
                        printText.append("[L]<b>OBSERVACIÓN: </b>").append(venta.getString("observacion").equals("null") ? "" : venta.getString("observacion")).append("\n");
                        printText.append("[L]\n");
                        if(size == 58){
                            printText.append("[C]<b>FECHA: </b>").append(venta.getString("fecha_emision")).append(" - ").append(venta.getString("hora")).append("\n");
                        }else{
                            printText.append("[C]<b>FECHA: </b>").append(venta.getString("fecha_emision")).append(" - <b>HORA: </b>").append(venta.getString("hora")).append("\n");
                        }
                        printText.append("[C]<b>MONEDA: </b>").append(venta.getString("moneda")).append(" - <b>F. DE PAGO: </b>").append(venta.getString("forma_pago")).append("\n");
                        printText.append("[L]\n");
                        printText.append("[L]<b>[ CANT. ] DESCRIPCIÓN</b>\n");
                        printText.append("[L]<b>PRECIO [R]IMPORTE</b>\n");
                        printText.append(linea);

                        // Añadir los detalles de los productos
                        for (int i = 0; i < dtVenta.length(); i++) {
                            JSONObject item = dtVenta.getJSONObject(i);
                            String cantidad = item.getString("cantidad");
                            String descripcion = item.getString("descripcion");
                            String precio = item.getString("precio");
                            String importe = item.getString("importe");
                            // Combinar cantidad y descripción
                            printText.append("[L][").append(cantidad).append("] ").append(descripcion).append("\n");
                            printText.append("[L]").append(precio).append("[R]").append(importe).append("\n");
                            printText.append(linea);
                        }
                        printText.append("[L]GRAVADA S/: ").append("[R]").append(venta.getString("op_gravada")).append("\n");
                        printText.append("[L]INAFECTA S/: ").append("[R]").append(venta.getString("op_inafecta")).append("\n");
                        printText.append("[L]IGV S/: ").append("[R]").append(venta.getString("igv")).append("\n");
                        printText.append("[L]<b>IMPORTE TOTAL S/: ").append("[R]").append(venta.getString("total")).append("</b>\n");
                        printText.append("[L]<b>SON:</b>").append(venta.getString("total_texto")).append("\n");
                        printText.append("[L]\n");

                        if (jsonObject.has("detraccion")) {
                            JSONObject detraccion = jsonObject.getJSONObject("detraccion");
                            printText.append("[L]<b>INFORMACIÓN DE LA DETRACCIÓN</b>\n");
                            printText.append("[L]Leyenda: ").append("[R]Operación sujeta a detracción\n");
                            printText.append("[L]Servicio: ").append("[R]").append(detraccion.getString("bien")).append("\n");
                            printText.append("[L]Medio Pago: ").append("[R]").append(detraccion.getString("medio_pago")).append("\n");
                            printText.append("[L]Nro. Cta. detracción: ").append("[R]").append(detraccion.getString("cuenta_detraccion")).append("\n");
                            printText.append("[L]% de detracción: ").append("[R]").append(detraccion.getString("porcentaje_detraccion")).append("\n");
                            printText.append("[L]Monto detracción:  ").append("[R]S/ ").append(detraccion.getString("monto_detraccion")).append("\n");
                            printText.append("[L]\n");
                        }
                        printText.append("[C]<qrcode size='20'>").append(contenidoQR).append("</qrcode>\n");
                        printText.append("[C]").append(ley).append("\n");
                        printText.append("[C]- GRACIAS POR SU COMPRA - \n");

                        printer.printFormattedTextAndCut(printText.toString());

                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Se imprimió el ticket", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Error al imprimir el ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Error obtener datos", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void mandarImprimirCotizacion(String data) {
        final Context context = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Cargar el bitmap desde el archivo PNG
                if (data != null && isPrinterConfigured()) {
                    String printerType = getPreference("printer_type");
                    EscPosPrinter printer = null;
                    int size = 0;
                    String linea = "";

                    try {
                        if ("bluetooth".equals(printerType)) {
                            size = Integer.parseInt(getPreference("bluetooth_size"));
                            String encode = getPreference("bluetooth_encode");
                            if (size == 58) {
                                printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32, new EscPosCharsetEncoding(encode, 6));
                                linea = "[L]--------------------------------\n";
                            } else if (size == 80) {
                                printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 72f, 48, new EscPosCharsetEncoding(encode, 6));
                                linea = "[L]------------------------------------------------\n";
                            }
                        } else if ("network".equals(printerType)) {
                            String ip = getPreference("network_ip");
                            String encode = getPreference("network_encode");
                            int port = Integer.parseInt(getPreference("network_port"));
                            size = Integer.parseInt(getPreference("network_size"));
                            TcpConnection conexion = new TcpConnection(ip, port, 15);
                            if (size == 58) {
                                linea = "[L]--------------------------------\n";
                                printer = new EscPosPrinter(conexion, 203, 48f, 32, new EscPosCharsetEncoding(encode, 6));
                            } else if (size == 80) {
                                linea = "[L]------------------------------------------------\n";
                                printer = new EscPosPrinter(conexion, 203, 72f, 48, new EscPosCharsetEncoding(encode, 6));
                            }
                        }

                        // Parsear el JSON
                        JSONObject jsonObject = new JSONObject(data);

                        // Obtener los datos del emisor, cliente, venta y ley
                        JSONObject emisor = jsonObject.getJSONObject("emisor");
                        JSONObject cliente = jsonObject.getJSONObject("cliente");
                        JSONObject cotizacion = jsonObject.getJSONObject("cotizacion");
                        JSONArray dt_cotizacion = jsonObject.getJSONArray("dt_cotizacion");
                        String ley = jsonObject.getString("ley");
                        String contenidoQR = jsonObject.getString("contenido_qr");

                        String url = cliente.getString("img");
                        Bitmap imagen = downloadImage(url);
                        // Convertir el bitmap a hexadecimal y enviar a imprimir
                        assert printer != null;

                        StringBuilder printText = new StringBuilder();
                        // Agregar la imagen al principio del ticket
                        printText.append("[C]<img>").append(PrinterTextParserImg.bitmapToHexadecimalString(printer, imagen)).append("</img>\n");
                        printText.append("[L]\n");
                        printText.append("[C]<b>").append(emisor.getString("nombre_comercial")).append("</b>\n");
                        printText.append("[C]").append(emisor.getString("direccion")).append("\n");
                        printText.append("[C]CEL: ").append(emisor.getString("celular")).append("\n");
                        printText.append("[C]EMAIL: ").append(emisor.getString("correo")).append("\n");
                        printText.append(linea);
                        printText.append("[C]<b>RUC: ").append(emisor.getString("documento")).append("</b>\n");
                        printText.append("[C]<b>").append(cotizacion.getString("tipo_comprobate")).append("</b>\n");
                        printText.append("[C]<b>").append(cotizacion.getString("serie")).append("</b>\n");

                        printText.append(linea);
                        printText.append("[L]<b>DOCUMENTO: </b>").append(cliente.getString("documento")).append("\n");
                        printText.append("[L]<b>CLIENTE: </b>").append(cliente.getString("razon_social")).append("\n");
                        printText.append("[L]<b>DIRECCIÓN: </b>").append(cliente.getString("direccion").equals("null") ? "" : cliente.getString("direccion")).append("\n");
                        printText.append("[L]<b>CELULAR: </b>").append(cliente.getString("celular").equals("null") ? "" : cliente.getString("celular")).append("\n");
                        printText.append("[L]<b>OBSERVACIÓN: </b>").append(cotizacion.getString("observacion").equals("null") ? "" : cotizacion.getString("observacion")).append("\n");
                        printText.append("[L]<b>MONEDA: </b>").append(cotizacion.getString("moneda")).append("\n");
                        printText.append("[L]\n");
                        if(size == 58){
                            printText.append("[C]<b>FECHA: </b>").append(cotizacion.getString("fecha")).append(" - ").append(cotizacion.getString("hora")).append("\n");
                        }else{
                            printText.append("[C]<b>FECHA: </b>").append(cotizacion.getString("fecha")).append(" - <b>HORA: </b>").append(cotizacion.getString("hora")).append("\n");
                        }
                        printText.append("[L]\n");
                        printText.append("[L]<b>[ CANT. ] DESCRIPCIÓN</b>\n");
                        printText.append("[L]<b>PRECIO [R]IMPORTE</b>\n");
                        printText.append(linea);

                        // Añadir los detalles de los productos
                        for (int i = 0; i < dt_cotizacion.length(); i++) {
                            JSONObject item = dt_cotizacion.getJSONObject(i);
                            String cantidad = item.getString("cantidad");
                            String descripcion = item.getString("descripcion");
                            String precio = item.getString("precio");
                            String importe = item.getString("importe");
                            // Combinar cantidad y descripción
                            printText.append("[L][").append(cantidad).append("] ").append(descripcion).append("\n");
                            printText.append("[L]").append(precio).append("[R]").append(importe).append("\n");
                            printText.append(linea);
                        }
                        printText.append("[L]GRAVADA S/: ").append("[R]").append(cotizacion.getString("op_gravada")).append("\n");
                        printText.append("[L]INAFECTA S/: ").append("[R]").append(cotizacion.getString("op_inafecta")).append("\n");
                        printText.append("[L]IGV S/: ").append("[R]").append(cotizacion.getString("igv")).append("\n");
                        printText.append("[L]<b>IMPORTE TOTAL S/: ").append("[R]").append(cotizacion.getString("total")).append("</b>\n");
                        printText.append("[L]<b>SON:</b>").append(cotizacion.getString("total_texto")).append("\n");
                        printText.append("[L]\n");
                        printText.append("[C]<qrcode size='20'>").append(contenidoQR).append("</qrcode>\n");
                        printText.append("[L]\n");
                        printText.append("[C]").append(ley).append("\n");

                        printer.printFormattedTextAndCut(printText.toString());

                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Se imprimió el ticket", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Error al imprimir el ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Error obtener datos", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void mandarImprimirGuia(String data) {
        final Context context = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Cargar el bitmap desde el archivo PNG
                if (data != null && isPrinterConfigured()) {
                    String printerType = getPreference("printer_type");
                    EscPosPrinter printer = null;
                    int size = 0;
                    String linea = "";

                    try {
                        if ("bluetooth".equals(printerType)) {
                            size = Integer.parseInt(getPreference("bluetooth_size"));
                            String encode = getPreference("bluetooth_encode");
                            if (size == 58) {
                                printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32, new EscPosCharsetEncoding(encode, 6));
                                linea = "[L]--------------------------------\n";
                            } else if (size == 80) {
                                printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 72f, 48, new EscPosCharsetEncoding(encode, 6));
                                linea = "[L]------------------------------------------------\n";
                            }
                        } else if ("network".equals(printerType)) {
                            String ip = getPreference("network_ip");
                            String encode = getPreference("network_encode");
                            int port = Integer.parseInt(getPreference("network_port"));
                            size = Integer.parseInt(getPreference("network_size"));
                            TcpConnection conexion = new TcpConnection(ip, port, 15);
                            if (size == 58) {
                                linea = "[L]--------------------------------\n";
                                printer = new EscPosPrinter(conexion, 203, 48f, 32, new EscPosCharsetEncoding(encode, 6));
                            } else if (size == 80) {
                                linea = "[L]------------------------------------------------\n";
                                printer = new EscPosPrinter(conexion, 203, 72f, 48, new EscPosCharsetEncoding(encode, 6));
                            }
                        }

                        // Parsear el JSON
                        JSONObject jsonObject = new JSONObject(data);

                        // Obtener los datos del emisor, cliente, venta y ley
                        JSONObject emisor = jsonObject.getJSONObject("emisor");
                        JSONObject destinatario = jsonObject.getJSONObject("destinatario");
                        JSONObject guia = jsonObject.getJSONObject("guia");
                        JSONObject partida = guia.getJSONObject("punto_partida");
                        JSONObject llegada = guia.getJSONObject("punto_llegada");
                        JSONObject transporte = jsonObject.getJSONObject("transporte");
                        JSONArray detalle = jsonObject.getJSONArray("dt_guia");
                        String ley = jsonObject.getString("ley");
                        String contenidoQR = jsonObject.getString("contenido_qr");

                        String url = emisor.getString("img");
                        Bitmap imagen = downloadImage(url);
                        // Convertir el bitmap a hexadecimal y enviar a imprimir
                        assert printer != null;
                        StringBuilder printText = new StringBuilder();
                        // Agregar la imagen al principio del ticket
                        printText.append("[C]<img>").append(PrinterTextParserImg.bitmapToHexadecimalString(printer, imagen)).append("</img>\n");
                        printText.append("[L]\n");
                        printText.append("[C]<b>").append(emisor.getString("nombre_comercial")).append("</b>\n");
                        printText.append("[C]").append(emisor.getString("direccion")).append("\n");
                        printText.append("[C]CEL: ").append(emisor.getString("celular")).append("\n");
                        printText.append("[C]EMAIL: ").append(emisor.getString("correo")).append("\n");
                        printText.append(linea);
                        printText.append("[C]<b>RUC: ").append(emisor.getString("documento")).append("</b>\n");
                        printText.append("[C]<b>").append(guia.getString("tipo_comprobate")).append("</b>\n");
                        printText.append("[C]<b>").append(guia.getString("serie")).append("</b>\n");
                        printText.append(linea);
                        printText.append("[C]<b>").append("DATOS DEL TRASLADO").append("</b>\n");
                        printText.append("[L]<b>FECHA EMISIÓN: </b>").append(guia.getString("fecha_emision")).append("\n");
                        printText.append("[L]<b>FECHA DE TRASLADO: </b>").append(guia.getString("fecha_traslado")).append("\n");

                        if(guia.has("motivo_traslado")){
                            printText.append("[L]<b>MOT. DE TRASLADO: </b>").append(guia.getString("motivo_traslado")).append("\n");
                        }
                        if(guia.has("modalidad_transporte")){
                            printText.append("[L]<b>MOD. TRANSPORTE: </b>").append(guia.getString("modalidad_transporte")).append("\n");
                        }
                        printText.append("[L]<b>PESO TOTAL: </b>").append(guia.getString("peso_bruto")).append("\n");
                        printText.append("[L]<b>INDICADOR DE ENVIO SUNAT: </b>").append(guia.getString("indicador")).append("\n");
                        if(!guia.getString("observacion").equals("null")) {
                            printText.append("[L]<b>OBSERVACIÓN: </b>").append(guia.getString("observacion")).append("\n");
                        }
                        printText.append(linea);

                        printText.append("[C]<b>").append("DATOS DEL DESTINATARIO").append("</b>\n");
                        printText.append("[L]<b>DOCUMENTO: </b>").append(destinatario.getString("documento")).append("\n");
                        printText.append("[L]<b>RAZON SOCIAL: </b>").append(destinatario.getString("razon_social")).append("\n");
                        printText.append(linea);

                        printText.append("[C]<b>").append("PUNTO DE PARTIDA").append("</b>\n");
                        printText.append("[C]").append(partida.getString("direccion")).append("\n");
                        printText.append("[C]").append(partida.getString("ubigeo")).append("\n");
                        printText.append("[C]").append(partida.getString("referencia").equals("null") ? "" : partida.getString("referencia")).append("\n");
                        printText.append("[C]<b>").append("PUNTO DE LLEGADA").append("</b>\n");
                        printText.append("[C]").append(llegada.getString("direccion")).append("\n");
                        printText.append("[C]").append(llegada.getString("ubigeo")).append("\n");
                        printText.append("[C]").append(llegada.getString("referencia").equals("null") ? "" : llegada.getString("referencia")).append("\n");
                        printText.append(linea);

                        if(transporte.has("placas")){
                            JSONArray placas = transporte.getJSONArray("placas");
                            printText.append("[C]<b>").append("DATOS DEL TRANSPORTE").append("</b>\n");
                            printText.append("[L]<b>DOC. CONDUCTOR: </b>").append(transporte.getString("documento")).append("\n");
                            printText.append("[L]<b>NOMB. Y APELL. CONDUCTOR: </b>").append(transporte.getString("razon_social")).append("\n");
                            printText.append("[L]<b>LIC. CON. CONDUCTOR: </b>").append(transporte.getString("licencia")).append("\n");

                            for (int p = 0; p < placas.length(); p++) {
                                JSONObject placa = placas.getJSONObject(p);
                                // Combinar cantidad y descripción
                                printText.append("[L]<b>PLACA VEH. </b>").append(placa.getString("vehiculo").toUpperCase()).append(": ").append(placa.getString("numero")).append("\n");
                            }
                        }else{
                            printText.append("[C]<b>").append("DATOS DEL TRANSPORTISTA").append("</b>\n");
                            printText.append("[L]<b>RUC: </b>").append(transporte.getString("documento")).append("\n");
                            printText.append("[L]<b>RAZON SOCIAL: </b>").append(transporte.getString("razon_social")).append("\n");
                            if(!transporte.getString("registro_mtc").equals("null")) {
                                printText.append("[L]<b>REG. MTC: </b>").append(transporte.getString("registro_mtc")).append("\n");
                            }
                        }

                        printText.append("[L]\n");

                        printText.append("[L]<b>DESCRIPCIÓN [R]CANT.</b>\n");
                        printText.append(linea);

                        // Añadir los detalles de los productos
                        for (int i = 0; i < detalle.length(); i++) {
                            JSONObject item = detalle.getJSONObject(i);
                            String cantidad = item.getString("cantidad");
                            String descripcion = item.getString("descripcion");
                            // Combinar cantidad y descripción
                            //printText.append("[L]").append(descripcion).append("[R]").append(cantidad).append("\n");
                            printText.append("[L]").append(descripcion).append("  ").append(cantidad).append("\n");
                            printText.append(linea);
                        }

                        printText.append("[C]<qrcode size='20'>").append(contenidoQR).append("</qrcode>\n");
                        printText.append("[C]").append(ley).append("\n");
                        printText.append("[C]- GRACIAS POR SU COMPRA - \n");

                        printer.printFormattedTextAndCut(printText.toString());

                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Se imprimió la guia", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Error al imprimir la guia: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Error obtener datos", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    //con imagen
    private Bitmap downloadImage(String imageUrl) {
        final Bitmap[] bitmapHolder = new Bitmap[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error de red: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                latch.countDown(); // Liberar el latch en caso de fallo
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Crear un archivo temporal para almacenar la imagen
                    File imageFile = new File(getExternalFilesDir(null), "logo.png");

                    try (InputStream inputStream = response.body().byteStream();
                         FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                        byte[] buffer = new byte[1024];
                        int len;

                        while ((len = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, len);
                        }

                        // Convertir la imagen a Bitmap
                        Bitmap originalBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                        if (originalBitmap == null) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Error en la url de la imagen", Toast.LENGTH_SHORT).show();
                            });
                            latch.countDown();
                            return;
                        }

                        // Redimensionar el Bitmap
                        bitmapHolder[0] = Bitmap.createScaledBitmap(originalBitmap, 256,
                                Math.round(originalBitmap.getHeight() * (256.0f / originalBitmap.getWidth())),
                                true);
                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Error al guardar la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Error en la descarga: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
                latch.countDown(); // Liberar el latch después de la respuesta
            }
        });

        // Esperar a que la descarga termine
        try {
            latch.await(); // Espera a que el latch cuente hasta 0
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return bitmapHolder[0]; // Retornar el bitmap
    }

    //para abrir las tabs
    public void abrirEnCustomTab(Context context, String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }

    public void descargarYAbrirArchivo(Context context, String url) {

        // Mostrar un ProgressDialog
        int verdeColor = ContextCompat.getColor(context, R.color.verde);
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);

        // Cambiar color del texto a blanco
        SpannableString message = new SpannableString("Descargando archivo...");
        message.setSpan(new ForegroundColorSpan(Color.WHITE), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        progressDialog.setMessage(message);

        // Cambiar el fondo del ProgressDialog a verde
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(verdeColor));

        progressDialog.show();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", webViewCookies) // 🔥 Pasamos la cookie aquí
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DESCARGA", "Error en la descarga", e);

                // Cerrar ProgressDialog y mostrar error
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Error al descargar el archivo", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String fileName = url.substring(url.lastIndexOf("/") + 1);
                    Uri fileUri = null;
                    File file = null;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                        values.put(MediaStore.Downloads.MIME_TYPE, response.body().contentType().toString());
                        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                        ContentResolver resolver = context.getContentResolver();
                        fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                        if (fileUri != null) {
                            try (OutputStream outputStream = resolver.openOutputStream(fileUri)) {
                                outputStream.write(response.body().bytes());
                            }
                        }
                    } else {
                        file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(response.body().bytes());
                        }
                        fileUri = Uri.fromFile(file);
                    }

                    if (fileUri != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            progressDialog.dismiss(); // Cerrar ProgressDialog
                            Toast.makeText(context, "Descarga completada", Toast.LENGTH_SHORT).show();
                        });

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(fileUri);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        String mimeType = response.body().contentType().toString();


                        intent.setDataAndType(fileUri, mimeType);

                        try {
                            context.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Log.e("DESCARGA", "No hay aplicación para abrir el archivo");
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(context, "No hay aplicación para abrir el archivo", Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                }
            }
        });
    }


}