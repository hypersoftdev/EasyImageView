[![](https://jitpack.io/v/hypersoftdev/EasyImageView.svg)](https://jitpack.io/#hypersoftdev/EasyImageView)


# EasyImageView

**EasyImageView** is a customizable Android ImageView designed to simplify adding rounded corners, gradient strokes, selection icons, and more. It allows developers to enhance the visual appeal of images in their applications with minimal setup. EasyImageView can be used directly in XML layouts and is built with configurable attributes, making it an ideal addition to any modern Android UI toolkit.
## Gradle Integration

### Step A: Add Maven Repository

In your project-level **build.gradle** or **settings.gradle** file, add the JitPack repository:
```
repositories {
    google()
    mavenCentral()
    maven { url "https://jitpack.io" }
}
```  

### Step B: Add Dependencies

Include the **EasyImageView** library in your **app-level** `build.gradle` file. Replace `x.x.x` with the latest version: [![](https://jitpack.io/v/hypersoftdev/EasyImageView.svg)](https://jitpack.io/#hypersoftdev/EasyImageView)


Groovy Version
```
 implementation 'com.github.hypersoftdev:EasyImageView:x.x.x'
```
Kts Version
```
 implementation("com.github.hypersoftdev:EasyImageView:x.x.x")
```

## Implementation

### XML Example:

```
  <com.hypersoft.easyimageview.EasyImageView
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:src="@drawable/background"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:scaleType="centerCrop"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        android:layout_marginHorizontal="20dp"
        app:cornerRadius="10dp"
        app:strokeWidth="5dp"
        app:strokeColor="@color/black"
        app:strokeGradientStartColor="@color/green"
        app:strokeGradientEndColor="@color/red"
        app:selectionIcon="@drawable/ic_gallery_unselected"
        app:selectionIconSize="15dp"
        app:selectionIconPosition="top_right"
        app:iconPadding="3dp"
        />

```


## Attribute Summary

| Attribute                  | Format    | Description                         |
|----------------------------|-----------|-------------------------------------|
| `cornerRadius `            | dimension | Set corner radius of the imageview. |
| `strokeWidth`              | dimension | Set width of the stroke.            |
| `strokeColor`              | color     | Set stroke color (solid).           |
| `strokeGradientStartColor` | color     | Set stroke start color              |
| `strokeGradientEndColor`   | color     | Set stroke end color                |
| `strokeGradientAngle`      | float     | Set stroke gradient angle           |
| `selectionIcon `           | reference | Set additional icon over the image. |
| `selectionIconSize`        | dimension | Set overlay icon size.              |
| `selectionIconPosition `   | enum      | Select icon position over image.    |
| `iconPadding `             | dimension | Select icon padding.                |


## Screen Sample

![Screen1](https://github.com/user-attachments/assets/1ddc4201-ea1b-4142-80de-a6f95d32630f)


# Acknowledgements

This work would not have been possible without the invaluable contributions of **Bilal Ahmed**. His expertise, dedication, and unwavering support have been instrumental in bringing this project to fruition.

![Profile](https://github.com/hypersoftdev/ColorPicker/blob/master/screens/profile_image.jpg?raw=true)

We are deeply grateful for **Bilal Ahmed** involvement and his belief in the importance of this work. His contributions have made a significant impact, and we are honored to have had the opportunity to collaborate with him.

# LICENSE

Copyright 2023 Hypersoft Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
