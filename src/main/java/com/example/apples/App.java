package com.example.apples;

import java.awt.Robot;
import java.io.File;

import javax.imageio.ImageIO;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Rectangle;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

public class App {

    static final int WIDTH = 17;
    static final int HEIGHT = 10;
    // kinda arbitrary, 765 is max cuz 255 * 3 = 765
    static final int WHITE_THRESHOLD = 650;

    static int[][] apples;

    public static void main(String[] args) throws Exception {
        getApples();
        int applesCleared = calculateMove();
        while (applesCleared > 0) {
            applesCleared = calculateMove();
        }
    }

    static void getApples() {
        apples = new int[HEIGHT][WIDTH];
        try {
            Robot bot = new Robot();
            // pause for 2s, switch tabs to apple game in this time
            bot.delay(2000);
            // press reset and play buttons
            int mask = InputEvent.BUTTON1_DOWN_MASK;
            bot.mouseMove(650, 1050);
            bot.mousePress(mask);
            bot.mouseRelease(mask);
            bot.delay(100);
            bot.mouseMove(1000, 650);
            bot.mousePress(mask);
            bot.mouseRelease(mask);
            // take screenshot
            bot.delay(500);
            // rectangle is set for the bounds of the apple box
            BufferedImage img = bot.createScreenCapture(new Rectangle(630, 228, 1270, 772));
            ImageIO.write(img, "png", new File("./screenshot.png"));
            // binarize
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    final int clr = img.getRGB(x, y);
                    final int red = (clr & 0x00ff0000) >> 16;
                    final int green = (clr & 0x0000ff00) >> 8;
                    final int blue = clr & 0x000000ff;
                    // above white threshold and not green to white (cuz the apple numbers are red-ish)
                    if (red + green + blue > WHITE_THRESHOLD && green <= red && green <= blue) {
                        img.setRGB(x, y, new Color(255, 255, 255).getRGB());
                    } else {
                        img.setRGB(x, y, new Color(0, 0, 0).getRGB());
                    }
                }
            }
            ImageIO.write(img, "png", new File("./binarized.png"));

            // do OCR
            final ITesseract instance = new Tesseract();
            instance.setVariable("tessedit_char_whitelist", "0123456789");
            final String result = instance.doOCR(img);
            System.out.println(result);
            // convert to 2d array
            int row = 0;
            int col = 0;
            for (int i = 0; i < result.length(); i++) {
                char c = result.charAt(i);
                if (c == '\n') {
                    row++;
                    col = 0;
                } else if (c != ' ') {
                    // convert char to int
                    apples[row][col] = c - '0';
                    ++col;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void performClick(int x1, int y1, int x2, int y2) {
        try {
            Robot bot = new Robot();
            int mask = InputEvent.BUTTON1_DOWN_MASK;
            bot.mouseMove(x1, y1);
            bot.mousePress(mask);
            bot.mouseMove(x2, y2);
            bot.mouseRelease(mask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // returns the number of apples cleared
    static int calculateMove() {
        for (int i = 0; i < apples.length; i++) {
            for (int j = i + 1; j < apples[i].length; j++) {
                int sum = apples[i][j];
                // empty space
                if (sum == 0) {
                    continue;
                }
                // go down the line
                int k = 1;
                while (i + k < apples.length && sum < 10) {
                    sum += apples[i + k][j];
                }
                if (sum == 10) {
                    performClick(i, j, i + k, j);
                    for (int l = 0; l < k; ++l) {
                        apples[i + l][j] = 0;
                    }
                    return k + 1;
                }
                // go across the row
                sum = apples[i][j];
                k = 1;
                while (j + k < apples.length && sum < 10) {
                    sum += apples[i][j + k];
                }
                if (sum == 10) {
                    performClick(i, j, i, j + k);
                    for (int l = 0; l < k; ++l) {
                        apples[i + l][j] = 0;
                    }
                    return k + 1;
                }
            }
        }
        return 0;
    }
}
