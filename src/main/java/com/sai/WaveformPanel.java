package com.sai;

import javax.swing.*;
import java.awt.*;

public class WaveformPanel extends JPanel {
    private short[] audioData;

    public WaveformPanel(short[] audioData) {
        this.audioData = audioData;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawWaveform(g);
    }

    public void updateAudioData(short[] newAudioData) {
        this.audioData = newAudioData;
        repaint(); // This will request the component to be redrawn
    }

    private void drawWaveform(Graphics g) {
        if (audioData == null || audioData.length == 0) return;

        int panelHeight = getHeight();
        int panelWidth = getWidth();

        // Determine the width of each audio data point
        float xIncrement = (float) panelWidth / audioData.length;

        // Find the maximum amplitude in audioData
        short maxAmplitude = 0;
        for (short sample : audioData) {
            if (Math.abs(sample) > maxAmplitude) {
                maxAmplitude = (short) Math.abs(sample);
            }
        }

        // Adjust the scaling factor based on the maximum amplitude
        float audioDataScale = (maxAmplitude == 0) ? 0 : (float) panelHeight / (2 * maxAmplitude);

        int oldX = 0;
        int oldY = (int) (panelHeight / 2);  // Middle for 0 amplitude

        g.setColor(Color.BLUE);

        for (int i = 0; i < audioData.length; i++) {
            int x = (int) (i * xIncrement);
            int y = (int) (panelHeight / 2 - audioData[i] * audioDataScale);  // Scale and invert

            g.drawLine(oldX, oldY, x, y);

            oldX = x;
            oldY = y;
        }
    }

}

