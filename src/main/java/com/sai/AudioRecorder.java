package com.sai;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AudioRecorder {
    public static final String recordingFile = "src/main/resources/RecordAudio.wav";
    public static final String binaryFileRecord = "src/main/resources/BinaryRecord.txt";
    public static final String decodedRecordings = "src/main/resources/Decoded.wav";
    public static final String editedRecordings = "src/main/resources/Edited.wav";
    static AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    static TargetDataLine line;
    Thread capturing;
    private Clip clip = null;
    public Boolean isPlaying = false;
    public Mixer.Info device = null;
    public static short[] audioData = null;

    static AudioFormat getAudioFormat() {
        float sampleRate = 48000.0f;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);
    }

    public void start() {
        capturing = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioFormat format = getAudioFormat();

                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                    if (!AudioSystem.isLineSupported(info)) {
                        System.out.println("Line not supported");
                        System.exit(0);
                    }
                    line = (TargetDataLine) AudioSystem.getMixer(device).getLine(info);

                    line.open(format);
                    line.start();

                    System.out.println("Start capturing...");
                    AudioInputStream ais = new AudioInputStream(line);

                    System.out.println("Start recording...");
                    AudioSystem.write(ais, fileType, new File(recordingFile));

                } catch (LineUnavailableException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        capturing.start();
    }

    public void finish() {
        line.stop();
        line.close();
        capturing.interrupt();
        System.out.println("Finished recording");
    }

    public void selectDevice() {
        System.out.println(selectInputDevices());
    }

    public void encodeAudio() {
        if (!new File(recordingFile).exists()) {
            return;
        }
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(recordingFile));

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            int totalSamples = (int) (audioInputStream.getFrameLength() * getAudioFormat().getChannels());

            short[] tempAudioData = new short[totalSamples];
            int currentIndex = 0;

            while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i += (getAudioFormat().getSampleSizeInBits() / 8) * getAudioFormat().getChannels()) {
                    short sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));

                    tempAudioData[currentIndex] = sample;
                    currentIndex++;
                }
            }

            // Create a StringBuilder to construct the integer array representation
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < tempAudioData.length; i++) {
                sb.append(tempAudioData[i]);
                if (i < tempAudioData.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");

            // Write the integer array representation to the text file
            FileWriter writer = new FileWriter(binaryFileRecord);
            writer.write(sb.toString());
            writer.close();  // Close the FileWriter when done.

            audioData = tempAudioData;

            System.out.println("AudioFile finished encoding");
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void decodeAudio() {
        if (!new File(binaryFileRecord).exists()) {
            return;
        }

        try {
            // Read the text data
            String encodedAudio = new String(Files.readAllBytes(Paths.get(binaryFileRecord)));

            // Remove square brackets, split, and parse values
            String[] valueStrings = encodedAudio
                    .replace("[", "")
                    .replace("]", "")
                    .split(",\\s*"); // Split by comma and optional space

            // Convert the string values back to shorts
            short[] tempAudioData = new short[valueStrings.length];
            for (int i = 0; i < valueStrings.length; i++) {
                tempAudioData[i] = Short.parseShort(valueStrings[i]);
            }

            // Create an audio input stream from the decoded audio data
            AudioFormat audioFormat = getAudioFormat(); // Ensure it matches the format used during encoding
            byte[] audioBytes = new byte[tempAudioData.length * (audioFormat.getSampleSizeInBits() / 8)];

            // Convert shorts to bytes
            for (int i = 0; i < tempAudioData.length; i++) {
                audioBytes[i * 2 + 1] = (byte) (tempAudioData[i] >> 8);
                audioBytes[i * 2] = (byte) (tempAudioData[i] & 0xFF);
            }

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);
            AudioInputStream audioInputStream = new AudioInputStream(
                    byteArrayInputStream,
                    audioFormat,
                    tempAudioData.length / audioFormat.getChannels()
            );


            // Write the audio data to a WAV file
            File outputFile = new File(decodedRecordings);
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);

            System.out.println("AudioFile finished decoding");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveEditedAudio() throws IOException {
        byte[] byteBuffer = new byte[audioData.length * 2]; // 2 bytes per short

        int byteIndex = 0;
        for (short sample : audioData) {
            byteBuffer[byteIndex + 1] = (byte) (sample >> 8); // higher byte
            byteBuffer[byteIndex] = (byte) (sample & 0xFF); // lower byte
            byteIndex += 2;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
        AudioInputStream ais = new AudioInputStream(bais, getAudioFormat(), audioData.length);

        File outFile = new File(editedRecordings);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outFile);
        System.out.println("AudioFile finished saving");
    }

    public static short[] resample(short[] input, double scale) {
        int outputSize = (int) (input.length / scale);
        short[] output = new short[outputSize];

        for (int i = 0; i < outputSize - 1; i++) {
            double inputIndex = i * scale;
            int indexBefore = (int) Math.floor(inputIndex);
            int indexAfter = indexBefore + 1;

            // Boundary check
            if (indexAfter >= input.length) {
                indexAfter = input.length - 1;
            }

            double distance = inputIndex - indexBefore;

            // Linear interpolation
            double interpolatedValue = input[indexBefore] + distance * (input[indexAfter] - input[indexBefore]);
            output[i] = (short) interpolatedValue;
        }
        audioData = output;
        return output;
    }

    public void play(String file) {
        File playFile = switch (file) {
            case "Recorded" -> new File(recordingFile);
            case "Edited" -> new File(editedRecordings);
            case "From text" -> new File(decodedRecordings);
            default -> new File(recordingFile);
        };
        if (!playFile.exists()) {
            JOptionPane.showMessageDialog(null, "There is no Audio recorded", "Error", 1);
        } else {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(playFile);
                if (!isPlaying) {
                    clip = AudioSystem.getClip();
                    clip.open(audioInputStream);
                    clip.start();
                    isPlaying = true;
                } else {
                    pause();
                }

            } catch (UnsupportedAudioFileException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (LineUnavailableException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void pause() {
        if (clip != null) {
            clip.close();
            isPlaying = false;
        }
    }

    public Mixer.Info[] selectInputDevices() {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        java.util.List<Mixer.Info> eligibleDevices = new java.util.ArrayList<>();

        for (Mixer.Info info : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
            for (Line.Info targetLineInfo : targetLineInfos) {
                if (targetLineInfo.getLineClass().equals(TargetDataLine.class)) {
                    // Check if this mixer supports the desired audio format
                    if (mixer.isLineSupported(new DataLine.Info(TargetDataLine.class, getAudioFormat()))) {
                        eligibleDevices.add(info);
                        break;
                    }
                }
            }
        }

        return eligibleDevices.toArray(new Mixer.Info[0]);
    }

}