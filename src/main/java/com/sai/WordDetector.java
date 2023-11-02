package com.sai;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WordDetector {

    private final String wordsDirectory;
    private Map<String, short[]> referenceWords;

    public WordDetector(String wordsDirectory) {
        this.wordsDirectory = wordsDirectory;
        this.referenceWords = new HashMap<>();
        loadReferenceWords();
    }

    private void loadReferenceWords() {
        File dir = new File(wordsDirectory);
        File[] files = dir.listFiles((directory, name) -> name.toLowerCase().endsWith(".wav"));

        if (files != null) {
            for (File file : files) {
                try {
                    short[] audioData = extractAudioData(file);
                    referenceWords.put(file.getName(), audioData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String detectWordInRecording(short[] segment) throws Exception {
        double maxCorrelation = -Double.MAX_VALUE;
        String matchedWord = "Unknown";

        for (Map.Entry<String, short[]> entry : referenceWords.entrySet()) {
            String wordName = entry.getKey();
            short[] wordData = entry.getValue();

            double correlation = computeNormalizedCrossCorrelation(segment, wordData);
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation;
                matchedWord = wordName;
            }
            System.out.println(correlation + " " + wordName);
        }

        if (maxCorrelation > 0.3) { // Adjust based on testing
            return matchedWord;
        } else {
            return "No match found";
        }
    }

    private short[] extractAudioData(File file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        byte[] audioBytes = audioStream.readAllBytes();
        short[] audioData = new short[audioBytes.length / 2];
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (short) ((audioBytes[2 * i] & 0xFF) | (audioBytes[2 * i + 1] << 8));
        }
        return audioData;
    }

    private double computeNormalizedCrossCorrelation(short[] segment, short[] wordData) {
        // This can be optimized further using FFT based cross-correlation
        int wordLength = wordData.length;
        int segLength = segment.length;

        if (wordLength > segLength) {
            return 0;
        }

        double maxCorrelation = 0;

        for (int delay = 0; delay <= segLength - wordLength; delay++) {
            double sumProduct = 0;
            for (int i = 0; i < wordLength; i++) {
                sumProduct += wordData[i] * segment[i + delay];
            }
            double norm = computeNorm(segment, delay, wordLength) * computeNorm(wordData, 0, wordLength);
            if (norm != 0) {
                double correlation = sumProduct / norm;
                if (correlation > maxCorrelation) {
                    maxCorrelation = correlation;
                }
            }
        }

        return maxCorrelation;
    }

    private double computeNorm(short[] data, int start, int M) {
        double sumSquare = 0;
        for (int i = start; i < start + M; i++) {
            sumSquare += data[i] * data[i];
        }
        return Math.sqrt(sumSquare);
    }

}