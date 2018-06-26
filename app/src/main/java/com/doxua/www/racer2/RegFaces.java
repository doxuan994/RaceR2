package com.doxua.www.racer2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_face;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.io.IOException;

import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.opencv.core.Core.LINE_8;
import static org.bytedeco.javacpp.opencv_core.Rect;
import static org.bytedeco.javacpp.opencv_core.RectVector;
import static org.bytedeco.javacpp.opencv_core.Size;
import static org.bytedeco.javacpp.opencv_core.Point;
import static org.bytedeco.javacpp.opencv_face.EigenFaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.Scalar;


public class RegFaces extends AppCompatActivity {

    private static final int ACCEPT_LEVEL = 1000;
    private static final int PICK_IMAGE = 100;
    private static final int IMG_SIZE = 160;

    // Views.
    private ImageView imageView;
    private TextView tv;

    // Face Detection.
    private CascadeClassifier faceDetector;
    private int absoluteFaceSize = 0;

    // Face Recognition.
    private FaceRecognizer faceRecognizer = EigenFaceRecognizer.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regfaces);

        // Create the image view and text view.
        imageView = (ImageView) findViewById(R.id.imageView);
        tv = (TextView) findViewById(R.id.predict_faces);

        // Pick an image and recognize.
        Button pickImageButton = (Button) findViewById(R.id.btnGallery);
        pickImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
    }

    private void openGallery() {
        Intent gallery =
                new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();

            // Convert to Bitmap.
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            detectDisplayAndRecognize(bitmap);
        }
    }

    /**
     * Face Detection.
     * Face Recognition.
     * Display the detection result and recognition result.
     * @param bitmap
     */
    void detectDisplayAndRecognize(Bitmap bitmap) {

        // Create a new gray Mat.
        Mat greyMat = new Mat();
        // JavaCV frame converters.
        AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

        // -------------------------------------------------------------------
        //                    Convert to mat for processing
        // -------------------------------------------------------------------
        // Convert to Bitmap.
        Frame frame = converterToBitmap.convert(bitmap);
        // Convert to Mat.
        Mat colorMat = converterToMat.convert(frame);


        // Convert to Gray scale.
        cvtColor(colorMat, greyMat, CV_BGR2GRAY);
        // Vector of rectangles where each rectangle contains the detected object.
        RectVector faces = new RectVector();


        // -----------------------------------------------------------------------------------------
        //                                  FACE DETECTION
        // -----------------------------------------------------------------------------------------
        // Load the CascadeClassifier class to detect objects.
        faceDetector = TrainFaces.loadClassifierCascade(RegFaces.this, R.raw.frontalface);
        // Detect the face.
        faceDetector.detectMultiScale(greyMat, faces, 1.25f, 3, 1,
                new Size(absoluteFaceSize, absoluteFaceSize),
                new Size(4 * absoluteFaceSize, 4 * absoluteFaceSize));


        // Count number of faces and display in text view.
        int numFaces = (int) faces.size();

        // -----------------------------------------------------------------------------------------
        //                                      DISPLAY
        // -----------------------------------------------------------------------------------------
        if ( numFaces > 0 ) {
            // Multiple face detection.
            for (int i = 0; i < numFaces; i++) {

                int x = faces.get(i).x();
                int y = faces.get(i).y();
                int w = faces.get(i).width();
                int h = faces.get(i).height();

                rectangle(colorMat, new Point(x, y), new Point(x + w, y + h), Scalar.GREEN, 2, LINE_8, 0);

                // -------------------------------------------------------------------
                //              Convert back to bitmap for displaying
                // -------------------------------------------------------------------
                // Convert processed Mat back to a Frame
                frame = converterToMat.convert(colorMat);
                // Copy the data to a Bitmap for display or something
                Bitmap bm = converterToBitmap.convert(frame);

                // Display the picked image.
                imageView.setImageBitmap(bm);
            }
        } else {
            imageView.setImageBitmap(bitmap);
        }

        // -----------------------------------------------------------------------------------------
        //                                  FACE RECOGNITION
        // -----------------------------------------------------------------------------------------
        recognize(faces.get(0), greyMat, tv);

    }

    /**
     * Predict whether the choosing image is matching or not.
     * IMPORTANT.
     * @param dadosFace
     * @param greyMat
     */
    void recognize(Rect dadosFace, Mat greyMat, TextView tv) {

        // Find the root path.
        String root = Environment.getExternalStorageDirectory().toString();

        // Find the correct root path where our trained face model is stored.
        String personName = "Tom Cruise";
        String photosFolderPath = root + "/saved_images/tom_cruise";
        File photosFolder = new File(photosFolderPath);
        File f = new File(photosFolder, TrainFaces.EIGEN_FACES_CLASSIFIER);

        // Loads a persisted model and state from a given XML or YAML file.
        faceRecognizer.read(f.getAbsolutePath());

        opencv_core.Mat detectedFace = new opencv_core.Mat(greyMat, dadosFace);
        resize(detectedFace, detectedFace, new opencv_core.Size(IMG_SIZE, IMG_SIZE));

        IntPointer label = new IntPointer(1);
        DoublePointer reliability = new DoublePointer(1);
        faceRecognizer.predict(detectedFace, label, reliability);

        // Display on the text view what we found.
        int prediction = label.get(0);
        int acceptanceLevel = (int) reliability.get(0);

        // If a face is not found but we have its model.
        // Read the next model to find the matching.
        if (prediction <= -1 || acceptanceLevel >= ACCEPT_LEVEL) {

            // Find the correct root path where our trained face model is stored.
            personName = "Katie Holmes";
            photosFolderPath = root + "/saved_images/katie_holmes";
            photosFolder = new File(photosFolderPath);
            f = new File(photosFolder, TrainFaces.EIGEN_FACES_CLASSIFIER);

            // Loads a persisted model and state from a given XML or YAML file.
            faceRecognizer.read(f.getAbsolutePath());

            detectedFace = new opencv_core.Mat(greyMat, dadosFace);
            resize(detectedFace, detectedFace, new opencv_core.Size(IMG_SIZE, IMG_SIZE));

            label = new IntPointer(1);
            reliability = new DoublePointer(1);
            faceRecognizer.predict(detectedFace, label, reliability);

            // Display on the text view what we found.
            prediction = label.get(0);
            acceptanceLevel = (int) reliability.get(0);


        }

        // Display the prediction.
        if (prediction <= -1 || acceptanceLevel >= ACCEPT_LEVEL) {
            // Display on text view, not matching or unknown person.
            tv.setText("Unknown ");
        } else {
            // Display the information for the matching image.
            tv.setText("A match is found " + "Hi, " + personName +  " " + acceptanceLevel);
        }

    }
}
