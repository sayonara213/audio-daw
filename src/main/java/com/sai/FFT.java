package com.sai;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jtransforms.fft.DoubleFFT_1D;

public class FFT {

    public static double[] computeFFT(short[] audioData) {
        int n = audioData.length;

        // Apply windowing function (Hanning window)
        for (int i = 0; i < n; i++) {
            audioData[i] = (short) (audioData[i] * (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1))));
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(n);
        double[] a = new double[n * 2];

        for (int i = 0; i < n; i++) {
            a[i] = audioData[i];
        }

        fft.realForwardFull(a);
        return a;
    }

    public static short[] applyLowPassFilterUsingFFT(short[] audioData, double cutoffFrequency) {
        int n = audioData.length;

        DoubleFFT_1D fft = new DoubleFFT_1D(n);

        double[] fftData = new double[n * 2];
        for (int i = 0; i < n; i++) {
            fftData[i] = audioData[i];
        }
        fft.realForwardFull(fftData);

        int cutoffBin = (int) (cutoffFrequency / 48000 * n);
        int transitionWidth = n / 100;  // Width of the transition band (1% of total bins)

        // Zero out frequencies outside the transition zones
        for (int i = cutoffBin + transitionWidth; i < n - cutoffBin + transitionWidth; i++) {
            fftData[i] = 0;
            fftData[n + i] = 0;
        }

        // Apply the taper to the transition zone
        for (int i = 0; i < transitionWidth; i++) {
            double multiplier = 1.0 - (double) i / transitionWidth;

            fftData[cutoffBin + i] *= multiplier;
            fftData[n + cutoffBin + i] *= multiplier;

            fftData[n - cutoffBin - i] *= multiplier;
            fftData[2 * n - cutoffBin - i] *= multiplier;
        }

        fft.realInverse(fftData, true);

        short[] filteredData = new short[n];
        for (int i = 0; i < n; i++) {
            filteredData[i] = (short) fftData[i];
        }

        return filteredData;
    }

    public static void displayFFTGraph() {
        double[] fftData = computeFFT(AudioRecorder.audioData);

        int sampleRate = 48000;
        int nyquistLimit = fftData.length / 4;

        XYSeries series = new XYSeries("Amplitude vs Frequency");

        final double EPSILON = 1e-10;

        int numLogBins = 1000; // You can adjust this for more or fewer bins
        double minFrequency = 20; // Adjust as needed
        double maxFrequency = sampleRate / 2.0;
        double logMin = Math.log10(minFrequency);
        double logMax = Math.log10(maxFrequency);
        double delta = (logMax - logMin) / numLogBins;

        for (int bin = 0; bin < numLogBins; bin++) {
            double logStart = logMin + bin * delta;
            double logEnd = logStart + delta;

            double freqStart = Math.pow(10, logStart);
            double freqEnd = Math.pow(10, logEnd);

            int indexStart = (int) (freqStart * fftData.length / sampleRate);
            int indexEnd = (int) (freqEnd * fftData.length / sampleRate);

            double sumMag = 0;
            int count = 0;
            for (int i = indexStart; i <= indexEnd && i < nyquistLimit; i++) {
                double real = fftData[i];
                double imag = fftData[fftData.length / 2 + i];
                double magnitude = Math.sqrt(real * real + imag * imag);
                sumMag += magnitude;
                count++;
            }

            if (count > 0) {
                double avgMagnitude = sumMag / count;
                double magnitudeInDB = 10 * Math.log10(avgMagnitude + EPSILON);
                series.add((freqStart + freqEnd) / 2, magnitudeInDB); // Use the center frequency for this bin
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Audio Spectrum",
                "Frequency (Hz)",
                "Amplitude (dB)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        ChartPanel chartPanel = new ChartPanel(chart);

        javax.swing.JFrame frame = new javax.swing.JFrame("Amplitude vs Frequency");
        frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }
}
