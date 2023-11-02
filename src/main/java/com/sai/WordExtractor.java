package com.sai;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WordExtractor {

    private static final int AMPLITUDE_THRESHOLD = 400;  // Adjust as per your requirement
    private static final int MIN_WORD_DURATION = 20000;   // Assuming 1 second duration for a word at 44.1kHz sampling rate

    public List<short[]> extractWordsFromAudio(String inputFile, String outputFolder, boolean isFile) throws Exception {
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(inputFile));
        AudioFormat format = audioStream.getFormat();

        byte[] audioBytes = audioStream.readAllBytes();
        short[] audioData = new short[audioBytes.length / 2];

        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (short) ((audioBytes[2 * i] & 0xFF) | (audioBytes[2 * i + 1] << 8));  // Adjust for endianness
        }

        boolean inWord = false;
        int wordStart = 0;
        int count = 0;
        int belowThresholdCounter = 0;
        int MAX_SAMPLES_BELOW_THRESHOLD = 2000;  // Adjust as required; it determines how long a quiet part can be before the word is considered ended
        List<short[]> segments = new ArrayList<>();

        for (int i = 0; i < audioData.length; i++) {
            if (Math.abs(audioData[i]) > AMPLITUDE_THRESHOLD) {
                if (!inWord) {
                    inWord = true;
                    wordStart = i;
                }
                belowThresholdCounter = 0; // Reset the counter as we have a sample above threshold
            } else if (inWord) {
                belowThresholdCounter++;
                if (belowThresholdCounter > MAX_SAMPLES_BELOW_THRESHOLD) {
                    if (i - belowThresholdCounter - wordStart > MIN_WORD_DURATION) {
                        count++;
                        if (isFile) {
                            saveWord(audioData, wordStart, i - belowThresholdCounter, format, outputFolder + "/word_" + (count) + ".wav");
                        }
                        short[] segmentData = new short[i - wordStart];
                        System.arraycopy(audioData, wordStart, segmentData, 0, segmentData.length);
                        segments.add(segmentData);
                    }
                    inWord = false;  // End the word
                    belowThresholdCounter = 0;  // Reset the counter
                }
            }
        }
        System.out.println("Extracted " + count + " words.");
        return segments;
    }

    private void saveWord(short[] audioData, int start, int end, AudioFormat format, String outputFile) throws Exception {
        byte[] byteArray = new byte[(end - start) * 2];
        for (int i = 0, j = start; j < end; i += 2, j++) {
            byteArray[i] = (byte) (audioData[j] & 0xFF);
            byteArray[i + 1] = (byte) ((audioData[j] >> 8) & 0xFF);
        }

        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteArray);
        AudioInputStream audioStream = new AudioInputStream(byteInputStream, format, byteArray.length / format.getFrameSize());
        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, new File(outputFile));
    }
}
