package com.deep.plant;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private static int RESULT_LOAD_IMAGE = 1;
    Button btn_browse, btn_submit;
    Spinner spinner;
    ImageView upload_image;
    String picturePath = "";
    Context context = this;
    String requestURL = "http://www.ajira.online:8091/findDisease";
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        btn_browse = (Button) findViewById(R.id.btn_browse);
        btn_submit = (Button) findViewById(R.id.btn_submit);
        spinner = (Spinner) findViewById(R.id.sp_spinner);
        upload_image = (ImageView) findViewById(R.id.img_upload_image);

        btn_submit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                new WebServiceUpload().execute();
            }
        });

        btn_browse.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });


        List<String> list = new ArrayList<String>();
        list.add("Apple");
        list.add("Corn");
        list.add("Potato");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, list);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // select image from Gallery
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
            bitmap = BitmapFactory.decodeFile(picturePath);
            upload_image.setImageBitmap(bitmap);
        }
    }

    private void showDialog(Result res) {
        if (res == null)
            return;
        String display = res.toString();
        new AlertDialog.Builder(context)
                .setTitle("Result")
                .setMessage(display)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private class WebServiceUpload extends AsyncTask<Void, Void, String> {

        private static final String LINE_FEED = "\r\n";
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);
        private String boundary;
        private HttpURLConnection httpConn;
        private String charset;
        private OutputStream outputStream;
        private PrintWriter writer;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //this method will be running on UI thread
            pdLoading.setMessage("\tPlease wait...");
            pdLoading.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            String result = "";
            try {
                this.charset = "UTF-8";
                // creates a unique boundary based on time stamp
                boundary = "===" + System.currentTimeMillis() + "===";
                URL url = new URL(requestURL);
                httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setUseCaches(false);
                httpConn.setDoOutput(true); // indicates POST method
                httpConn.setDoInput(true);
                httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                outputStream = httpConn.getOutputStream();
                writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                        true);

                addFormField("cropName", spinner.getSelectedItem().toString().replace("\\r\\n", ""));

                addFilePart("upload");

                result = finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }


        /**
         * Completes the request and receives response from the server.
         *
         * @return a list of Strings as response in case the server returned
         * status OK, otherwise an exception is thrown.
         * @throws IOException
         */
        public String finish() throws IOException {
            List<String> response = new ArrayList<String>();

            writer.append(LINE_FEED).flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();

            // checks server's status code first
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpConn.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    response.add(line);
                }
                reader.close();
                httpConn.disconnect();
            } else {
                throw new IOException("Server returned non-OK status: " + status);
            }

            return response.toString().replace("[", "").replace("]", "");
        }

        /**
         * Adds a form field to the request
         *
         * @param name  field name
         * @param value field value
         */
        public void addFormField(String name, String value) {
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=" + charset).append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(value).append(LINE_FEED);
            writer.flush();
        }

        /**
         * Adds a upload file section to the request
         *
         * @param fieldName name attribute in <input type="file" name="..." />
         * @throws IOException
         */
        public void addFilePart(String fieldName)
                throws IOException {

            File uploadFile = new File(picturePath);
            String fileName = uploadFile.getName();
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + fieldName
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();

            writer.append(LINE_FEED);
            writer.flush();
        }

        @Override
        protected void onPostExecute(String result) {
            pdLoading.dismiss();
            System.out.println(result);
            Gson gson = new Gson();
            Result res = gson.fromJson(result, Result.class);
            showDialog(res);
        }
    }

}
