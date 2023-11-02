package com.sai;

import java.util.Arrays;

public class Filter {
    public static void applyMedianFilter(short[] audioData, int windowSize) {
        if (windowSize % 2 == 0) {
            throw new IllegalArgumentException("Window size should be odd.");
        }

        int halfWindowSize = windowSize / 2;
        int dataSize = audioData.length;
        short[] tempData = new short[dataSize];
        short[] window = new short[windowSize];

        // Copy boundaries
        System.arraycopy(audioData, 0, tempData, 0, halfWindowSize);
        System.arraycopy(audioData, dataSize - halfWindowSize, tempData, dataSize - halfWindowSize, halfWindowSize);

        for (int i = halfWindowSize; i < dataSize - halfWindowSize; i++) {
            // Fill window with values from audio data
            for (int j = 0; j < windowSize; j++) {
                window[j] = audioData[i - halfWindowSize + j];
            }
            Arrays.sort(window);
            tempData[i] = window[halfWindowSize];
        }

        // Copy the result back to audioData
        System.arraycopy(tempData, 0, audioData, 0, dataSize);
    }

    public static void applyContrastFilter(short[] audioData) {
        // Calculate mean of the audioData
        double mean = 0;
        for (short sample : audioData) {
            mean += sample;
        }
        mean /= audioData.length;

        // Apply contrast enhancement
        double factor = 2.0; // This can be adjusted
        for (int i = 0; i < audioData.length; i++) {
            double diff = audioData[i] - mean;
            audioData[i] = (short) (mean + diff * factor);

            // Handle potential overflow or underflow
            if (audioData[i] > Short.MAX_VALUE) {
                audioData[i] = Short.MAX_VALUE;
            } else if (audioData[i] < Short.MIN_VALUE) {
                audioData[i] = Short.MIN_VALUE;
            }
        }
    }

    public static short[] applyLowPassFilter(short[] audioData, double cutoffFrequency) {
        double tau = 1.0 / (2 * Math.PI * cutoffFrequency);
        double alpha = tau / (tau + 1.0 / 48000);

        short[] filteredData = new short[audioData.length];

        // Start with the first sample as it has no previous value
        filteredData[0] = audioData[0];

        for (int i = 1; i < audioData.length; i++) {
            filteredData[i] = (short) (alpha * audioData[i] + (1 - alpha) * filteredData[i - 1]);
        }

        System.out.println("Low pass filter applied.");
        return filteredData;
    }
}
