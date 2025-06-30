# Viseme Lip-Sync Animation

A lightweight Android application that demonstrates real-time lip-sync animation based on microphone input. The app analyzes audio amplitude to animate a mouth shape, creating a simple but effective lip-sync effect.

## Features

- Real-time audio processing from the device's microphone
- Smooth animation between different mouth shapes based on audio amplitude
- Low CPU and memory usage
- Works offline - no internet connection required
- Dark/Light theme support
- Debug mode with amplitude visualization

## Requirements

- Android 8.0 (API level 26) or higher
- Microphone permission
- Android Studio Giraffe or later (for development)

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run the app on a device or emulator

## How It Works

The app uses Android's `MediaRecorder` to capture audio input from the microphone. It then:

1. Measures the audio amplitude in real-time
2. Applies smoothing to the amplitude values for more natural animation
3. Maps the amplitude to one of five mouth states (closed to fully open)
4. Updates the UI with the appropriate mouth shape
5. Applies subtle scaling animations for more dynamic movement

## Customization

### Changing Mouth Shapes

Replace the following drawable resources in `res/drawable/`:
- `o.png` - Closed mouth
- `l.png` - Slightly open
- `ae.png` - Half open
- `qw.png` - Mostly open
- `bmp.png` - Fully open

### Adjusting Sensitivity

In `MainActivity.kt`, modify the amplitude thresholds in the `updateMouthState` method:

```kotlin
val state = when {
    smoothedAmplitude < 0.2 -> 0  // Closed
    smoothedAmplitude < 0.4 -> 1  // Slightly open
    smoothedAmplitude < 0.6 -> 2  // Half open
    smoothedAmplitude < 0.8 -> 3  // Mostly open
    else -> 4                     // Fully open
}
```

## Permissions

The app requires the following permissions:
- `RECORD_AUDIO` - To capture audio input from the microphone
- `FOREGROUND_SERVICE` - For potential future background processing

## Dependencies

- AndroidX Core KTX
- AndroidX AppCompat
- Material Design Components
- AndroidX ConstraintLayout
- AndroidX Activity KTX
- AndroidX Media

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
